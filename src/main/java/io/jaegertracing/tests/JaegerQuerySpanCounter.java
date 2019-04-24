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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.tests.model.Result;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class JaegerQuerySpanCounter extends UntilNoChangeCounter {

    private OkHttpClient okClient;
    private ObjectMapper objectMapper;
    private Set<Request> requests = new LinkedHashSet<>();
    private boolean async;

    public JaegerQuerySpanCounter(
            TestConfig config,
            Set<String> serviceNames,
            boolean async) {
        super();

        String queryUrl = "http://" + config.getJaegerQueryHost() + ":" + config.getJaegerQueryPort();
        long limit = -1;

        limit = config.getSpansCount() * config.getSpansReportDurationInSecond();

        Timer jaegerQueryTimer = ReportFactory.timer("jaeger-query-span-counter");

        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .addInterceptor(chain -> {
                    long start = System.currentTimeMillis();
                    Response response = chain.proceed(chain.request());
                    long duration = System.currentTimeMillis() - start;
                    jaegerQueryTimer.update(duration, TimeUnit.MILLISECONDS);
                    logger.trace("{} --> in {}s", response.body(), TimeUnit.MILLISECONDS.toSeconds(duration));
                    return response;
                })
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        for (String service : serviceNames) {
            Request request = new Request.Builder()
                    .url(String.format("%s/api/traces?service=%s&limit=%d", queryUrl, service, limit)).build();
            this.requests.add(request);
        }
        this.async = async;
    }

    @Override
    public void close() {
        okClient.dispatcher().executorService().shutdown();
        okClient.connectionPool().evictAll();
    }

    @Override
    public int count() {
        return async ? getAsync() : getSync();
    }

    private int getAsync() {
        AtomicInteger totalCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(requests.size());
        for (Request request : requests) {
            okClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Result result = parseResult(response);
                    totalCount.addAndGet(result.getData().size());
                    logger.debug("---> {} spans", result.getData().size());
                    response.close();
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return totalCount.get();

    }

    private int getSync() {
        int totalCount = 0;
        for (Request request : requests) {
            try {
                Response response = okClient.newCall(request).execute();
                String body = response.body().string();
                Result result = objectMapper.readValue(body, Result.class);
                logger.debug("{} >> spans found: {}", request, result.getData().size());
                totalCount += result.getData().size();
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }
        return totalCount;
    }

    private Result parseResult(Response response) throws IOException {
        String body = response.body().string();
        return objectMapper.readValue(body, Result.class);
    }
}
