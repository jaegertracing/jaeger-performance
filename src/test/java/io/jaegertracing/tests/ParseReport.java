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
package io.jaegertracing.tests;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import io.jaegertracing.tests.report.model.JaegerMetrics;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import io.jaegertracing.tests.report.model.JaegerTestReport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParseReport {
    private static JaegerTestReport _REPORT;

    @SuppressWarnings("unchecked")
    private static void updateJaegerServiceMetrics(TestConfig config) {
        // check is this local run on OpenShift run
        if (config.getRunningOnOpenshift()) {
            // filter json files
            String[] filter = new String[] { "json" };
            List<File> files = (List<File>) FileUtils.listFiles(new File(config.getLogsDirectory()), filter,
                    true);
            JaegerMetrics jaegerMetrics = JaegerMetrics.builder().build();
            for (File _file : files) {
                try {
                    logger.info("Metrics file found, location: {}", _file.getCanonicalPath());
                    if (_file.getName().contains("metrics-query")) {
                        Map<String, Object> metrics = (Map<String, Object>) JsonUtils.loads(HashMap.class, _file);
                        jaegerMetrics.getQuery().add(metrics);
                    } else if (_file.getName().contains("metrics-agent")) {
                        Map<String, Object> metrics = (Map<String, Object>) JsonUtils.loads(HashMap.class, _file);
                        jaegerMetrics.getAgent().add(metrics);
                    } else if (_file.getName().contains("metrics-collector")) {
                        Map<String, Object> metrics = (Map<String, Object>) JsonUtils.loads(HashMap.class, _file);
                        jaegerMetrics.getCollector().add(metrics);
                    }
                } catch (Exception ex) {
                    logger.error("Exception,", ex);
                }
            }
            jaegerMetrics.updateSummary();
            _REPORT.getData().getMetric().setJaegerMetrics(jaegerMetrics);

            // update dropped in collector
            if (jaegerMetrics.getSummary().containsKey("collector")) {
                Map<String, Object> collecor = (Map<String, Object>) jaegerMetrics.getSummary().get("collector");
                if (collecor.get("spansDropped") != null) {
                    double droppedCollector = Double.valueOf(String.valueOf(collecor.get("spansDropped")));
                    double sent = Double.valueOf(String.valueOf(_REPORT.getData().getSpansCountStatistics()
                            .get("sent")));
                    double droppedCollectorPercent = (droppedCollector * 100) / sent;
                    _REPORT.getData().getSpansCountStatistics()
                            .put("dropped_percentage_collector", Math.round(droppedCollectorPercent * 100.0) / 100.0);
                }
            }

            // send it to report engine
            ReportEngineClient reClient = new ReportEngineClient(config.getReportEngineUrl());
            _REPORT.setReady(true);
            _REPORT.getData().getConfig().getJenkins().update();

            // update labels
            HashMap<String, String> labels = new HashMap<>();

            labels.put("imageCollector", config.getImageCollector());
            labels.put("imageAgent", config.getImageAgent());
            labels.put("imageQuery", config.getImageQuery());
            labels.put("jaegerClientVersion", config.getJaegerClientVersion());
            labels.put("imageQuery", config.getImageQuery());
            labels.put("jenkinsBuildDate", config.getJenkins().getBuildDate());
            labels.put("jenkinsJobName", config.getJenkins().getJobName());
            labels.put("collectorPods", String.valueOf(config.getCollectorPods()));
            labels.put("collectorQueueSize", String.valueOf(config.getCollectorQueueSize()));
            labels.put("collectorWorkers", String.valueOf(config.getCollectorWorkersCount()));
            labels.put("esMemory", config.getEsMemory());
            labels.put("esBulkSize", String.valueOf(config.getEsBulkSize()));
            labels.put("esBulkWorkers", String.valueOf(config.getEsBulkWorkers()));

            if (System.getenv().containsKey("JOB_REFERENCE")) {
                labels.put("jobReference", System.getenv("JOB_REFERENCE"));
            }

            // update labels into original list
            _REPORT.getLabels().putAll(labels);

            // update custom labels from user input
            _REPORT.getLabels().putAll(ReportEngineUtils.labels(config.getReportEngineLabel()));

            reClient.updateTestData(_REPORT);
        }
    }

    public static JaegerTestReport report() {
        if (_REPORT == null) {
            TestConfig config = TestConfig.get();
            // check is this local run on OpenShift run
            if (config.getRunningOnOpenshift()) {
                // parse report from log file
                String[] filter = new String[] { "log" };
                List<File> files = (List<File>) FileUtils.listFiles(new File(config.getLogsDirectory()), filter,
                        true);
                for (File _file : files) {
                    if (_file.getName().contains("jaeger-performance-test-job")) {
                        try {
                            logger.info("Report found, location: {}", _file.getCanonicalPath());
                            String content = FileUtils.readFileToString(_file, "UTF-8");
                            int beginIndex = content.indexOf("@@START@@");
                            int endIndex = content.indexOf("@@END@@");
                            if (beginIndex != -1 && endIndex != -1) {
                                beginIndex += 9;
                                logger.debug("File content:{}", content.substring(beginIndex, endIndex));
                                _REPORT = (JaegerTestReport) JsonUtils.loadFromString(
                                        content.substring(beginIndex, endIndex),
                                        JaegerTestReport.class);
                                // update jaeger service metrics
                                updateJaegerServiceMetrics(config);
                                // dump this json
                                JsonUtils.dumps(_REPORT, _file.getParent(), "jaeger-performance-result.json");
                                break;
                            }
                        } catch (Exception ex) {
                            logger.error("Exception,", ex);
                        }
                    }
                }
            } else {
                _REPORT = ReportFactory.getFinalReport(config);
            }
        }
        return _REPORT;
    }
}
