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
package io.jaegertracing.tests.report.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.Builder.Default;
import lombok.Builder;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Slf4j
public class JaegerMetrics {
    @Default
    private List<Map<String, Object>> collector = new ArrayList<>();
    @Default
    private List<Map<String, Object>> agent = new ArrayList<>();
    @Default
    private List<Map<String, Object>> query = new ArrayList<>();
    @Default
    private HashMap<String, Object> summary = new HashMap<>();

    public void updateSummary() {
        try {
            Map<String, Object> collectorSummary = new HashMap<>();

            List<Map<String, Object>> podList = new ArrayList<>();
            long spansDropped = 0;
            long spansReceived = 0;
            long errorBusy = 0;
            // update from collector service
            for (Map<String, Object> map : collector) {
                long _spansDropped = getSumOfLong(get(map, "jaeger.collector.spans.dropped"));
                long _spansReceived = getSumOfLong(get(map, "jaeger.collector.spans.received"));
                long _errorBusy = getSumOfLong(get(map, "jaeger.collector.error.busy"));

                // add it into overall summary
                spansDropped += _spansDropped;
                spansReceived += _spansReceived;
                errorBusy += _errorBusy;

                Map<String, Object> pod = new HashMap<>();
                pod.put("hostname", getHostname(map, "jaeger.collector.spans.dropped.host_"));
                pod.put("spansDropped", _spansDropped);
                pod.put("spansDroppedPercent", Math.round(((_spansDropped * 100.0) / _spansReceived) * 100.0) / 100.0);
                pod.put("spansSuccess", _spansReceived - _spansDropped);
                pod.put("spansReceived", _spansReceived);
                pod.put("errorBusy", _errorBusy);

                // add it in to pods list
                podList.add(pod);
            }

            // update collector summary
            collectorSummary.put("pod", podList);
            collectorSummary.put("spansDropped", spansDropped);
            collectorSummary.put("spansDroppedPercent",
                    Math.round(((spansDropped * 100.0) / spansReceived) * 100.0) / 100.0);
            collectorSummary.put("spansSuccess", spansReceived - spansDropped);
            collectorSummary.put("spansReceived", spansReceived);
            collectorSummary.put("errorBusy", errorBusy);

            // update into summary
            summary.put("collector", collectorSummary);
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }

    private long getSumOfLong(List<Object> data) {
        long sum = 0;
        for (Object obj : data) {
            if (obj instanceof Long) {
                sum += (Long) obj;
            } else {
                sum += (long) Double.parseDouble(String.valueOf(obj));
            }
        }
        return sum;
    }

    private String getHostname(Map<String, Object> map, String prefix) {
        for (String key : map.keySet()) {
            if (key.startsWith(prefix)) {
                return key.replace(prefix, "");
            }
        }
        return null;
    }

    private List<Object> get(Map<String, Object> map, String prefix) {
        List<Object> data = new ArrayList<>();
        for (String key : map.keySet()) {
            if (key.startsWith(prefix)) {
                data.add(map.get(key));
            }
        }
        return data;
    }
}
