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
package io.jaegertracing.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.tests.model.TestConfig;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Pavol Loffay
 */
@Slf4j
public class CreateSpansRunnable implements Runnable {

    class SpansReporer extends TimerTask {

        private long delay = 0;

        public SpansReporer() {
            if (config.isPerformanceTestLongRunEnabled()) {
                // actual is 1000 ms, modify maximum available duration based on spans count.
                // change it to microsecond
                int maxDuration = 100;
                if (config.getSpansCount() < 200) {
                    maxDuration = 700;
                } else if (config.getSpansCount() < 500) {
                    maxDuration = 500;
                }
                delay = (maxDuration * 1000L) / config.getSpansCount();
                logger.debug("Maximun execution time per round:{}ms, delay between spans:{}us, spans count:{}",
                        maxDuration, delay, config.getSpansCount());
            } else {
                delay = config.getPerformanceTestSpanDelay() * 1000L; // delay in microseconds
            }
        }

        @Override
        public void run() {
            if (config.isPerformanceTestLongRunEnabled()
                    && executedCount.get() >= config.getPerformanceTestDuration()) {
                return;
            }
            Map<String, Object> logs = new HashMap<>();
            logs.put("event", Tags.ERROR);
            logs.put("error.object", new RuntimeException());
            logs.put("class", this.getClass().getName());
            int count = 0;
            long startTime = System.currentTimeMillis();
            do {
                count++;
                // emulate client spans
                Span span = tracer.buildSpan(name)
                        .withTag(Tags.COMPONENT.getKey(), "perf-test")
                        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                        .withTag(Tags.HTTP_METHOD.getKey(), "get")
                        .withTag(Tags.HTTP_STATUS.getKey(), 200)
                        .withTag(Tags.HTTP_URL.getKey(), "http://www.example.com/foo/bar?q=bar")
                        .start();
                span.log(logs);
                span.finish();
                try {
                    TimeUnit.MICROSECONDS.sleep(delay);
                } catch (InterruptedException ex) {
                    logger.error("exception, ", ex);
                }
            } while (count < config.getSpansCount());
            if (config.isPerformanceTestLongRunEnabled()) {
                executedCount.incrementAndGet();
                if (logger.isTraceEnabled()) {
                    if (executedCount.get() % 60 == 0 || executedCount.get() == 1) { // print every 1 minute once
                        logger.trace("Round number:{}, duration:{}ms, Tracer:{}",
                                executedCount.get(), System.currentTimeMillis() - startTime, name);
                    }
                }

            } else {
                logger.debug("Reporting spans done, duration:{}, Tracer:{}",
                        TestUtils.timeTaken(System.currentTimeMillis() - startTime), name);
            }
        }

    }

    private JaegerTracer tracer;
    private String name;
    private TestConfig config;
    private boolean close;
    private AtomicInteger executedCount = new AtomicInteger(0);

    private final int expectedCount;

    public CreateSpansRunnable(JaegerTracer tracer, String name, TestConfig config, boolean close) {
        this.tracer = tracer;
        this.name = name;
        this.config = config;
        this.close = close;
        if (config.isPerformanceTestLongRunEnabled()) {
            this.expectedCount = config.getPerformanceTestDuration();
        } else {
            this.expectedCount = 0;
        }
    }

    @Override
    public void run() {
        logger.debug("Sending spans triggered for the tracer: {}", name);

        if (config.isPerformanceTestQuickRunEnabled()) {
            SpansReporer spansReporer = new SpansReporer();
            spansReporer.run();
        } else {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new SpansReporer(), 0L, 1000L);
            try {
                while (executedCount.get() < expectedCount) {
                    TimeUnit.MILLISECONDS.sleep(500L);
                }
                // sleep 1 second to avoid breaking last run of sending spans
                TimeUnit.SECONDS.sleep(1);
                timer.cancel();
                logger.debug("Number of rounds[executed:{}, actual:{}]", executedCount.get(), expectedCount);
            } catch (Exception ex) {
                logger.error("Exception,", ex);
            }
        }

        if (close) {
            tracer.close();
        }
    }

}
