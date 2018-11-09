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
package io.jaegertracing.tests;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;

@Slf4j
public class JaegerQueryRunnable implements Closeable, Runnable {

    class QueryTimer extends TimerTask {
        @Override
        public void run() {
            execute(executedCount.get() + "_");
            executedCount.incrementAndGet();
        }
    }

    private static final int DEFAULT_LIMIT = 20;
    private static final AtomicInteger executedCount = new AtomicInteger(1);
    private OkHttpClient okClient;
    private ObjectMapper objectMapper;
    private TestConfig config;
    private String service;
    private String operation;
    private List<Map<String, String>> tagsList;

    private final int numberOfExecution;

    public JaegerQueryRunnable(
            TestConfig config,
            String service,
            String operation,
            List<Map<String, String>> tagsList) {
        logger.debug("Service:{}, Operation:{}, {}", service, operation);
        this.config = config;
        this.service = service;
        this.operation = operation;
        this.tagsList = tagsList;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .build();

        if (config.getQueryInterval() != -1) {
            int finalExecution = config.getPerformanceTestDuration() - config.getQueryInterval() - 5;
            numberOfExecution = finalExecution / config.getQueryInterval();
        } else {
            numberOfExecution = 0;
        }
    }

    @Override
    public void close() {
        okClient.dispatcher().executorService().shutdown();
        okClient.connectionPool().evictAll();
    }

    private Map<String, Timer> createTimers(String prefix) {
        String queryUrl = "http://" + config.getJaegerQueryHost() + ":" + config.getJaegerQueryPort();

        String urlServiceLimit = "%s/api/traces?service=%s&limit=%d&lookback=1h";
        String urlServiceLimitTags = "%s/api/traces?service=%s&limit=%d&lookback=1h&tags=%s";
        String urlServiceLimitOperation = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s";
        String urlServiceLimitOperationTags = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s&tags=%s";

        List<String> urls = new ArrayList<>();
        urls.add(String.format(urlServiceLimit, queryUrl, service, DEFAULT_LIMIT));
        urls.add(String.format(urlServiceLimit, queryUrl, service, config.getQueryLimit()));
        urls.add(String.format(urlServiceLimitOperation, queryUrl, service, DEFAULT_LIMIT, operation));
        urls.add(String.format(urlServiceLimitOperation, queryUrl, service, config.getQueryLimit(), operation));

        for (Map<String, String> map : tagsList) {
            String tagsQueryString = getTagsQueryString(map);
            urls.add(String.format(urlServiceLimitTags, queryUrl, service, DEFAULT_LIMIT, tagsQueryString));
            urls.add(String.format(urlServiceLimitTags, queryUrl, service, config.getQueryLimit(), tagsQueryString));
            urls.add(String.format(urlServiceLimitOperationTags, queryUrl, service, DEFAULT_LIMIT, operation,
                    tagsQueryString));
            urls.add(String.format(urlServiceLimitOperationTags, queryUrl, service, config.getQueryLimit(), operation,
                    tagsQueryString));
        }

        HashMap<String, Timer> timersMap = new HashMap<>();
        for (String name : urls) {
            timersMap.put(name, ReportFactory.timer(prefix + name));
        }
        return timersMap;
    }

    public void execute(String prefix) {
        long start = 0;
        long startOverAll = System.currentTimeMillis();
        for (int sample = 0; sample < config.getQuerySamples(); sample++) {
            start = System.currentTimeMillis();
            executeQueries(prefix);
            logger.debug("QueryRun prefix:{}, sample:{}, timeTaken:{}",
                    prefix, sample, TestUtils.timeTaken(System.currentTimeMillis() - start));
        }
        logger.debug("Overall time taken for the prefix:{}, samples:{}, timeTaken:{}",
                prefix, config.getQuerySamples(), TestUtils.timeTaken(System.currentTimeMillis() - startOverAll));
    }

    private void executeQueries(String prefix) {
        Map<String, Timer> timersMap = createTimers(prefix);
        for (Map.Entry<String, Timer> urlTimer : timersMap.entrySet()) {
            try {
                long start = System.currentTimeMillis();
                Response response = okClient.newCall(new Builder()
                        .url(urlTimer.getKey())
                        .build())
                        .execute();

                if (!response.isSuccessful()) {
                    logger.warn("Not successful request, response code:{}, message:{}.",
                            response.code(), response.message());
                }

                response.body().string();
                long duration = System.currentTimeMillis() - start;
                urlTimer.getValue().update(duration, TimeUnit.MILLISECONDS);
                logger.trace("[{}] {}: {}", TestUtils.timeTaken(duration), prefix, urlTimer.getKey());
                response.close();
            } catch (IOException ex) {
                logger.error("Exception,", ex);
            }
        }
    }

    private String getTagsQueryString(Map<String, String> tags) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (stringBuilder.length() != 1) {
                stringBuilder.append(",");
            }
            stringBuilder.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @Override
    public void run() {
        if (config.isPerformanceTestQuickRunEnabled()) {
            logger.info("Running query in multiple intervals only applicable for long run test");
            return;
        }
        logger.debug("Query run in multiple intervals triggered. Startes after {} seconds"
                + " and total iterations:{}, interval:{} sec",
                config.getQueryInterval(), numberOfExecution, config.getQueryInterval());
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new QueryTimer(), config.getQueryInterval() * 1000L, config.getQueryInterval() * 1000L);

        try {
            while (executedCount.get() < numberOfExecution) {
                TimeUnit.MILLISECONDS.sleep(500L);
            }
            timer.cancel();
            logger.debug("Executed count:{}, expected count:{}", executedCount.get(), numberOfExecution);
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
    }
}
