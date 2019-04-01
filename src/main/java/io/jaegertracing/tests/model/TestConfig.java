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
package io.jaegertracing.tests.model;

import static io.jaegertracing.tests.TestUtils.getBooleanEnv;
import static io.jaegertracing.tests.TestUtils.getIntegerEnv;
import static io.jaegertracing.tests.TestUtils.getLongEnv;
import static io.jaegertracing.tests.TestUtils.getStringEnv;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Slf4j
public class TestConfig implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6130562311305075882L;

    public static TestConfig loadFromEnvironment() {
        return TestConfig
                .builder()
                .jobReference(getStringEnv("JOB_REFERENCE", ""))
                .jaegerServiceName(getStringEnv("JAEGER_SERVICE_NAME", "jaegerqe"))
                .OpenShiftUrl(getStringEnv("OS_URL", ""))
                .OpenShiftUsername(getStringEnv("OS_USERNAME", ""))
                .OpenShiftNamespace(getStringEnv("OS_NAMESPACE", ""))
                .testsToRun(getStringEnv("TESTS_TO_RUN", "performance,smoke"))
                .elasticsearchProvider(getStringEnv("ELASTICSEARCH_PROVIDER", "es-operator"))
                .storageHost(getStringEnv("STORAGE_HOST", "elasticsearch"))
                .storagePort(getIntegerEnv("STORAGE_PORT", "9200"))
                .preInstallJaegerOperator(getBooleanEnv("PRE_INSTALL_JAEGER_OPERATOR", "false"))
                .preInstallJaegerServices(getBooleanEnv("PRE_INSTALL_JAEGER_SERVICES", "false"))
                .preInstallReporterNodes(getBooleanEnv("PRE_INSTALL_REPORTER_NODES", "false"))
                .reporterNodeReplicaCount(getIntegerEnv("REPORTER_NODE_REPLICA_COUNT", "5"))
                .reporterReference(getStringEnv("REPORTER_REFERENCE", "global"))
                .postDeleteJaegerServices(getBooleanEnv("POST_DELETE_JAEGER_SERVICES", "false"))
                .postDeleteTestJobs(getBooleanEnv("POST_DELETE_TEST_JOBS", "false"))
                .postDeleteReporterNodes(getBooleanEnv("POST_DELETE_REPORTER_NODES", "false"))
                .testHAsetup(getBooleanEnv("TEST_HA_SETUP", "false"))
                .jaegerqeControllerUrl(getStringEnv("JAEGERQE_CONTROLLER_URL", "http://localhost:8080"))
                .reportEngineUrl(getStringEnv("REPORT_ENGINE_URL", "http://localhost:8080"))
                .reportEngineLabel(getStringEnv("REPORT_ENGINE_LABELS", "{ }"))
                .imagePerformanceTest(getStringEnv("IMAGE_PERFORMANCE_TEST", "jkandasa/jaeger-performance-test:2.2.0"))
                .imageElsaticsearchOperator(getStringEnv("IMAGE_ELASTICSEARCH_OPERATOR",
                        "quay.io/openshift/origin-elasticsearch-operator:latest"))
                .imageElsaticsearch(
                        getStringEnv("IMAGE_ELASTICSEARCH", "registry.centos.org/rhsyseng/elasticsearch:5.6.10"))
                .imageJaegerOperator(getStringEnv("IMAGE_JAEGER_OPERATOR", "jaegertracing/jaeger-operator:master"))
                .imageJaegerAllInOne(getStringEnv("IMAGE_JAEGER_ALL_IN_ONE", "jaegertracing/all-in-one:latest"))
                .imageJaegerAgent(getStringEnv("IMAGE_JAEGER_AGENT", "jaegertracing/jaeger-agent:latest"))
                .imageJaegerCollector(getStringEnv("IMAGE_JAEGER_COLLECTOR", "jaegertracing/jaeger-collector:latest"))
                .imageJaegerQuery(getStringEnv("IMAGE_JAEGER_QUERY", "jaegertracing/jaeger-query:latest"))
                .useInternalReporter(getBooleanEnv("USE_INTERNAL_REPORTER", "true"))
                .nodeCountSpansReporter(getIntegerEnv("NODE_COUNT_SPANS_REPORTER", "1"))
                .nodeCountQueryRunner(getIntegerEnv("NODE_COUNT_QUERY_RUNNER", "1"))
                .mqttBrokerHost(getStringEnv("MSG_BROKER_HOST", "localhost"))
                .mqttBrokerPort(getIntegerEnv("MSG_BROKER_PORT", "1883"))
                .mqttBrokerUsername(getStringEnv("MSG_BROKER_USER", "guest"))
                .numberOfTracers(getIntegerEnv("NUMBER_OF_TRACERS", "10"))
                .numberOfSpans(getIntegerEnv("NUMBER_OF_SPANS", "10"))
                .reportSpansDuration(getStringEnv("REPORT_SPANS_DURATION", "10m"))
                .spansCountFrom(getStringEnv("SPANS_COUNT_FROM", "storage"))
                .queryLimit(getIntegerEnv("QUERY_LIMIT", "2000"))
                .querySamples(getIntegerEnv("QUERY_SAMPLES", "5"))
                .queryInterval(getIntegerEnv("QUERY_INTERVAL", "-1"))
                .sender(getStringEnv("SENDER", "udp"))
                .reporterType(getStringEnv("REPORTER_TYPE", "gprc"))
                .metricsBackend(getStringEnv("METRICS_BACKEND", "expvar"))
                .jaegerAgentQueueSize(getLongEnv("JAEGER_AGENT_QUEUE_SIZE", "1000"))
                .jaegerAgentWorkers(getIntegerEnv("JAEGER_AGENT_WORKERS", "10"))
                .jaegerClientFlushInterval(getIntegerEnv("JAEGER_CLIENT_FLUSH_INTERVAL", "200"))
                .jaegerClientMaxPocketsize(getIntegerEnv("JAEGER_CLIENT_MAX_POCKET_SIZE", "0"))
                .jaegerClientMaxQueueSize(getIntegerEnv("JAEGER_CLIENT_MAX_QUEUE_SIZE", "10000"))
                .collectorReplicaCount(getIntegerEnv("COLLECTOR_REPLICA_COUNT", "1"))
                .collectorQueueSize(getLongEnv("COLLECTOR_QUEUE_SIZE", "2000"))
                .collectorNumWorkers(getIntegerEnv("COLLECTOR_NUM_WORKERS", "50"))
                .collectorEsBulkSize(getLongEnv("COLLECTOR_ES_BULK_SIZE", "5000000"))
                .collectorEsBulkWorkers(getIntegerEnv("COLLECTOR_ES_BULK_WORKERS", "1"))
                .collectorEsBulkFlushInterval(getStringEnv("COLLECTOR_ES_BULK_FLUSH_INTERVAL", "200ms"))
                .collectorEsTagsAsFieldsAll(getBooleanEnv("COLLECTOR_ES_TAGS_AS_FIELDS", "false"))
                .jaegerQueryStaticFiles(getStringEnv("JAEGER_QUERY_STATIC_FILES", null))
                .esMemory(getStringEnv("ES_MEMORY", "1Gi"))
                .logLevelJaegerAgent(getStringEnv("LOG_LEVEL_JAEGER_AGENT", "info"))
                .logLevelJaegerCollector(getStringEnv("LOG_LEVEL_JAEGER_COLLECTOR", "info"))
                .logLevelJaegerOperator(getStringEnv("LOG_LEVEL_JAEGER_OPERATOR", "info"))
                .logLevelJaegerQuery(getStringEnv("LOG_LEVEL_JAEGER_QUERY", "info"))
                .resourceLimitJaegerAgentCpu(getStringEnv("RESO_LMT_AGENT_CPU", null))
                .resourceLimitJaegerAgentMemory(getStringEnv("RESO_LMT_AGENT_MEM", null))
                .resourceLimitJaegerCollectorCpu(getStringEnv("RESO_LMT_COLLECTOR_CPU", null))
                .resourceLimitJaegerCollectorMemory(getStringEnv("RESO_LMT_COLLECTOR_MEM", null))
                .resourceLimitJaegerQueryCpu(getStringEnv("RESO_LMT_QUERY_CPU", null))
                .resourceLimitJaegerQueryMemory(getStringEnv("RESO_LMT_QUERY_MEM", null))
                .runningOnOpenshift(getBooleanEnv("RUNNING_ON_OPENSHIFT", "false"))
                .logsDirectory(getStringEnv("LOGS_DIRECTORY", "logs/"))
                .jaegerAgentHost(getStringEnv("JAEGER_AGENT_HOST", "localhost"))
                .jaegerAgentPort(getIntegerEnv("JAEGER_AGENT_PORT", "6831"))
                .jaegerAgentCollectorPort(getIntegerEnv("JAEGER_AGENT_COLLECTOR_PORT", "14250"))
                .jaegerCollectorHost(getStringEnv("JAEGER_COLLECTOR_HOST", "localhost"))
                .jaegerCollectorPort(getIntegerEnv("JAEGER_COLLECTOR_PORT", "6831"))
                .jaegerQueryHost(getStringEnv("JAEGER_QUERY_HOST", "localhost"))
                .jaegerQueryPort(getIntegerEnv("JAEGER_QUERY_PORT", "6831"))
                .build();
    }

    public static TestConfig get() {
        String testConfigFile = System.getProperty("TEST_CONFIG_FILE");
        if (testConfigFile == null) {
            testConfigFile = getStringEnv("TEST_CONFIG_FILE", "environment");
        }
        if (!testConfigFile.equalsIgnoreCase("environment")) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try {
                TestConfig config = mapper.readValue(FileUtils.getFile(testConfigFile), TestConfig.class);
                // update defaults
                config.setRunningOnOpenshift(false);

                return config;
            } catch (IOException ex) {
                logger.error("Exception,", ex);
            }
        }
        return loadFromEnvironment();
    }

    private String jobReference;
    private String jaegerServiceName;

    private String OpenShiftUrl;
    private String OpenShiftUsername;
    private String OpenShiftNamespace;

    private String testsToRun;

    private String elasticsearchProvider;
    private String storageHost;
    private Integer storagePort;

    private Boolean preInstallJaegerOperator;
    private Boolean preInstallJaegerServices;
    private Boolean preInstallReporterNodes;
    private Integer reporterNodeReplicaCount;
    private String reporterReference;
    private Boolean postDeleteJaegerServices;
    private Boolean postDeleteTestJobs;
    private Boolean postDeleteReporterNodes;

    private Boolean testHAsetup;

    private String jaegerqeControllerUrl;
    private String reportEngineUrl;
    private String reportEngineLabel;

    // images
    private String imagePerformanceTest;
    private String imageElsaticsearchOperator;
    private String imageElsaticsearch;
    private String imageJaegerOperator;
    private String imageJaegerAllInOne;
    private String imageJaegerAgent;
    private String imageJaegerCollector;
    private String imageJaegerQuery;

    private Boolean useInternalReporter;
    private Integer nodeCountSpansReporter;
    private Integer nodeCountQueryRunner;

    private String mqttBrokerHost;
    private Integer mqttBrokerPort;
    private String mqttBrokerUsername;

    private Integer numberOfTracers;
    private Integer numberOfSpans;
    private String reportSpansDuration;
    private String spansCountFrom;

    private Integer queryLimit;
    private Integer querySamples;
    private Integer queryInterval; // in seconds, -1 means, run only at the end

    private String sender;
    private String reporterType;
    private String metricsBackend;

    private Long jaegerAgentQueueSize;
    private Integer jaegerAgentWorkers;

    private Integer jaegerClientFlushInterval;
    private Integer jaegerClientMaxPocketsize;
    private Integer jaegerClientMaxQueueSize;

    private Integer collectorReplicaCount;
    private Long collectorQueueSize;
    private Integer collectorNumWorkers;
    private Long collectorEsBulkSize;
    private Integer collectorEsBulkWorkers;
    private String collectorEsBulkFlushInterval;
    private Boolean collectorEsTagsAsFieldsAll;

    private String jaegerQueryStaticFiles;

    private String esMemory;

    private String logLevelJaegerAgent;
    private String logLevelJaegerCollector;
    private String logLevelJaegerOperator;
    private String logLevelJaegerQuery;

    private String resourceLimitJaegerAgentCpu;
    private String resourceLimitJaegerAgentMemory;
    private String resourceLimitJaegerCollectorCpu;
    private String resourceLimitJaegerCollectorMemory;
    private String resourceLimitJaegerQueryCpu;
    private String resourceLimitJaegerQueryMemory;

    private Jenkins jenkins;

    private Boolean runningOnOpenshift;
    private String logsDirectory;

    // Jaeger services details
    private String jaegerAgentHost;
    private Integer jaegerAgentPort;
    private Integer jaegerAgentCollectorPort;

    private String jaegerQueryHost;
    private Integer jaegerQueryPort;

    private String jaegerCollectorHost;
    private Integer jaegerCollectorPort;

    private String jaegerClientVersion;

    public Float getJaegerSamplingRate() {
        return 1.0F;
    }

    public boolean isPerformanceTestEnabled() {
        if (testsToRun.contains("performance")) {
            return true;
        }
        return false;
    }

    public boolean isSmokeTestEnabled() {
        if (testsToRun.contains("smoke")) {
            return true;
        }
        return false;
    }

    public Long getSpansReportDurationInMillisecond() {
        Long number = Long.valueOf(reportSpansDuration.replaceAll("[^0-9]", ""));
        Long timestamp = null;
        if (reportSpansDuration.endsWith("s")) {
            timestamp = number * 1000L;
        } else if (reportSpansDuration.endsWith("m")) {
            timestamp = number * 1000L * 60;
        } else if (reportSpansDuration.endsWith("h")) {
            timestamp = number * 1000L * 60 * 60;
        } else if (reportSpansDuration.endsWith("d")) {
            timestamp = number * 1000L * 60 * 60 * 24;
        } else {
            timestamp = number;
        }
        return timestamp;
    }

    public int getSpansReportDurationInSecond() {
        return (int) (getSpansReportDurationInMillisecond() / 1000L);
    }

    public Jenkins getJenkins() {
        if (jenkins == null) {
            jenkins = new Jenkins();
        }
        return jenkins;
    }

    @JsonIgnore
    public HashMap<String, Object> getMap() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("spansCount", numberOfSpans);
        data.put("tracersCount", numberOfTracers);
        data.put("sender", sender);
        data.put("jaegerCollectorHost", jaegerCollectorHost);
        data.put("jaegerCollectorPort", jaegerCollectorPort);
        data.put("jaegerAgentHost", jaegerAgentHost);
        data.put("jaegerAgentPort", jaegerAgentPort);
        data.put("jaegerSamplingRate", 1.0);
        data.put("jaegerFlushInterval", jaegerClientFlushInterval);
        data.put("jaegerMaxPocketSize", jaegerClientMaxPocketsize);
        data.put("jaegerMaxQueueSize", jaegerClientMaxQueueSize);
        data.put("endTime", reportSpansDuration);
        data.put("jaegerQueryHost", jaegerQueryHost);
        data.put("jaegerQueryPort", jaegerQueryPort);
        data.put("jaegerQueryLimit", queryLimit);
        data.put("jaegerQuerySamples", querySamples);
        data.put("jaegerQueryInterval", queryInterval);
        data.put("reportEngineUrl", reportEngineUrl);
        data.put("queryHostCount", nodeCountQueryRunner);
        data.put("reporterHostCount", nodeCountSpansReporter);
        return data;
    }
}
