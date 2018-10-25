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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.model.JaegerTestReport;

public class TestSuite {
    private static final AtomicBoolean PERFORMANCE_TEST_EXECUTED = new AtomicBoolean(false);
    protected static TestConfig config;
    protected static JaegerTestReport report;

    @BeforeClass
    public static void setUp() throws Exception {
        if (PERFORMANCE_TEST_EXECUTED.get()) {
            return;
        }
        PERFORMANCE_TEST_EXECUTED.set(true);

        config = TestConfig.get();
        // setting up
        // if this test triggered on local, execute performance test first
        if (!config.getRunningOnOpenshift()) {
            Main instance = new Main();
            instance.execute();
        }
        // load report
        report = ParseReport.report();
    }

    @AfterClass
    public static void tearDown() {
        // tearing down
    }
}
