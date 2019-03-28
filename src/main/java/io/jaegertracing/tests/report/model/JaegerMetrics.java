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
    private List<Object> collector = new ArrayList<>();
    @Default
    private List<Object> agent = new ArrayList<>();
    @Default
    private List<Object> query = new ArrayList<>();
    @Default
    private HashMap<String, Object> summary = new HashMap<>();

    private static final String[] REMOVE_KEYS = { "memstats" };

    public void updateSummary(String metricsType) {
        updateCollectorSummary(metricsType);
        updateAgentSummary(metricsType);
        updateQuerySummary(metricsType);
        // remove metrics
        if (metricsType.equals("prometheus")) {
            collector.clear();
            agent.clear();
            query.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void updateQuerySummary(String metricsType) {
        try {
            // TODO: query service required values need to be updated 
            for (Object _query : query) {
                if (metricsType.equals("expvar")) {
                    Map<String, Object> map = (Map<String, Object>) _query;
                    removeKey(map);
                }
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAgentSummary(String metricsType) {
        // "jaeger.agent.reporter.batches.submitted.format_jaeger.protocol_grpc": 147926,
        // "jaeger.agent.reporter.spans.submitted.format_jaeger.protocol_grpc": 2.709052e+06,
        // "jaeger.agent.thrift.udp.server.packet_size.model_jaeger.protocol_compact": 576,
        // "jaeger.agent.thrift.udp.server.packets.processed.model_jaeger.protocol_compact": 147926,

        try {
            Map<String, Object> agentSummary = new HashMap<>();
            List<Map<String, Object>> podList = new ArrayList<>();

            long batchesSubmittedProtocolGrpc = 0;
            long spansSubmittedProtocolGrpc = 0;
            long spansFailuresTotalGrpc = 0;

            long pacetSizeProtocolCompact = 0;
            long pacetsProcessedTotalProtocolCompact = 0;
            long pacetsDroppedTotalProtocolCompact = 0;

            for (Object _agent : agent) {
                long _batchesSubmittedProtocolGrpc = 0;
                long _spansSubmittedProtocolGrpc = 0;
                long _spansFailuresTotalGrpc = 0;

                long _pacetSizeProtocolCompact = 0;
                long _pacetsProcessedTotalProtocolCompact = 0;
                long _pacetsDroppedTotalProtocolCompact = 0;

                if (metricsType.equals("expvar")) {
                    Map<String, Object> map = (Map<String, Object>) _agent;
                    removeKey(map);

                    _batchesSubmittedProtocolGrpc = getLong(map,
                            "jaeger.agent.reporter.batches.submitted.format_jaeger.protocol_grpc");
                    _spansSubmittedProtocolGrpc = getLong(map,
                            "jaeger.agent.reporter.spans.submitted.format_jaeger.protocol_grpc");
                    _spansFailuresTotalGrpc = getLong(map,
                            "jaeger.agent.reporter.spans.failures.format_jaeger.protocol_grpc");

                    _pacetSizeProtocolCompact = getLong(map,
                            "jaeger.agent.thrift.udp.server.packet_size.model_jaeger.protocol_compact");
                    _pacetsProcessedTotalProtocolCompact = getLong(map,
                            "jaeger.agent.thrift.udp.server.packets.processed.model_jaeger.protocol_compact");
                    _pacetsDroppedTotalProtocolCompact = getLong(map,
                            "jaeger.agent.thrift.udp.server.packets.dropped.model_jaeger.protocol_compact");

                } else if (metricsType.equals("prometheus")) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) _agent;

                    HashMap<String, String> labelFilter = new HashMap<>();
                    labelFilter.put("format", "jaeger");
                    labelFilter.put("protocol", "grpc");

                    _batchesSubmittedProtocolGrpc = getMetricValue(
                            list, "jaeger_agent_reporter_batches_submitted_total", labelFilter);
                    _spansSubmittedProtocolGrpc = getMetricValue(
                            list, "jaeger_agent_reporter_spans_submitted_total", labelFilter);
                    _spansFailuresTotalGrpc = getMetricValue(
                            list, "jaeger_agent_reporter_spans_failures_total", labelFilter);

                    // update protocol
                    labelFilter.put("protocol", "compact");

                    _pacetSizeProtocolCompact = getMetricValue(
                            list, "jaeger_agent_thrift_udp_server_packet_size", labelFilter);
                    _pacetsProcessedTotalProtocolCompact = getMetricValue(
                            list, "jaeger_agent_thrift_udp_server_packets_processed_total", labelFilter);
                    _pacetsDroppedTotalProtocolCompact = getMetricValue(
                            list, "jaeger_agent_thrift_udp_server_packets_dropped_total", labelFilter);
                }

                // add it into overall summary
                batchesSubmittedProtocolGrpc += _batchesSubmittedProtocolGrpc;
                spansSubmittedProtocolGrpc += _spansSubmittedProtocolGrpc;
                spansFailuresTotalGrpc += _spansFailuresTotalGrpc;
                pacetSizeProtocolCompact += _pacetSizeProtocolCompact;
                pacetsProcessedTotalProtocolCompact += _pacetsProcessedTotalProtocolCompact;

                Map<String, Object> pod = new HashMap<>();
                pod.put("batchesSubmittedProtocolGrpc", _batchesSubmittedProtocolGrpc);
                pod.put("spansFailuresTotalGrpc", _spansFailuresTotalGrpc);
                pod.put("spansSubmittedProtocolGrpc", _spansSubmittedProtocolGrpc);
                pod.put("pacetSizeProtocolCompact", _pacetSizeProtocolCompact);
                pod.put("pacetsProcessedTotalProtocolCompact", pacetsProcessedTotalProtocolCompact);
                pod.put("pacetsDroppedTotalProtocolCompact", _pacetsDroppedTotalProtocolCompact);

                // add it in to pods list
                podList.add(pod);
            }

            // update summary
            agentSummary.put("pod", podList);
            agentSummary.put("batchesSubmittedProtocolGrpc", batchesSubmittedProtocolGrpc);
            agentSummary.put("spansSubmittedProtocolGrpc", spansSubmittedProtocolGrpc);
            agentSummary.put("spansFailuresTotalGrpc", spansFailuresTotalGrpc);
            agentSummary.put("pacetSizeProtocolCompact", pacetSizeProtocolCompact);
            agentSummary.put("pacetsProcessedTotalProtocolCompact", pacetsProcessedTotalProtocolCompact);
            agentSummary.put("pacetsDroppedTotalProtocolCompact", pacetsDroppedTotalProtocolCompact);

            // update into summary
            summary.put("agent", agentSummary);
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateCollectorSummary(String metricsType) {
        try {
            Map<String, Object> collectorSummary = new HashMap<>();

            // update from collector service
            List<Map<String, Object>> podList = new ArrayList<>();
            long spansDropped = 0;
            long spansReceived = 0;
            long errorBusy = 0;
            for (Object _collector : collector) {
                long _spansDropped = 0;
                long _spansReceived = 0;
                long _errorBusy = 0;
                String _hostname = null;

                if (metricsType.equals("expvar")) {
                    Map<String, Object> map = (Map<String, Object>) _collector;
                    removeKey(map);

                    _spansDropped = getSumOfLong(get(map, "jaeger.collector.spans.dropped"));
                    _spansReceived = getSumOfLong(get(map, "jaeger.collector.spans.received"));
                    _errorBusy = getSumOfLong(get(map, "jaeger.collector.error.busy"));
                    _hostname = getHostname(map, "jaeger.collector.spans.dropped.host_");

                } else if (metricsType.equals("prometheus")) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) _collector;
                    _spansDropped = getMetricValue(list, "jaeger_collector_spans_dropped_total");
                    _spansReceived = getMetricValue(list, "jaeger_collector_spans_received_total");
                    _errorBusy = getMetricValue(list, "jaeger_collector_error_busy");
                    _hostname = getLabel(list, "jaeger_collector_spans_dropped_total", "host");
                }

                // add it into overall summary
                spansDropped += _spansDropped;
                spansReceived += _spansReceived;
                errorBusy += _errorBusy;

                Map<String, Object> pod = new HashMap<>();
                pod.put("hostname", _hostname);
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

            // update collector receive percentage
            for (Map<String, Object> pod : podList) {
                long podSpansReceived = (long) pod.get("spansReceived");
                pod.put("spansReceivedPercent",
                        Math.round(((podSpansReceived * 100.0) / spansReceived) * 100.0) / 100.0);
            }

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

    private long getLong(Map<String, Object> map, String prefix) {
        try {
            for (String key : map.keySet()) {
                if (key.startsWith(prefix)) {
                    return Long.valueOf(String.valueOf(map.get(key)));
                }
            }
        } catch (Exception ex) {
            logger.debug("Exception,", ex);
        }
        return 0L;
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

    private long getMetricValue(List<Map<String, Object>> list, String prefix) {
        return getMetricValue(list, prefix, null);
    }

    @SuppressWarnings("unchecked")
    private long getMetricValue(List<Map<String, Object>> list, String prefix, Map<String, String> labelFilter) {
        long value = 0;
        for (Map<String, Object> map : list) {
            String name = (String) map.get("name");
            if (name.equals(prefix)) {
                List<Map<String, Object>> metrics = (List<Map<String, Object>>) map.get("metrics");
                for (Map<String, Object> metric : metrics) {
                    if (labelFilter != null) {
                        boolean include = true;
                        Map<String, String> labels = (Map<String, String>) metric.get("labels");
                        for (String key : labelFilter.keySet()) {
                            if (labels.get(key) == null || !labels.get(key).equals(labelFilter.get(key))) {
                                include = false;
                                break;
                            }
                        }
                        if (include) {
                            value += Double.valueOf((String) metric.get("value")).longValue();
                        }
                    } else if (metric.get("value") != null) {
                        value += Double.valueOf((String) metric.get("value")).longValue();
                    }
                }
                break;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private String getLabel(List<Map<String, Object>> list, String prefix, String labelKey) {
        for (Map<String, Object> map : list) {
            String name = (String) map.get("name");
            if (name.equals(prefix)) {
                List<Map<String, Object>> metrics = (List<Map<String, Object>>) map.get("metrics");
                for (Map<String, Object> metric : metrics) {
                    if (metric.get("labels") != null) {
                        Map<String, Object> labels = (Map<String, Object>) metric.get("labels");
                        if (labels.get(labelKey) != null) {
                            return (String) labels.get(labelKey);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void removeKey(Map<String, Object> map) {
        for (String key : REMOVE_KEYS) {
            if (map.containsKey(key)) {
                map.remove(key);
            }
        }
    }

}
