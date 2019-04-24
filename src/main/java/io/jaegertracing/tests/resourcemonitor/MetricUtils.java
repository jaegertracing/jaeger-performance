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

import java.util.List;
import java.util.Map;

import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.Gauge;
import org.hawkular.agent.prometheus.types.Metric;
import org.hawkular.agent.prometheus.types.MetricFamily;
import org.hawkular.agent.prometheus.types.MetricType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MetricUtils {

    public static long getCounterValue(List<MetricFamily> families, String prefix) {
        return getCounterValue(families, prefix, null);
    }

    public static long getCounterValue(List<MetricFamily> families, String prefix, Map<String, String> labelFilter) {
        Double value = 0.0;
        for (MetricFamily family : families) {
            if (family.getName().equals(prefix) && family.getType() == MetricType.COUNTER) {
                for (Metric metric : family.getMetrics()) {
                    Counter counter = (Counter) metric;
                    if (labelFilter != null) {
                        boolean include = true;
                        Map<String, String> labels = metric.getLabels();
                        for (String key : labelFilter.keySet()) {
                            if (labels.get(key) == null || !labels.get(key).equals(labelFilter.get(key))) {
                                include = false;
                                break;
                            }
                        }
                        if (include) {
                            value += counter.getValue();
                        }
                    } else {
                        value += counter.getValue();
                    }
                }
                break;
            }
        }
        return value.longValue();
    }

    public static Double getGaugeValue(List<MetricFamily> families, String prefix) {
        return getGaugeValue(families, prefix, null);
    }

    public static Double getGaugeValue(List<MetricFamily> families, String prefix, Map<String, String> labelFilter) {
        for (MetricFamily family : families) {
            if (family.getName().equals(prefix) && family.getType() == MetricType.GAUGE) {
                for (Metric metric : family.getMetrics()) {
                    Gauge gauge = (Gauge) metric;
                    if (labelFilter != null) {
                        Map<String, String> labels = metric.getLabels();
                        for (String key : labelFilter.keySet()) {
                            if (labels.get(key) == null || !labels.get(key).equals(labelFilter.get(key))) {
                                return gauge.getValue();
                            }
                        }
                    } else {
                        return gauge.getValue();
                    }
                }
                break;
            }
        }
        return new Double(0.0);
    }

    public static String getLabel(List<MetricFamily> families, String prefix, String labelKey) {
        for (MetricFamily family : families) {
            if (family.getName().equals(prefix)) {
                for (Metric metric : family.getMetrics()) {
                    if (metric.getLabels().containsKey(labelKey)) {
                        return metric.getLabels().get(labelKey);
                    }
                }
            }
        }
        return null;
    }

}
