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
package io.jaegertracing.tests.report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.runner.Result;

import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.jaegertracing.tests.ReportEngineUtils;

import io.jaegertracing.client.Version;
import io.jaegertracing.tests.JsonUtils;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.model.TestSuiteStatus;
import io.jaegertracing.tests.report.model.JaegerTestReport;

public class ReportFactory {
    // final report
    private static final JaegerTestReport TEST_REPORT = JaegerTestReport.builder().build();

    private static final MetricRegistry METRICS_REGISTRY = new MetricRegistry();

    private static long spanCountSent = -1;
    private static long spanCountFound = -1;
    private static long spanCountSentByQuery = 0;
    private static long spansLatency = 0;

    public static JaegerTestReport getFinalReport(TestConfig config) {
        updateReport(config);
        return TEST_REPORT;
    }

    public static String getReSuiteId() {
        return TEST_REPORT.getId();
    }

    public static String getReporterReference() {
        return System.getenv("REPORTER_REFERENCE");
    }

    public static String getFinalReportAsString(TestConfig config) {
        updateReport(config);
        return JsonUtils.asString(TEST_REPORT);
    }

    public static void saveFinalReport(TestConfig config, String filename) {
        updateReport(config);
        // save it in file
        JsonUtils.dumps(TEST_REPORT, filename);
    }

    public static Timer timer(String name) {
        return METRICS_REGISTRY.timer(name);
    }

    public static void triggeredJaegerApiQuery() {
        long spansCount = 0;
        if (TEST_REPORT.getData().getConfig().getUseInternalReporter()) {
            // number of samples * number of spans for per trigger * spans per query
            spansCount = TEST_REPORT.getData().getConfig().getQuerySamples() * 12L * 5L;
        } else {
            // number of query host * number of samples * number of spans for per trigger * spans per query
            spansCount = TEST_REPORT.getData().getConfig().getNodeCountQueryRunner()
                    * TEST_REPORT.getData().getConfig().getQuerySamples() * 12L * 5L;
        }
        spanCountSentByQuery += spansCount;
    }

    private static void updateReport(TestConfig config) {
        // add test configurations
        TEST_REPORT.getData().setConfig(config);

        // update custom labels
        TEST_REPORT.getLabels().putAll(ReportEngineUtils.labels(config.getReportEngineLabel()));

        // update Jaeger java client version
        TEST_REPORT.getData().getConfig().setJaegerClientVersion(Version.get());

        // disable metric attributes
        Set<MetricAttribute> disabledMetric = new HashSet<>();
        disabledMetric.add(MetricAttribute.MEAN_RATE);
        disabledMetric.add(MetricAttribute.M1_RATE);
        disabledMetric.add(MetricAttribute.M5_RATE);
        disabledMetric.add(MetricAttribute.M15_RATE);

        // update metrics
        TEST_REPORT.getData().setMetric(
                JsonReporter.forRegistry(METRICS_REGISTRY)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .disabledMetricAttributes(disabledMetric)
                        .build().getReport());

        // update count statistics
        TEST_REPORT.getData().setSpansCountStatistics(new HashMap<String, Object>());
        TEST_REPORT.getData().getSpansCountStatistics().put("sent", getSpansSent());
        TEST_REPORT.getData().getSpansCountStatistics().put("sent_by_reporer", spanCountSent);
        TEST_REPORT.getData().getSpansCountStatistics().put("sent_by_query", spanCountSentByQuery);
        TEST_REPORT.getData().getSpansCountStatistics().put("found", spanCountFound);

        TEST_REPORT.getData().getSpansCountStatistics().put("spansLatency", spansLatency);

        long dropped_count = getDroupCount();
        double dropped_percentage = getDroupPercentage();

        TEST_REPORT.getData().getSpansCountStatistics().put("dropped_count", dropped_count);
        TEST_REPORT.getData().getSpansCountStatistics().put("dropped_percentage",
                dropped_percentage < 0 ? 0 : Math.round(dropped_percentage * 100.0) / 100.0);

        final int spansPersecond = getSpansPerSecond(config);
        TEST_REPORT.getData().getSpansCountStatistics().put("per_second", spansPersecond);
        TEST_REPORT.getData().getSpansCountStatistics()
                .put("per_minute", spansPersecond != -1 ? spansPersecond * 60 : -1);
    }

    public static int getSpansPerSecond(TestConfig config) {
        final int spansPersecond;
        if (config.getUseInternalReporter()) {
            spansPersecond = config.getNumberOfTracers() * config.getNumberOfSpans();
        } else {
            spansPersecond = config.getNodeCountSpansReporter() * config.getNumberOfTracers()
                    * config.getNumberOfSpans();
        }
        return spansPersecond;
    }

    public static void updateSpansCountByReporter(long expected) {
        spanCountSent = expected;
    }

    public static void updateSpansCount(long sent, long found) {
        spanCountSent = sent;
        spanCountFound = found;
    }

    public static void updateSpansLatency(long latency) {
        spansLatency = latency;
    }

    public static long getDroupCount() {
        return getSpansSent() - spanCountFound;
    }

    public static double getDroupPercentage() {
        return ((double) getDroupCount() / getSpansSent()) * 100.0;
    }

    public static long getSpansSent() {
        return spanCountSent + spanCountSentByQuery;
    }

    public static long getSpansFound() {
        return spanCountFound;
    }

    public static void updateTestSuiteStatus(String name, Result testResult) {
        if (TEST_REPORT.getData().getTestSuiteStatus() == null) {
            TEST_REPORT.getData().setTestSuiteStatus(new HashMap<>());
        }
        TEST_REPORT.getData().getTestSuiteStatus().put(name, TestSuiteStatus.get(name, testResult));
    }

    public static TestSuiteStatus gettestSuiteStatus(String name) {
        if (TEST_REPORT.getData().getTestSuiteStatus() == null) {
            TEST_REPORT.getData().setTestSuiteStatus(new HashMap<>());
        }
        return TEST_REPORT.getData().getTestSuiteStatus().get(name);
    }
}
