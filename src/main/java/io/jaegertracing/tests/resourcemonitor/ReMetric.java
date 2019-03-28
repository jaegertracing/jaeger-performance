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
package io.jaegertracing.tests.resourcemonitor;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;
import lombok.Getter;
import lombok.Builder;
import lombok.Builder.Default;

@Builder
@Getter
@ToString
public class ReMetric {
    private String suiteId;
    private String measurementSuffix;
    private Long timestamp;
    @Default
    private Map<String, String> labels = new HashMap<>();
    @Default
    private Map<String, Object> data = new HashMap<>();
}
