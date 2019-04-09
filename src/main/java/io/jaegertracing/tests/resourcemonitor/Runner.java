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

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.MetricFamily;

import io.jaegertracing.tests.clients.ClientUtils;
import io.jaegertracing.tests.clients.ReportEngineClient;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Timer TIMER = new Timer();
    private static final HashMap<String, List<String>> DOMAIN_IPS = new HashMap<>();
    private static TestConfig config = TestConfig.get();

    public static boolean isRunning() {
        return RUNNING.get();
    }

    private static HashMap<String, Object> getReAgentData(String command) {
        HashMap<String, Object> data = new HashMap<>();
        ArrayList<String> resources = new ArrayList<>();
        resources.add("cpu");
        resources.add("memory");
        resources.add("swap");
        resources.add("jvm,org.elasticsearch.bootstrap.Elasticsearch");

        data.put("resources", resources);

        data.put("mqttTopic", "re/agent");
        data.put("agentReference", config.getReportEngineAgentReference());
        data.put("command", command);
        data.put("suiteId", ReportFactory.getReSuiteId());
        data.put("measurementSuffix", "external");
        data.put("monitorInterval", "15s");
        data.put("endTime", String.valueOf((config.getSpansReportDurationInSecond() + 120) * 1000L)); // duration in milliseconds
        return data;
    }

    public static void start() {
        if (RUNNING.get()) {
            return;
        }
        try {
            updateIps();
            TIMER.schedule(new MonitorResouces(), 10000L, 10 * 1000L);
            if (config.getReportEngineAgentReference() != null
                    && config.getReportEngineAgentReference().trim().length() > 0) {
                ClientUtils.qeCtlClient().postMqttTopic(getReAgentData("start"));
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        } finally {
            RUNNING.set(true);
        }
    }

    public static void stop() {
        if (!RUNNING.get()) {
            return;
        }
        try {
            TIMER.cancel();
            if (config.getReportEngineAgentReference() != null
                    && config.getReportEngineAgentReference().trim().length() > 0) {
                ClientUtils.qeCtlClient().postMqttTopic(getReAgentData("stop"));
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        } finally {
            RUNNING.set(false);
        }
    }

    private static List<String> getIPs(String domin) {
        List<String> ips = new ArrayList<>();
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            InetAddress[] machines = InetAddress.getAllByName(domin);
            for (InetAddress address : machines) {
                ips.add(address.getHostAddress());
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
        return ips;
    }

    private static void updateIps() {
        String fqName = String.format("%s.svc.cluster.local", config.getOpenShiftNamespace());

        // clear all IPs
        DOMAIN_IPS.clear();
        // update collector ips
        DOMAIN_IPS.put("collector",
                getIPs(String.format("%s-collector-headless.%s", config.getJaegerServiceName(), fqName)));

        // update query IPs
        DOMAIN_IPS.put("query", getIPs(String.format("%s-query.%s", config.getJaegerServiceName(), fqName)));

        // update agent ips
        List<String> agentIPs = new ArrayList<>();
        if (config.getQueryInterval() > 0) {
            agentIPs.addAll(getIPs(String.format("%s-query.%s", config.getJaegerServiceName(), fqName)));
        }
        if (config.getUseInternalReporter()) {
            agentIPs.add("localhost");
        } else {
            agentIPs.addAll(getIPs(String.format("jaegerqe-spans-reporter-headless.%s", fqName)));
        }
        // update agent IPs
        DOMAIN_IPS.put("agent", agentIPs);

        logger.info("Domain IPs: {}", DOMAIN_IPS);
    }

    static class MonitorResouces extends TimerTask {

        private static final int AGENT_PORT = 5778;
        private static final int COLLECTOR_PORT = 14268;

        private static final String[] COUNTER_COLLECTOR = {
                "jaeger_collector_spans_received_total",
                "jaeger_collector_spans_rejected_total",
                "jaeger_collector_spans_saved_by_svc_total",
                "jaeger_collector_traces_received_total",
                "jaeger_collector_traces_rejected_total",
                "jaeger_collector_traces_saved_by_svc_total",
                "jaeger_index_create_attempts_total",
                "jaeger_index_create_errors_total",
                "jaeger_index_create_inserts_total",
                "jaeger_bulk_index_attempts_total",
                "jaeger_bulk_index_errors_total",
                "jaeger_bulk_index_inserts_total",
                "process_cpu_seconds_total"
        };
        private static final String[] GAUGE_COLLECTOR = {
                "jaeger_collector_batch_size",
                "jaeger_collector_queue_length",
                "jaeger_collector_spans_serviceNames",
                "process_max_fds",
                "process_open_fds",
                "process_resident_memory_bytes",
                "process_virtual_memory_bytes"
        };
        private static final String[] COUNTER_AGENT = {
                "jaeger_agent_http_server_errors_total",
                "jaeger_agent_http_server_requests_total",
                "jaeger_agent_reporter_batches_failures_total",
                "jaeger_agent_reporter_batches_submitted_total",
                "jaeger_agent_reporter_spans_failures_total",
                "jaeger_agent_reporter_spans_submitted_total",
                "jaeger_agent_thrift_udp_server_packets_dropped_total",
                "jaeger_agent_thrift_udp_server_packets_processed_total",
                "jaeger_agent_thrift_udp_server_read_errors_total",
                "jaeger_agent_thrift_udp_t_processor_handler_errors_total",
                "process_cpu_seconds_total"
        };
        private static final String[] GAUGE_AGENT = {
                "jaeger_agent_reporter_batch_size",
                "jaeger_agent_thrift_udp_server_packet_size",
                "jaeger_agent_thrift_udp_server_queue_size",
                "process_max_fds",
                "process_open_fds",
                "process_resident_memory_bytes",
                "process_virtual_memory_bytes"
        };

        private long timestamp = 0;

        private String url(String ip, int port) {
            return String.format("http://%s:%d/metrics", ip, port);
        }

        private void updateCollectorMetric(List<ReMetric> metrics, String ip) {
            try {
                if (config.getMetricsBackend().equals("prometheus")) {
                    PrometheusScraper scraper = new PrometheusScraper(new URL(url(ip, COLLECTOR_PORT)));
                    List<MetricFamily> metricFamilies = scraper.scrape();

                    // add counter metrics
                    ReMetric counter = ReMetric.builder()
                            .suiteId(ReportFactory.getReSuiteId())
                            .measurementSuffix("collector_counter")
                            .timestamp(timestamp)
                            .build();
                    // add hostname
                    String hostname = MetricUtils.getLabel(
                            metricFamilies, "jaeger_collector_spans_dropped_total", "host");
                    if (hostname == null) {
                        hostname = "";
                    }
                    counter.getLabels().put("ip", ip);
                    counter.getLabels().put("hostname", hostname);

                    for (String name : COUNTER_COLLECTOR) {
                        counter.getData().put(name.replace("jaeger_", ""),
                                MetricUtils.getCounterValue(metricFamilies, name));
                    }

                    // add gauge metrics
                    ReMetric gauge = ReMetric.builder()
                            .suiteId(ReportFactory.getReSuiteId())
                            .measurementSuffix("collector_gauge")
                            .timestamp(timestamp)
                            .build();
                    // add hostname
                    gauge.getLabels().put("ip", ip);
                    gauge.getLabels().put("hostname", hostname);

                    for (String name : GAUGE_COLLECTOR) {
                        gauge.getData().put(name.replace("jaeger_", ""),
                                MetricUtils.getGaugeValue(metricFamilies, name));
                    }

                    // add collected metrics
                    metrics.add(counter);
                    metrics.add(gauge);
                }
            } catch (Exception ex) {
                logger.error("Exception,", ex);
            }
        }

        private void updateAgentMetric(List<ReMetric> metrics, String ip) {
            try {
                if (config.getMetricsBackend().equals("prometheus")) {
                    PrometheusScraper scraper = new PrometheusScraper(new URL(url(ip, AGENT_PORT)));
                    List<MetricFamily> metricFamilies = scraper.scrape();

                    // add counter metrics
                    ReMetric counter = ReMetric.builder()
                            .suiteId(ReportFactory.getReSuiteId())
                            .measurementSuffix("agent_counter")
                            .timestamp(timestamp)
                            .build();
                    counter.getLabels().put("ip", ip);

                    for (String name : COUNTER_AGENT) {
                        counter.getData().put(name.replace("jaeger_", ""),
                                MetricUtils.getCounterValue(metricFamilies, name));
                    }

                    // add gauge metrics
                    ReMetric gauge = ReMetric.builder()
                            .suiteId(ReportFactory.getReSuiteId())
                            .measurementSuffix("agent_gauge")
                            .timestamp(timestamp)
                            .build();
                    // add hostname
                    gauge.getLabels().put("ip", ip);

                    for (String name : GAUGE_AGENT) {
                        gauge.getData().put(name.replace("jaeger_", ""),
                                MetricUtils.getGaugeValue(metricFamilies, name));
                    }

                    // add collected metrics
                    metrics.add(counter);
                    metrics.add(gauge);
                }
            } catch (Exception ex) {
                logger.error("Exception, ip:{}", ip, ex);
            }
        }

        @Override
        public void run() {
            timestamp = System.currentTimeMillis();
            List<ReMetric> metrics = new ArrayList<>();
            ReportEngineClient reClient = ClientUtils.newReClient();
            try {
                // collector metrics
                for (String ip : DOMAIN_IPS.get("collector")) {
                    updateCollectorMetric(metrics, ip);
                }

                // agent metrics
                for (String ip : DOMAIN_IPS.get("agent")) {
                    updateAgentMetric(metrics, ip);
                }
                if (metrics.size() > 0) {
                    reClient.postMetrics(metrics);
                    logger.debug("Metrics updated. Size:{}", metrics.size());
                } else {
                    logger.warn("No metrics found");
                }
            } catch (Exception ex) {
                logger.error("Exception,", ex);
            } finally {
                reClient.close();
            }
        }
    }

}
