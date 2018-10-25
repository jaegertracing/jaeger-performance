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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.jaegertracing.tests.smoke.QESpan;
import io.jaegertracing.tests.smoke.TestBase;
import io.opentracing.Span;

/**
 * Created by Kevin Earls on 04 April 2017.
 */
public class TagAndDurationTest extends TestBase {

    AtomicLong operationId = new AtomicLong(Instant.now().getEpochSecond());

    @Before
    public void beforeMethod() {
        operationId.incrementAndGet();
    }

    /**
     * Write a single span with one tag, and verify that the correct tag is returned
     */
    @Test
    public void simpleTagTest() throws Exception {
        String operationName = "simpleTagTest-" + operationId.getAndIncrement();
        Span span = tracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        span.finish();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", spans.size(), 1);

        myAssertTag(spans.get(0).getTags(), "simple", true);
    }

    /**
     * Write a single span with a sleep before the finish, and make sure the
     * duration is correct.
     *
     */
    @Test
    public void simpleDurationTest() {
        String operationName = "simpleDurationTest-" + operationId.getAndIncrement();
        Span span = tracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        long expectedMinimumDuration = 100;
        sleep(expectedMinimumDuration);
        span.finish();

        List<JsonNode> traces = simpleRestClient.getTracesSinceTestStart(testStartTime, 1);
        assertEquals("Expected 1 trace", 1, traces.size());
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals("Expected 1 span", 1, spans.size());

        long expectedDuration = TimeUnit.MILLISECONDS.toMicros(expectedMinimumDuration);  // Remember duration is in microseconds
        assertTrue("Expected duration: " + expectedDuration, spans.get(0).getDuration() >= expectedDuration);
    }
}