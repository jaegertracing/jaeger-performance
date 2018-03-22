/**
 * Copyright 2018 The Jaeger Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.jaegertracing.qe.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.jaegertracing.qe.restclient.SimpleRestClient;
import io.jaegertracing.qe.restclient.model.Datum;
import io.jaegertracing.qe.restclient.model.Span;
import io.jaegertracing.qe.restclient.model.Tag;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Examples:
 *
 * 989  curl "jaeger-query-myproject.192.168.64.35.nip.io/api/traces?service=standalong&limit=5"
 990  curl "jaeger-query-myproject.192.168.64.35.nip.io/api/traces?service=standalong&operationName=Thread1&limit=1"
 991  curl jaeger-query-myproject.192.168.64.35.nip.io/api/traces/18b58350a83ffe3a
 *
 *
 *
 * TODO How do we get test parameters, i.e. number of traces created, number of pods that ran, number of threads, etc?
 *
 */
public class TimeQueriesTest {
    private static final Map<String, String> envs = System.getenv();
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "standalone");
    private static final Integer WORKER_PODS = Integer.valueOf(envs.getOrDefault("WORKER_PODS", "1"));
    private static final Integer THREAD_COUNT = new Integer(envs.getOrDefault("THREAD_COUNT", "100"));

    private NumberFormat numberFormat = NumberFormat.getInstance();
    private static SimpleRestClient simpleRestClient;
    private static Map<String, List<String>> queryParameters;
    private static final Logger logger = LoggerFactory.getLogger(TimeQueriesTest.class.getName());
    private static Instant testStartTime;
    private static List<String> workerPodsNames;
    private ThreadLocalRandom random = ThreadLocalRandom.current();

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info("Starting test: " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void initialize() {
        queryParameters = new LinkedHashMap<>();  // We want to maintain insertion order of keys
        queryParameters.put("service", Arrays.asList(TEST_SERVICE_NAME));      // This needs to be first
        simpleRestClient = new SimpleRestClient();
        workerPodsNames = getWorkerPodsNames();
    }

    @Before
    public void setup() {
        queryParameters.clear();
        queryParameters.put("service", Arrays.asList(TEST_SERVICE_NAME));
        testStartTime = Instant.now();
    }

    @Test
    public void simpleTimedQueryTest() {
        SimpleRestClient simpleRestClient = new SimpleRestClient();
        queryParameters.put("limit", Arrays.asList("1"));

        List<Datum> traces = simpleRestClient.getTraces(queryParameters, 1);
        System.out.println("Got " + traces.size() + " Traces ");
    }


    @Test
    public void timeGetByServiceNameTest() {
        int limit = 10000;
        queryParameters.put("limit", Arrays.asList(String.valueOf(limit)));
        List<Datum> traces = simpleRestClient.getTraces(queryParameters, limit);
        Instant testEndTime = Instant.now();
        long duration = Duration.between(testStartTime, testEndTime).toMillis();
        logger.info("Retrieval of " + limit + " spans took " + numberFormat.format(duration) + " milliseconds");
        assertNotNull(traces);
        assertEquals(limit, traces.size());

        // TODO other assertions?
    }

    /*
     *
     */
    @Test
    public void testGetWithOperationName() {
        int limit = 100;   // TODO find maximum?
        String operationName = "Thread" + (random.nextInt(THREAD_COUNT) + 1);  // pick a random Thread
        queryParameters.put("limit", Arrays.asList(String.valueOf(limit)));
        queryParameters.put("operation", Arrays.asList(operationName));
        List<Datum> traces = simpleRestClient.getTraces(queryParameters, limit);
        Instant testEndTime = Instant.now();
        long duration = Duration.between(testStartTime, testEndTime).toMillis();
        logger.info("Retrieval of " + limit + " spans by operation name took " + numberFormat.format(duration) + " milliseconds");
        assertNotNull(traces);
        assertEquals(limit, traces.size());

        for (Datum trace : traces) {
            for (Span span : trace.getSpans()) {
                assertEquals(operationName, span.getOperationName());
            }
        }
        // TODO other assertions?
    }


    @Test
    public void testGetWithOneTag() {
        int limit = 100; // TODO How to set this?  This should be number of THREADS x PODS if iteration number is lower than number of spans created by all threads.
        queryParameters.put("limit", Arrays.asList(String.valueOf(limit)));
        queryParameters.put("tag", Arrays.asList("iteration:1"));       // TODO how to search on multiple tags?
        List<Datum> traces = simpleRestClient.getTraces(queryParameters, limit);
        Instant testEndTime = Instant.now();
        long duration = Duration.between(testStartTime, testEndTime).toMillis();
        logger.info("Retrieval of " + limit + " spans by one tag took " + numberFormat.format(duration) + " milliseconds");
        assertNotNull(traces);
        assertEquals(limit, traces.size());

        for (Datum trace : traces) {

            for (Span span : trace.getSpans()) {
                //System.out.println("TAGS: " + span.getTags());
                // FIXME how to get tags?
            }
        }
    }


    @Test
    public void testGetWithTwoTags() {
        int limit = THREAD_COUNT;        // TODO How to set this?
        queryParameters.put("limit", Arrays.asList(String.valueOf(limit)));
        // TODO pick iteration at random
        queryParameters.put("tag", Arrays.asList("iteration:1", "podname:" + workerPodsNames.get(random.nextInt(WORKER_PODS))));
        System.out.println("QP " + queryParameters.size());
        List<Datum> traces = simpleRestClient.getTraces(queryParameters, limit);
        Instant testEndTime = Instant.now();
        long duration = Duration.between(testStartTime, testEndTime).toMillis();
        logger.info("Retrieval of " + limit + " spans by one tag took " + numberFormat.format(duration) + " milliseconds");
        assertNotNull(traces);
        assertEquals(limit, traces.size());

        // TODO add more validation
    }


    private static List<String> getWorkerPodsNames() {
        int limit = WORKER_PODS; // We know pod count, set this to that
        String operationName = "Thread1";
        queryParameters.put("limit", Arrays.asList(String.valueOf(limit)));
        queryParameters.put("operation", Arrays.asList(operationName));
        queryParameters.put("tag", Arrays.asList("iteration:1"));
        List<Datum> traces = simpleRestClient.getTraces(queryParameters, limit);

        Set<String> podNames = new HashSet<>();
        for (Datum trace : traces) {
            for (Span span : trace.getSpans()) {
                for (Tag tag : span.getTags()) {
                    if (tag.getKey().equalsIgnoreCase("podname")) {
                        podNames.add(tag.getValue());
                    }
                }
            }
        }
        return new ArrayList<>(podNames);
    }
}
