/**
 * Copyright 2018 The Jaeger Authors
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
import static io.jaegertracing.tests.TestUtils.getStringEnv;

import java.io.IOException;

import org.apache.commons.io.FileUtils;

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
public class TestConfig {

    public static TestConfig loadFromEnvironment() {
        return TestConfig
                .builder()
                .testsToRun(getStringEnv("TESTS_TO_RUN", "performance,smoke"))
                .performanceTestData(getStringEnv("PERFORMANCE_TEST_DATA", "quick,50"))
                .tracersCount(getIntegerEnv("NUMBER_OF_TRACERS", "50"))
                .spansCount(getIntegerEnv("NUMBER_OF_SPANS", "10000"))
                .runningOnOpenshift(getBooleanEnv("RUNNING_ON_OPENSHIFT", "false"))
                .logsDirectory(getStringEnv("LOGS_DIRECTORY", "logs/"))
                .queryLimit(getIntegerEnv("QUERY_LIMIT", "20000"))
                .querySamples(getIntegerEnv("QUERY_SAMPLES", "5"))
                .queryInterval(getIntegerEnv("QUERY_INTERVAL", "-1"))
                .sender(getStringEnv("SENDER", "http"))
                .storageType(getStringEnv("STORAGE_TYPE", "elasticsearch"))
                .spansCountFrom(getStringEnv("SPANS_COUNT_FROM", "jaeger-query"))
                .storageHost(getStringEnv("STORAGE_HOST", "localhost"))
                .storagePort(getIntegerEnv("STORAGE_PORT", "9200"))
                .storageKeyspace(getStringEnv("STORAGE_KEYSPACE", "keyspace"))
                .jaegerQueryHost(getStringEnv("JAEGER_QUERY_HOST", "localhost"))
                .jaegerQueryPort(getIntegerEnv("JAEGER_QUERY_PORT", "16686"))
                .jaegerCollectorHost(getStringEnv("JAEGER_COLLECTOR_HOST", "localhost"))
                .jaegerCollectorPort(getIntegerEnv("JAEGER_COLLECTOR_PORT", "14268"))
                .jaegerAgentHost(getStringEnv("JAEGER_AGENT_HOST", "localhost"))
                .jaegerAgentPort(getIntegerEnv("JAEGER_AGENT_PORT", "6831"))
                .jaegerFlushInterval(getIntegerEnv("JAEGER_FLUSH_INTERVAL", "100"))
                .jaegerMaxPocketSize(getIntegerEnv("JAEGER_MAX_POCKET_SIZE", "0"))
                .jaegerMaxQueueSize(getIntegerEnv("JAEGER_MAX_QUEUE_SIZE", "10000"))
                .collectorPods(getIntegerEnv("COLLECTOR_PODS", "1"))
                .collectorQueueSize(getIntegerEnv("COLLECTOR_QUEUE_SIZE", "2000"))
                .collectorWorkersCount(getIntegerEnv("COLLECTOR_NUM_WORKERS", "50"))
                .queryStaticFiles(getStringEnv("QUERY_STATIC_FILES", ""))
                .esMemory(getStringEnv("ES_MEMORY", "1Gi"))
                .esBulkSize(getIntegerEnv("ES_BULK_SIZE", "5000000"))
                .esBulkWorkers(getIntegerEnv("ES_BULK_WORKERS", "1"))
                .esBulkFlushInterval(getStringEnv("ES_BULK_FLUSH_INTERVAL", "200ms"))
                .imageAgent(getStringEnv("JAEGER_AGENT_IMAGE", "jaegertracing/jaeger-agent:latest"))
                .imageCollector(getStringEnv("JAEGER_COLLECTOR_IMAGE", "jaegertracing/jaeger-collector:latest"))
                .imageQuery(getStringEnv("JAEGER_QUERY_IMAGE", "jaegertracing/jaeger-query:latest"))
                .imageStorage(getStringEnv("STORAGE_IMAGE", "registry.centos.org/rhsyseng/elasticsearch:5.5.2"))
                .storageImageInSecure(getBooleanEnv("STORAGE_IMAGE_INSECURE", "false"))
                .imagePerformanceTest(
                        getStringEnv("PERFORMANCE_TEST_IMAGE", "jkandasa/jaeger-performance-test:latest"))
                .jaegerAgentQueueSize(getIntegerEnv("JAEGER_AGENT_QUEUE_SIZE", "1000"))
                .jaegerAgentWorkers(getIntegerEnv("JAEGER_AGENT_WORKERS", "10"))
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

    // general data
    private String testsToRun;
    private String performanceTestData;
    private Integer tracersCount;
    private Integer spansCount;

    private Boolean runningOnOpenshift;
    private String logsDirectory;

    // HTTP GET details
    private Integer queryLimit;
    private Integer querySamples;

    private Integer queryInterval; // in seconds, -1 means, run only at the end

    // sender details
    private String sender; // http, udp
    // database details
    private String storageType; // elasticsearch, cassandra
    private String spansCountFrom; // storage, jaeger-query
    private String storageHost;
    private Integer storagePort;

    private String storageKeyspace;
    // Jaeger Query details
    private String jaegerQueryHost;
    private Integer jaegerQueryPort;

    // Jaeger collector details
    private String jaegerCollectorHost;
    private Integer jaegerCollectorPort;

    // Jaeger agent details
    private String jaegerAgentHost;
    private Integer jaegerAgentPort;

    private Integer jaegerFlushInterval;
    private Integer jaegerMaxPocketSize;

    private Integer jaegerMaxQueueSize;
    // collector pod details
    private Integer collectorPods;
    private Integer collectorQueueSize;

    private Integer collectorWorkersCount;

    // query pod config
    private String queryStaticFiles;
    // Elasticsearch configurations
    private String esMemory;
    private Integer esBulkSize;
    private Integer esBulkWorkers;
    private String esBulkFlushInterval;

    // images
    private String imageAgent;
    private String imageCollector;
    private String imageQuery;

    private String imageStorage;
    private Boolean storageImageInSecure;

    private String imagePerformanceTest;

    private String jaegerClientVersion;

    private Integer jaegerAgentQueueSize;
    private Integer jaegerAgentWorkers;

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

    public boolean isPerformanceTestQuickRunEnabled() {
        if (performanceTestData.toLowerCase().startsWith("quick")) {
            return true;
        }
        return false;
    }

    public boolean isPerformanceTestLongRunEnabled() {
        return !isPerformanceTestQuickRunEnabled();
    }

    private Long getPerformanceTestSpanDelayOrDuration() {
        String[] data = performanceTestData.split(",");
        try {
            if (data.length == 2) {
                return Long.valueOf(data[1].trim());
            } else {
                throw new RuntimeException("Invalid performancte test data: " + performanceTestData);
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
            throw new RuntimeException("Exception:" + ex.getMessage());
        }
    }

    public Long getPerformanceTestSpanDelay() {
        if (isPerformanceTestLongRunEnabled()) {
            return -1L;
        }
        return getPerformanceTestSpanDelayOrDuration();
    }

    public Integer getPerformanceTestDuration() {
        if (isPerformanceTestQuickRunEnabled()) {
            return -1;
        }
        return getPerformanceTestSpanDelayOrDuration().intValue();
    }
}
