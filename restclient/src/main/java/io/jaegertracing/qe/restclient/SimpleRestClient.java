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
package io.jaegertracing.qe.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaegertracing.qe.restclient.model.Datum;
import io.jaegertracing.qe.restclient.model.Traces;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Earls
 */
public class SimpleRestClient {
    private static Map<String, String> evs = System.getenv();
    private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(evs.getOrDefault("JAEGER_FLUSH_INTERVAL", "1000"));
    private static final String JAEGER_QUERY_HOST = evs.getOrDefault("JAEGER_QUERY_HOST", "localhost");
    private static final Integer JAEGER_QUERY_SERVICE_PORT = new Integer(evs.getOrDefault("JAEGER_QUERY_SERVICE_PORT", "16686"));
    private static final String TEST_SERVICE_NAME = evs.getOrDefault("TEST_SERVICE_NAME", "PR696");

    // Limit for the number of retries when getting traces
    private static final Integer RETRY_LIMIT = 10;

    private ObjectMapper jsonObjectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(SimpleRestClient.class.getName());

    private List<Datum> getTraces(Map<String, List<String>> parameters) {
        Client client = ResteasyClientBuilder.newBuilder()
                .build();
        String targetUrl = "http://" + JAEGER_QUERY_HOST + ":" + JAEGER_QUERY_SERVICE_PORT + "/api/traces";

        WebTarget target = client.target(targetUrl);
        for (String key : parameters.keySet()) {
            for (String value : parameters.get(key)) {
                target = target.queryParam(key, value);
            }
        }

        logger.info("GETTING TRACES: " + target.getUri());
        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_JSON);
        Traces traces = builder.get(Traces.class);
        client.close();

        return traces.getData();
    }

    /**
     * Get the traces using the parameters passed in.  This method has a retry loop built in, as there
     * can often be a delay between the time a trace is created and when it can be obtained from the UI.
     *
     * @param parameters key value pairs to append to request
     * @param expectedTraceCount number of traces we expect; this will be used as the limit parameter
     * @return
     */
    public List<Datum> getTraces(Map<String, List<String>> parameters, int expectedTraceCount) {
        int iterations = 0;
        List<Datum> traces = new ArrayList<>();

        if (!parameters.containsKey("service")) {
            parameters.put("service", Arrays.asList(TEST_SERVICE_NAME));
        }
        if (!parameters.containsKey("limit")) {
            parameters.put("limit", Arrays.asList(String.valueOf(expectedTraceCount)));
        }

        // Retry for up to RETRY_LIMIT seconds to get the expected number of traces
        while (iterations < RETRY_LIMIT && traces.size() < expectedTraceCount) {
            iterations++;
            traces = getTraces(parameters);
            if (traces.size() >= expectedTraceCount) {
                return traces;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Sleep was interrupted", e);
            }
        }

        if (traces.size() < expectedTraceCount) {
            logger.error("Expected " + expectedTraceCount + " traces, found: " + traces.size());
        }

        return traces;
    }

    /**
     * Return all of the traces created since the start time given.
     *
     * @param start get traces created since this time
     * @return A List of Traces created after the time specified.
     */
    public List<Datum> getTracesSinceStart(Instant start, int expectedTraceCount) {
        long startTime = TimeUnit.MILLISECONDS.toMicros(start.toEpochMilli());
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        parameters.put("start", Arrays.asList(String.valueOf(startTime)));

        List<Datum> traces = getTraces(parameters, expectedTraceCount);
        return traces;
    }

    /**
     * Return all of the traces created between the start and end times given.
     *
     * @param start start time
     * @param end end time
     * @return A List of traces created between the times specified.
     */
    public List<Datum> getTracesBetween(Instant start, Instant end, int expectedTraceCount) {
        long startTime = TimeUnit.MILLISECONDS.toMicros(start.toEpochMilli());
        long endTime = TimeUnit.MILLISECONDS.toMicros(end.toEpochMilli());
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        parameters.put("start", Arrays.asList(String.valueOf(startTime)));
        parameters.put("end", Arrays.asList(String.valueOf(endTime)));

        List<Datum> traces = getTraces(parameters, expectedTraceCount);
        return traces;
    }


    /**
     * Get a single trace by id
     * @param traceId the traceID we're searching for
     * @return A trace (Datum)
     */
    public Datum getTraceById(String traceId) {
        Client client = ResteasyClientBuilder.newBuilder()
                .build();
        String targetUrl = "http://" + JAEGER_QUERY_HOST + ":" + JAEGER_QUERY_SERVICE_PORT + "/api/trace/" + traceId;

        WebTarget target = client.target(targetUrl);
        logger.debug("GETTING TRACE: " + target.getUri());

        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_JSON);
        Traces traces = builder.get(Traces.class);
        client.close();

        return traces.getData().get(0);
    }

}
