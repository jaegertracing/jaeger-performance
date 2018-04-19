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

import static io.jaegertracing.qe.CreateTraces.TRACES_CREATED_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.jaegertracing.qe.restclient.SimpleRestClient;
import io.jaegertracing.qe.restclient.model.Datum;
import io.jaegertracing.qe.tests.util.PodWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ValidateTracesTest {
    private static final Map<String, String> envs = System.getenv();

    public static final String APPLICATION_NAME = envs.getOrDefault("APPLICATION_NAME", "jaeger-standalone-performance-tests");
    private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "cassandra");
    private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");
    private static final Integer DURATION_IN_MINUTES = new Integer(envs.getOrDefault("DURATION_IN_MINUTES", "5"));
    private static final String ELASTICSEARCH_HOST = envs.getOrDefault("ELASTICSEARCH_HOST", "elasticsearch");
    private static final Integer ELASTICSEARCH_PORT = new Integer(envs.getOrDefault("ELASTICSEARCH_PORT", "9200"));
    private static final String SPAN_STORAGE_TYPE = envs.getOrDefault("SPAN_STORAGE_TYPE", "cassandra");
    private static final Boolean RUNNING_IN_OPENSHIFT = Boolean.valueOf(envs.getOrDefault("RUNNING_IN_OPENSHIFT", "true"));

    private static final Logger logger = LoggerFactory.getLogger(ValidateTracesTest.class.getName());
    private NumberFormat numberFormat = NumberFormat.getInstance();


    /**
     * If running in OpenShift this test must be run first, as there can be a delay between the time spans are created
     * and they are available in the back end.  When run on OpenShift this test must also be started when pods are still
     * running so it can get trace counts out of the logs.
     *
     * @throws InterruptedException if interrupted  TODO add more info please.
     * @throws IOException if there's an ioexception
     */
    @Test
    public void countTraces() throws InterruptedException, IOException {
        Integer expectedTraceCount = 0;
        if (RUNNING_IN_OPENSHIFT) {
            expectedTraceCount = getExpectedTraceCountFromPods();
        } else {
            expectedTraceCount = Integer.valueOf(System.getProperty("expectedTraceCount"));
        }
        logger.info("EXPECTED_TRACE_COUNT " + numberFormat.format(expectedTraceCount));
        Files.write(Paths.get("tracesCreatedCount.txt"), Long.toString(expectedTraceCount).getBytes(), StandardOpenOption.CREATE);

        Instant startTime = Instant.now();
        int actualTraceCount = 0;
        if (SPAN_STORAGE_TYPE.equalsIgnoreCase("cassandra")) {
            logger.info("Validating Cassandra Traces");
            actualTraceCount = validateCassandraTraces(expectedTraceCount);
        } else  {
            logger.info("Validating ES Traces");
            actualTraceCount = validateElasticSearchTraces(expectedTraceCount);
        }

        Instant countEndTime = Instant.now();
        long countDuration = Duration.between(startTime, countEndTime).toMillis();
        logger.info("Counting " + numberFormat.format(actualTraceCount) + " traces took " + countDuration / 1000 + "." + countDuration % 1000 + " seconds.");
        Files.write(Paths.get("tracesFoundCount.txt"), Long.toString(actualTraceCount).getBytes(), StandardOpenOption.CREATE);
        assertEquals("Did not find expected number of traces", expectedTraceCount.intValue(), actualTraceCount);
    }

    /**
     * This method uses the fabric8 openshift client to:
     * -- Find out how many pods are running the test application
     * -- Setting up watchers to wait until they finish their work
     * -- Getting the number of traces created from their logs
     *
     * The last part is a bit hacky but is the best solution I could come up with for the moment
     *
     * @return The number of traces created by all pods
     * @throws InterruptedException only if sleeps are interrupted
     */
    private Integer getExpectedTraceCountFromPods() throws InterruptedException {
        Thread.sleep(30 * 1000);  // Give all pods time to start up

        OpenShiftClient client = new DefaultOpenShiftClient();
        List<Pod> pods = client.pods()
                .inNamespace(client.getNamespace())
                .withLabel("app=" + APPLICATION_NAME)
                .list()
                .getItems();

        assertNotNull("Expected at least one pod", pods);
        assertTrue("Expected at least one pod", pods.size() > 0);

        logger.info("Waiting for " + pods.size() + " pod(s) to finish");
        CountDownLatch podsCountDownLatch = new CountDownLatch(pods.size());
        client.pods()
                .inNamespace(client.getNamespace())
                .withLabel("app=" + APPLICATION_NAME)
                .watch(new PodWatcher(podsCountDownLatch));
        podsCountDownLatch.await(DURATION_IN_MINUTES + 1, TimeUnit.MINUTES);      // set timeout to DURATION + 1?

        Integer expectedTraceCount = 0;
        for (Pod pod : pods) {
            String targetPodName = pod.getMetadata().getName();
            Integer traceCount = getTraceCountForPod(client, targetPodName);
            logger.info("Got " + numberFormat.format(traceCount) + " traces from pod " + targetPodName);
            expectedTraceCount += traceCount;
        }
        return expectedTraceCount;
    }


    /**
     *
     *
     * @param client An OpenShiftClient
     * @param podName Name of the pod whose log we want to search
     * @return number of traces created in this pod
     */
    private Integer getTraceCountForPod(OpenShiftClient client, String podName) {
        String log  = client.pods()
                .inNamespace(client.getNamespace())
                .withName(podName)
                .inContainer(APPLICATION_NAME)
                .tailingLines(3)
                .getLog();

        int startOfCount = log.indexOf(TRACES_CREATED_MESSAGE) + TRACES_CREATED_MESSAGE.length();
        int endOfCount = log.indexOf("\n", startOfCount);
        String countString = log.substring(startOfCount, endOfCount);
        Integer tracesWrittenCount = Integer.valueOf(countString);

        return tracesWrittenCount;
    }


    /**
     * It can take a while for traces to actually get written to storage, so both this and the Cassandra validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount number of traces we expect to find
     * @return actual number of traces found in ElasticSearch
     */
    private int validateElasticSearchTraces(int expectedTraceCount) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = now.format(formatter);
        String targetUrlString = "/jaeger-span-" + formattedDate + "/_count";
        logger.info("Using ElasticSearch URL : [" + targetUrlString + "]");

        RestClient restClient = getESRestClient();

        int previousTraceCount = -1;
        int actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
        final int startTraceCount = actualTraceCount;
        int iterations = 0;
        final long sleepDelay = 10L;

        logger.info("Setting SLEEP DELAY " + sleepDelay + " seconds");
        logger.info("Actual Trace count " + numberFormat.format(actualTraceCount));
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            try {
                TimeUnit.SECONDS.sleep(sleepDelay);
            } catch (InterruptedException e) {
                logger.warn("Got interrupted exception", e);
            }
            previousTraceCount = actualTraceCount;
            actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
            logger.info("FOUND " + numberFormat.format(actualTraceCount) + " traces in ElasticSearch");
            iterations++;
        }

        logger.info("It took " + iterations  + " iterations to go from " + numberFormat.format(startTraceCount)
                + " to " + numberFormat.format(actualTraceCount) + " traces");
        logger.info("FOUND " + numberFormat.format(actualTraceCount) + " traces in ElasticSearch");
        return actualTraceCount;
    }

    private RestClient getESRestClient() {
        logger.debug("Connecting to elasticsearch using host " + ELASTICSEARCH_HOST + " and port " + ELASTICSEARCH_PORT);
        return RestClient.builder(
                    new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT, "http"),
                    new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT + 1, "http"))
                    .build();
    }

    private int getElasticSearchTraceCount(RestClient restClient, String targetUrlString) throws IOException {
        Response response = restClient.performRequest("GET", targetUrlString);
        String responseBody = EntityUtils.toString(response.getEntity());
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        JsonNode jsonPayload = jsonObjectMapper.readTree(responseBody);
        JsonNode count = jsonPayload.get("count");
        int traceCount = count.asInt();

        return traceCount;
    }

    /**
     * It can take a while for traces to actually get written to storage, so both this and the ElasticSearch validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount number of traces we expect to find
     * @return final trace count found in Cassandra
     */
    private int validateCassandraTraces(int expectedTraceCount) {
        Session cassandraSession = getCassandraSession();
        int previousTraceCount = -1;
        int actualTraceCount = countTracesInCassandra(cassandraSession);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Got interrupted exception", 3);
            }
            previousTraceCount = actualTraceCount;
            actualTraceCount = countTracesInCassandra(cassandraSession);
            logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
            iterations++;
        }

        logger.info("It took " + iterations  + " iterations to go from " + startTraceCount + " to " + actualTraceCount + " traces");
        logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
        return actualTraceCount;
    }

    private Session getCassandraSession() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CASSANDRA_CLUSTER_IP);
        Cluster cluster = builder.build();
        Session session = cluster.connect(CASSANDRA_KEYSPACE_NAME);

        return session;
    }

    /**
     * For performance reasons Cassandra won't let us do a "select count(*) from traces" so instead we just have to do
     * "select * from traces" and count the number of rows it returns.
     *
     * @param session An open Cassandra session to use for queries
     * @return current number of traces found in Cassandra
     */
    private int countTracesInCassandra(Session session) {
        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();

        return totalTraceCount;
    }


    class RowCountingConsumer implements Consumer<Row> {
        AtomicInteger rowCount = new AtomicInteger(0);

        @Override
        public void accept(Row r) {
            rowCount.getAndIncrement();
        }

        public int getRowCount() {
            return rowCount.get();
        }
    }

}
