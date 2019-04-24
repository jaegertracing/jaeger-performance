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
package io.jaegertracing.tests.run;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import io.jaegertracing.tests.ParseReport;
import io.jaegertracing.tests.TestEnabled;
import io.jaegertracing.tests.TestSuite;
import io.jaegertracing.tests.model.TestSuiteStatus;
import io.jaegertracing.tests.smoke.TestSuiteSmoke;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmokeTest extends TestSuite {

    @ClassRule
    public static TestEnabled testEnabled = new TestEnabled();

    @Test
    public void testSmokeTestReport() {
        try {
            TestSuiteStatus _status = ParseReport.report().getData().getTestSuiteStatus()
                    .get(TestSuiteSmoke.SUITE_NAME);
            logger.info("Smoke test status:{}", _status);
            Assert.assertNotNull(_status);
            Assert.assertTrue(_status.getWasSuccessful());
            Assert.assertEquals("failure conunt", 0, (int) _status.getFailureCount());
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }
}
