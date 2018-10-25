/**
 * Copyright ${license.git.copyrightYears} The Jaeger Authors
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
package io.jaegertracing.tests.smoke.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.jaegertracing.tests.smoke.QESpan;
import io.jaegertracing.tests.smoke.TestBase;
import io.opentracing.Span;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Kevin Earls on 14/04/2017.
 *
 */
@Slf4j
public class FirstJaegerTest extends TestBase {
    AtomicInteger operationId = new AtomicInteger(0);

    @Before
    public void beforeMethod() {
        operationId.incrementAndGet();
    }

    /**
     * A simple test that just creates one span, and verifies that it was created correctly.
     *
     */
    @Test
    public void writeASingleSpanTest() throws Exception {
        String operationName = "writeASingleSpanTest" + operationId.getAndIncrement();
        Span span = tracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        span.finish();
        waitForFlush();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", 1, spans.size());
        QESpan receivedSpan = spans.get(0);
        assertEquals(operationName, receivedSpan.getOperation());
        //logger.debug(simpleRestClient.prettyPrintJson(receivedSpan.getJson()));

        assertTrue(receivedSpan.getTags().size() >= 3);
        myAssertTag(receivedSpan.getTags(), "simple", true);
    }

    /**
     * Simple test of creating a span with children
     *
     */
    @Test
    public void spanWithChildrenTest() {
        String operationName = "spanWithChildrenTest" + operationId.getAndIncrement();
        Span parentSpan = tracer().buildSpan(operationName)
                .withTag("parent", true)
                .start();

        Span childSpan1 = tracer().buildSpan(operationName + "-child1")
                .asChildOf(parentSpan)
                .withTag("child", 1)
                .start();
        sleep(100);

        Span childSpan2 = tracer().buildSpan(operationName + "-child2")
                .asChildOf(parentSpan)
                .withTag("child", 2)
                .start();
        sleep(50);

        childSpan1.finish();
        childSpan2.finish();
        parentSpan.finish();

        List<QESpan> spans;
        int iteration = 0;
        do {
            List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
            assertEquals("Expected 1 trace", 1, traces.size());
            spans = getSpansFromTrace(traces.get(0));
            iteration++;
        } while (iteration < 10 && spans.size() < 3);

        assertEquals(3, spans.size());

        // TODO validate parent child structure, operationNames, etc.
    }

    /**
     * A simple test of the start and end options when fetching traces.
     *
     */
    @Ignore("See https://github.com/Hawkular-QE/jaeger-java-test/issues/14")
    @Test
    public void testStartEndTest() {
        String operationName = "startEndTest" + operationId.getAndIncrement();
        Instant testEndTime = Instant.now();
        int expectedTraceCount = 3;
        for (int i = 0; i < 5; i++) {
            if (i == expectedTraceCount) {
                testEndTime = Instant.now();
                sleep(50);
            }
            Span testSpan = tracer().buildSpan(operationName)
                    .withTag("startEndTestSpan", i)
                    .start();
            testSpan.finish();
        }

        List<JsonNode> traces = simpleRestClient.getTracesBetween(testStartTime, testEndTime, expectedTraceCount);
        assertEquals("Expected " + expectedTraceCount + " traces", expectedTraceCount, traces.size());
        // TODO add more assertions
    }

    /**
     * This should create 2 traces as Jaeger closes a trace when finish() is called
     * on the top-level span
     *
     */
    @Test
    public void successiveSpansTest() {
        String operationName = "successiveSpansTest" + operationId.getAndIncrement();
        Span firstSpan = tracer().buildSpan(operationName)
                .withTag("firstSpan", true)
                .start();
        sleep(50);
        firstSpan.finish();

        operationName = "successiveSpansTest" + operationId.getAndIncrement();
        Span secondSpan = tracer().buildSpan(operationName)
                .withTag("secondSpan", true)
                .start();
        sleep(75);
        secondSpan.finish();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 2);
        assertEquals("Expected 2 traces", 2, traces.size());
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", 1, spans.size());
    }

    /**
     * Test span log
     */
    @Test
    public void spanDotLogIsBrokenTest() {
        String operationName = "spanDotLogIsBrokenTest";
        Span span = tracer().buildSpan(operationName).start();
        Map<String, String> logFields = new HashMap<>();
        logFields.put("something", "happened");
        logFields.put("event", "occured");
        span.log(logFields);
        //span.log("event");
        span.finish();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());

        //TODO: validate log; need to update QESpan to add log fields, or get them directly from Json
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        QESpan receivedSpan = spans.get(0);
        assertEquals(operationName, receivedSpan.getOperation());
        //logger.debug(simpleRestClient.prettyPrintJson(receivedSpan.getJson()));
    }

    /**
     * According to the OpenTracing spec tags can be String, Number, or Boolean
     *
     */
    @Test
    public void tagsShouldBeTypedTest() {
        String operationName = "tagsShouldBeTypedTest";
        Span span = tracer().buildSpan(operationName)
                .withTag("booleanTag", true)
                .withTag("numberTag", 42)
                .withTag("floatTag", Math.PI)
                .withTag("stringTag", "I am a tag")
                .start();
        span.finish();
        waitForFlush();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", 1, spans.size());
        Map<String, Object> tags = spans.get(0).getTags();

        // TODO do we need to validate the tag type in the Json?
        myAssertTag(tags, "booleanTag", true);
        myAssertTag(tags, "numberTag", 42);
        myAssertTag(tags, "stringTag", "I am a tag");

        // Workaround for https://github.com/jaegertracing/jaeger/issues/685
        // WAS: myAssertTag(tags, "floatTag", Math.PI);
        assertTrue("Could not find key: " + "floatTag", tags.containsKey("floatTag"));
        Object actualValue = tags.get("floatTag");
        logger.info("Actual Value is a " + actualValue.getClass().getCanonicalName() + " " + actualValue);
        assertTrue("Put a message here", actualValue.equals(Math.PI) || actualValue.equals(3.141592654));
    }
}