/**
 * Copyright 2018-2019 The Jaeger Authors
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
package io.jaegertracing.tests.smoke;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.fasterxml.jackson.databind.JsonNode;

import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.tests.model.TestConfig;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestBase {
    public SimpleRestClient simpleRestClient = new SimpleRestClient();
    protected Instant testStartTime = null;
    private static Tracer tracer = null;
    public static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    TestConfig config = TestConfig.get();

    public static final String SMOKE_TEST_SERVICE_NAME = "smoke_test";

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void failed(Throwable ex, Description description) {
            logger.info("*** Test[FAILED]: {} ***", ex, description.getMethodName());
        }

        protected void finished(Description description) {
            logger.info("*** Test[FINISHED]: {} ***", description.getMethodName());
        }

        protected void skipped(AssumptionViolatedException ex, Description description) {
            logger.info("*** Test[SKIPPED]: {} ***", ex, description.getMethodName());
        }

        protected void starting(Description description) {
            logger.info("*** Test[STARTING]: {} ***", description.getMethodName());
        }

        protected void succeeded(Description description) {
            logger.info("*** Test[SUCCEEDED]: {} ***", description.getMethodName());
        }
    };

    @Before
    public void updateTestStartTime() {
        testStartTime = Instant.now();
        sleep(10);
    }

    /**
     *
     * @param tags Map of tags from a span
     * @param key name of the tag we want to check
     * @param expectedValue expected value of that tag
     */
    public void myAssertTag(Map<String, Object> tags, String key, Object expectedValue) {
        assertTrue("Could not find key: " + key, tags.containsKey(key));
        Object actualValue = tags.get(key);
        assertEquals("Wrong value for key " + key + " expected " + expectedValue.toString(), expectedValue,
                actualValue);
    }

    public Tracer tracer() {
        if (tracer == null) {
            SenderConfiguration conf = null;

            if (config.getSender().equalsIgnoreCase("http")) {
                String httpEndpoint = "http://" + config.getJaegerCollectorHost() + ":"
                        + config.getJaegerCollectorPort() + "/api/traces";
                logger.info("Using collector endpoint [" + httpEndpoint + "]");
                conf = new SenderConfiguration()
                        .withEndpoint(httpEndpoint);
            } else {
                logger.info("Using JAEGER agent on host " + config.getJaegerAgentHost() + " port "
                        + config.getJaegerAgentPort());
                conf = new SenderConfiguration()
                        .withAgentHost(config.getJaegerAgentHost())
                        .withAgentPort(config.getJaegerAgentPort());
            }
            RemoteReporter remoteReporter = new RemoteReporter.Builder()
                    .withSender(conf.getSender())
                    .withFlushInterval(config.getJaegerClientFlushInterval())
                    .build();

            Sampler sampler = new ProbabilisticSampler(config.getJaegerSamplingRate());
            tracer = new JaegerTracer.Builder(SMOKE_TEST_SERVICE_NAME)
                    .withReporter(remoteReporter)
                    .withSampler(sampler)
                    .build();
        }
        return tracer;
    }

    public void waitForFlush() {
        sleep(config.getJaegerClientFlushInterval() + 1000L);
    }

    public void sleep(long milliseconds) {
        try {
            //logger.debug("Sleeping {} ms", milliseconds);
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            logger.error("Exception,", ex);
        }
    }

    /**
     *
     * @param spans List of spans from a given trace
     * @param targetOperationName the operation name of the span we're looking for
     * @return A Span with the specified operation name
     */
    public QESpan getSpanByOperationName(List<QESpan> spans, String targetOperationName) {
        for (QESpan s : spans) {
            if (s.getOperation().equals(targetOperationName)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Convert all JSON Spans in the trace returned by the Jaeeger Rest API to QESpans
     **
     * @param trace A single trace
     * @return A list of all spans contained in this trace
     */
    public List<QESpan> getSpansFromTrace(JsonNode trace) {
        List<QESpan> spans = new ArrayList<>();
        Iterator<JsonNode> spanIterator = trace.get("spans").iterator();

        while (spanIterator.hasNext()) {
            JsonNode jsonSpan = spanIterator.next();
            QESpan span = createSpanFromJsonNode(jsonSpan);
            spans.add(span);
        }
        return spans;
    }

    /**
     * Convert a Span in JSON returned from the Jaeger REST API to a QESpan
     * @param jsonSpan JSON representation of a span from a trace
     * @return A QESpan build from the given json
     */
    public QESpan createSpanFromJsonNode(JsonNode jsonSpan) {
        Map<String, Object> tags = new HashMap<>();

        JsonNode jsonTags = jsonSpan.get("tags");
        Iterator<JsonNode> jsonTagsIterator = jsonTags.iterator();
        while (jsonTagsIterator.hasNext()) {
            JsonNode jsonTag = jsonTagsIterator.next();
            String key = jsonTag.get("key").asText();
            String tagType = jsonTag.get("type").asText();
            switch (tagType) {
                case "bool":
                    boolean b = jsonTag.get("value").asBoolean();
                    tags.put(key, b);
                    break;
                case "float64":
                    Number n = jsonTag.get("value").asDouble();
                    tags.put(key, n);
                    break;
                case "int64":
                    Integer i = jsonTag.get("value").asInt();
                    tags.put(key, i);
                    break;
                case "string":
                    String s = jsonTag.get("value").asText();
                    tags.put(key, s);
                    break;
                default:
                    throw new RuntimeException("Unknown tag type [" + tagType + "[");
            }
        }

        Long start = jsonSpan.get("startTime").asLong();
        Long duration = jsonSpan.get("duration").asLong();
        String operation = jsonSpan.get("operationName").textValue();
        String id = jsonSpan.get("spanID").textValue();

        QESpan qeSpan = new QESpan.Builder(tags, start, duration, operation, id)
                .json(jsonSpan)
                .build();
        return qeSpan;
    }
}