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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.tag.Tags;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;

@Slf4j
public class JaegerQueryRunnable implements Closeable, Runnable {

    class QueryTimer implements Runnable {
        @Override
        public void run() {
            long _start = System.currentTimeMillis();
            logger.debug("Triggered query job, iteration:{}", iteration);
            execute(iteration.get() + "_");
            // update spans count by api query
            ReportFactory.triggeredJaegerApiQuery();
            logger.debug("Completed query job in {} ms", System.currentTimeMillis() - _start);
        }
    }

    private static final int DEFAULT_LIMIT = 20;
    private static final AtomicInteger iteration = new AtomicInteger(0);
    private OkHttpClient okClient;
    private ObjectMapper objectMapper;
    private TestConfig config;
    private String service;
    private String operation;
    private List<Map<String, String>> tagsList;
    private String queryUrl = null;

    private static final String URL_SERVICE_LIMIT = "%s/api/traces?service=%s&limit=%d&lookback=1h";
    private static final String URL_SERVICE_LIMIT_TAGS = "%s/api/traces?service=%s&limit=%d&lookback=1h&tags=%s";
    private static final String URL_SERVICE_LIMIT_OPERATION = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s";
    private static final String URL_SERVICE_LIMIT_OPERATION_TAGS = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s&tags=%s";

    static Map<String, String> getNonseseTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("fooo.bar1", "fobarhax*+??");
        tags.put("fooo.ba2sar", "true");
        tags.put("fooo.ba4342r", "1");
        tags.put("fooo.ba24r*?%", "hehe");
        tags.put("fooo.bar*?%http.d6cconald", "hehuhoh$?ij");
        tags.put("fooo.bar*?%http.do**2nald", "goobarRAXbaz");
        tags.put("fooo.bar*?%http.don(a44ld", "goobarRAXbaz");
        return tags;
    }

    static Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        tags.put(Tags.HTTP_METHOD.getKey(), "get");
        tags.put(Tags.HTTP_METHOD.getKey(), "get");
        return tags;
    }

    public JaegerQueryRunnable(
            TestConfig config,
            String service,
            String operation) {
        logger.debug("Service:{}, Operation:{}, {}", service, operation);
        this.config = config;
        this.service = service;
        this.operation = operation;
        this.tagsList = Arrays.asList(getNonseseTags(), getTags());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        queryUrl = "http://" + config.getJaegerQueryHost() + ":" + config.getJaegerQueryPort();

        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public void close() {
        okClient.dispatcher().executorService().shutdown();
        okClient.connectionPool().evictAll();
    }

    private Map<String, Timer> createTimers(String prefix) {
        List<String> urls = new ArrayList<>();
        urls.add(String.format(URL_SERVICE_LIMIT, queryUrl, service, DEFAULT_LIMIT));
        urls.add(String.format(URL_SERVICE_LIMIT, queryUrl, service, config.getQueryLimit()));
        urls.add(String.format(URL_SERVICE_LIMIT_OPERATION, queryUrl, service, DEFAULT_LIMIT, operation));
        urls.add(String.format(URL_SERVICE_LIMIT_OPERATION, queryUrl, service, config.getQueryLimit(), operation));

        for (Map<String, String> map : tagsList) {
            String tagsQueryString = getTagsQueryString(map);
            urls.add(String.format(URL_SERVICE_LIMIT_TAGS, queryUrl, service, DEFAULT_LIMIT, tagsQueryString));
            urls.add(String.format(URL_SERVICE_LIMIT_TAGS, queryUrl, service, config.getQueryLimit(), tagsQueryString));
            urls.add(String.format(URL_SERVICE_LIMIT_OPERATION_TAGS, queryUrl, service, DEFAULT_LIMIT, operation,
                    tagsQueryString));
            urls.add(String.format(URL_SERVICE_LIMIT_OPERATION_TAGS, queryUrl, service, config.getQueryLimit(),
                    operation,
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
        try {
            if (config.getQueryInterval() <= 0) {
                return;
            }
            logger.debug("Query run in multiple intervals triggered. Will be triggered in {} seconds",
                    config.getQueryInterval());

            long waitTime = config.getSpansReportDurationInMillisecond();
            // remove a interval from wait time
            waitTime -= config.getQueryInterval() * 1000L;
            long queryEnd = System.currentTimeMillis() + waitTime;
            // initial delay
            Thread.sleep(config.getQueryInterval() * 1000L);
            while (queryEnd > System.currentTimeMillis()) {
                iteration.incrementAndGet();
                // trigger query run
                new Thread(new QueryTimer()).start();
                Thread.sleep(config.getQueryInterval() * 1000L);
            }

            logger.debug("Query run completed, iterations: {}", iteration.get());
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }

    }
}
