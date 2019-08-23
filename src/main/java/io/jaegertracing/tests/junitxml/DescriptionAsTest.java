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

import org.junit.runner.Description;

import junit.framework.TestResult;
import junit.framework.Test;

// copied from: https://github.com/cloudbees/junit-standalone-runner/blob/
// 9f969beb818fa842938b90b17e82101427ed91b4/src/main/java/com/cloudbees/junit/runner/App.java
/**
 * Wraps {@link Description} into {@link Test} enough to fake {@link JUnitResultFormatter}.
 */
public class DescriptionAsTest implements Test {
    private final Description description;
    private final String IGNORE_CLASSNAME_PART = "io.jaegertracing.tests.smoke.tests.";

    public DescriptionAsTest(Description description) {
        this.description = description;
    }

    public int countTestCases() {
        return 1;
    }

    public void run(TestResult result) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@link JUnitResultFormatter} determines the test name by reflection.
     */
    public String getName() {
        return description.getClassName().replace(IGNORE_CLASSNAME_PART, "") + "."
                + description.getMethodName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DescriptionAsTest that = (DescriptionAsTest) o;

        if (!description.equals(that.description))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }
}
