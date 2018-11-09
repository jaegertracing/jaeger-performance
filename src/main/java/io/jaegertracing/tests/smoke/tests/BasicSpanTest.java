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
package io.jaegertracing.tests.smoke.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.jaegertracing.tests.smoke.QESpan;
import io.jaegertracing.tests.smoke.TestBase;
import io.opentracing.Span;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @since 1.0.0
 */
@Slf4j
public class BasicSpanTest extends TestBase {

    /*
     * Test: Create single span, send it to server and do validate it via query
     * api. Steps: 1. Create a span 2. send it to server 3. validate from server
     */
    @Test
    public void singleSpanTest() {
        Span span = tracer().buildSpan("simple-span").start();
        span.setTag("testType", "singleSpanTest");
        int random = RANDOM.nextInt(100000);
        span.setTag("random", random);
        span.finish();
        waitForFlush();

        // Validation
        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", 1, spans.size());
        QESpan receivedSpan = spans.get(0);
        assertEquals("simple-span", receivedSpan.getOperation());
        Map<String, Object> tags = receivedSpan.getTags();
        myAssertTag(tags, "testType", "singleSpanTest");
        myAssertTag(tags, "random", random);
    }

    /*
     * Test: Create parent span and child span Steps: 1. Create parent span and
     * child span 2. validate from server
     */
    @Test
    public void spanWithChildTest() {
        long randomSleep = RANDOM.nextInt(1000 * 2);
        Span parentSpan = tracer().buildSpan("parent-span").start();
        parentSpan.setTag("sentFrom", "automation code");
        Span childSpan = tracer().buildSpan("child-span")
                .asChildOf(parentSpan)
                .start();
        sleep(randomSleep);
        childSpan.finish();
        sleep(50L);
        parentSpan.finish();
        waitForFlush();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 2 spans", 2, spans.size());

        QESpan receivedParentSpan = getSpanByOperationName(spans, "parent-span");
        assertNotNull(receivedParentSpan);
        logger.debug(simpleRestClient.prettyPrintJson(receivedParentSpan.getJson()));
        myAssertTag(receivedParentSpan.getTags(), "sentFrom", "automation code");

        QESpan receivedChildSpan = getSpanByOperationName(spans, "child-span");
        assertNotNull(receivedChildSpan);
    }
}