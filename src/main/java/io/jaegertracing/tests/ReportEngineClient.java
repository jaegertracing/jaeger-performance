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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.tests.report.model.JaegerTestReport;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class ReportEngineClient {

    private String hostUrl;
    private final OkHttpClient okClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void sethostUrl(String hostUrl) {
        if (hostUrl.endsWith("/")) {
            this.hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        } else {
            this.hostUrl = hostUrl;
        }
    }

    public ReportEngineClient(String hostUrl, OkHttpClient okClient) {
        sethostUrl(hostUrl);
        this.okClient = okClient;
    }

    public ReportEngineClient(String hostUrl) {
        sethostUrl(hostUrl);
        this.okClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.MINUTES)
                .addInterceptor(chain -> {
                    long start = System.currentTimeMillis();
                    Response response = chain.proceed(chain.request());
                    long duration = System.currentTimeMillis() - start;
                    logger.trace("{} --> in {}s", response.body(), TimeUnit.MILLISECONDS.toSeconds(duration));
                    return response;
                })
                .build();
    }

    public void addTestData(JaegerTestReport jaegerTestReport) {
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(jaegerTestReport));
            Request request = new Request.Builder()
                    .url(String.format("%s/suites", this.hostUrl))
                    .post(body)
                    .build();
            execute(request);
        } catch (JsonProcessingException ex) {
            logger.error("Exception,", ex);
        }
    }

    public void updateTestData(JaegerTestReport jaegerTestReport) {
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(jaegerTestReport));
            Request request = new Request.Builder()
                    .url(String.format("%s/suites", this.hostUrl))
                    .put(body)
                    .build();
            execute(request);
        } catch (JsonProcessingException ex) {
            logger.error("Exception,", ex);
        }
    }

    public Response execute(Request request) {
        try {
            Response response = okClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                logger.debug("{}, responseBody:{}", response, response.body().string());
            }
            return response;
        } catch (IOException ex) {
            logger.error("Exception,", ex);
        }
        return null;
    }

    public void close() {
        if (okClient != null) {
            okClient.dispatcher().executorService().shutdown();
            okClient.connectionPool().evictAll();
        }
    }

}
