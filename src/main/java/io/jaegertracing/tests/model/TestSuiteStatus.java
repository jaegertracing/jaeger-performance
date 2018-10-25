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
package io.jaegertracing.tests.model;

import org.junit.runner.Result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestSuiteStatus {
    private String name;
    private Boolean wasSuccessful;
    private Integer failureCount;
    private Integer runCount;
    private Integer ignoreCount;
    private Long runTime;

    public static TestSuiteStatus get(String name, Result testResult) {
        testResult.wasSuccessful();
        return TestSuiteStatus.builder()
                .name(name)
                .wasSuccessful(testResult.wasSuccessful())
                .runCount(testResult.getRunCount())
                .failureCount(testResult.getFailureCount())
                .ignoreCount(testResult.getIgnoreCount())
                .runTime(testResult.getRunTime())
                .build();
    }
}
