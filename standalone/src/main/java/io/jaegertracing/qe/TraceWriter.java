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
package io.jaegertracing.qe;

import io.opentracing.Span;
import io.opentracing.Tracer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceWriter implements Callable<Integer> {
    Tracer tracer;
    int id;
    int durationInMinutes;

    private static final Map<String, String> envs = System.getenv();
    private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "1"));
    private static final Logger logger = LoggerFactory.getLogger(TraceWriter.class);


    public TraceWriter(Tracer tracer, int durationInMinutes, int id) {
        this.tracer = tracer;
        this.durationInMinutes = durationInMinutes;
        this.id = id;
    }

    @Override
    public Integer call() throws Exception {
        int  spanCount = 0;
        String s = "Thread " + id;
        logger.debug("Starting " + s);

        Instant finish = Instant.now().plus(durationInMinutes, ChronoUnit.MINUTES);
        while (Instant.now().isBefore(finish)) {
            Span span = tracer.buildSpan(s).start();
            try {
                span.setTag("iteration", spanCount);
                spanCount++;
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                logger.warn("Got interrupted exception", 3);
            }
            span.finish();
        }

        return spanCount;
    }
}

