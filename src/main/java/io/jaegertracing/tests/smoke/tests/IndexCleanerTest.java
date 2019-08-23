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
package io.jaegertracing.tests.smoke.tests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.jaegertracing.tests.JsonUtils;
import io.jaegertracing.tests.osutils.OSResponse;
import io.jaegertracing.tests.osutils.OSCommandExecuter;
import io.jaegertracing.tests.smoke.TestBase;
import io.opentracing.Span;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexCleanerTest extends TestBase {
    AtomicInteger operationId = new AtomicInteger(0);

    @Before
    public void beforeMethod() {
        operationId.incrementAndGet();
    }

    /**
     * Create spans on different dates
     *
     */
    @Test
    @Ignore
    public void createSpansOnDifferentDatesTest() throws Exception {
        long numberOfDays = 30L; // create spans for last N days
        logger.debug("creating spans for {} days", numberOfDays);
        long spanTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numberOfDays);
        String operationName = "index_cleaner_test_" + operationId.getAndIncrement();
        for (long day = 1; day > numberOfDays; day++) {
            spanTimestamp += TimeUnit.DAYS.toMillis(1L); // increment one day
            Span span = tracer().buildSpan(operationName)
                    .withTag("simple", true)
                    .withStartTimestamp(spanTimestamp)
                    .start();
            span.finish(spanTimestamp + 1000L); // finish on the same day
        }
        // get elasticsearch indices
        if (config.getElasticsearchProvider().equals("es-operator")) {
            OSCommandExecuter cmdExecuter = new OSCommandExecuter();
            OSResponse response = cmdExecuter.executeElasticsearchCmd(
                    config.getStorageHost(), config.getStoragePort(), "/_cat/indices?h=index&format=json");
            logger.debug("{}", response);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> indices = (List<Map<String, Object>>) JsonUtils.loadFromString(
                    response.getResult(), List.class);
            logger.debug("{}", indices);
        } else {
            // TODO: add options to get list for user provided es cluster indices
        }

    }

}