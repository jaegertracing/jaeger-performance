/**
 * Copyright 2018-2020 The Jaeger Authors
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
package io.jaegertracing.tests.clients;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.jaegertracing.tests.resourcemonitor.ReMetric;
import okhttp3.MultipartBody;
import io.jaegertracing.tests.report.model.JaegerTestReport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@Slf4j
public class ReportEngineClient extends GenericRestClient {

    private boolean available = false;

    public ReportEngineClient(String hostUrl) {
        super(hostUrl);
        // update available
        try {
            status();
        } catch (Exception e) {
            logger.warn("can't connect to report server", e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void addTestData(JaegerTestReport jaegerTestReport) {
        this.post("/suites", jaegerTestReport);
    }

    public void updateTestData(JaegerTestReport jaegerTestReport) {
        this.put("/suites", jaegerTestReport);
    }

    public void postMetric(ReMetric metric) {
        post("/metrics/single", metric);
    }

    public void postMetrics(List<ReMetric> metrics) {
        post("/metrics/multiple", metrics);
    }

    public void uploadFile(String suiteId, File file) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(FORM_DATA)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();

        Request request = new Request.Builder()
                .url(String.format("%s/suites/files/uploadSingle/%s", getHostUrl(), suiteId))
                .post(requestBody)
                .build();
        execute(request);
        logger.debug("File uploaded: {}", file.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> status() {
        Map<String, Object> status = (Map<String, Object>) get("/system/status", Map.class);
        available = status != null ? true : false;
        return status;
    }

}
