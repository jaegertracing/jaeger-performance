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
package io.jaegertracing.tests.junitxml;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

// copied from: https://github.com/cloudbees/junit-standalone-runner/blob/master
// /src/main/java/com/cloudbees/junit/runner/App.java#L189-L260
public class JUnitResultFormatterAsRunListener extends RunListener {
    protected final JUnitResultFormatter formatter;
    private String suiteName;

    public JUnitResultFormatterAsRunListener(JUnitResultFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        suiteName = description.getDisplayName();
        formatter.startTestSuite(new JUnitTest(suiteName));
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        JUnitTest suite = new JUnitTest(suiteName);
        suite.setCounts(result.getRunCount(), result.getFailureCount(), 0);
        suite.setRunTime(result.getRunTime());
        formatter.endTestSuite(suite);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        formatter.startTest(new DescriptionAsTest(description));
    }

    @Override
    public void testFinished(Description description) throws Exception {
        formatter.endTest(new DescriptionAsTest(description));     
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        testAssumptionFailure(failure);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        formatter.addError(new DescriptionAsTest(failure.getDescription()), failure.getException());
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
    }
}