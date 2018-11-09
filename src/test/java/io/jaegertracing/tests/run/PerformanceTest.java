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
package io.jaegertracing.tests.run;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import io.jaegertracing.tests.TestEnabled;
import io.jaegertracing.tests.TestSuite;
import io.jaegertracing.tests.TestUtils;
import io.jaegertracing.tests.report.model.TimerModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceTest extends TestSuite {

    private static final double MAX_ACCEPTED_DURATION = 1000 * 20; // 20 seconds

    @ClassRule
    public static TestEnabled testEnabled = new TestEnabled();

    @Test
    public void test01ReportStatus() {
        Assert.assertNotNull(report);
    }

    @Test
    public void test02SpansCount() {
        Number spansSent = (Number) report.getSpansCountStatistics().get("sent");
        Number spansFound = (Number) report.getSpansCountStatistics().get("found");
        logger.debug("Spans[sent:{}, found:{}]", spansSent.longValue(), spansFound.longValue());
        Assert.assertEquals(spansSent, spansFound);
    }

    @Test
    public void test03QueryTime() {
        for (TimerModel timer : report.getMetric().getTimers()) {
            if (timer.getName().startsWith("FINAL")) {
                Double mean = (double) timer.getDuration().get("mean");
                Double max = (double) timer.getDuration().get("max");
                Double min = (double) timer.getDuration().get("min");
                logger.debug("Time taken, name:{}, count:{}, mean:{}, max:{}, min:{}",
                        timer.getName(), timer.getCount(),
                        TestUtils.timeTaken(mean.longValue()),
                        TestUtils.timeTaken(max.longValue()),
                        TestUtils.timeTaken(min.longValue()));
                Assert.assertTrue(mean <= MAX_ACCEPTED_DURATION);
            }
        }
    }
}
