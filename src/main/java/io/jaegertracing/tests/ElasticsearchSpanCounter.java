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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.tests.model.TestConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticsearchSpanCounter extends UntilNoChangeCounter {

    static RestClient getESRestClient(String host, int port) {
        return RestClient.builder(
                new HttpHost(host, port, "http"))
                .build();
    }

    static String getSpanIndex() {
        String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String spanIndex = "jaeger-span-" + formattedDate;
        logger.info("Using ElasticSearch index : [" + spanIndex + "]");
        return spanIndex;
    }

    private final String spanIndex;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public ElasticsearchSpanCounter(TestConfig config) {
        super();
        this.restClient = getESRestClient(config.getStorageHost(), config.getStoragePort());
        this.objectMapper = new ObjectMapper();
        this.spanIndex = getSpanIndex();
    }

    @Override
    public void close() throws IOException {
        restClient.close();
    }

    @Override
    public int count() {
        refreshSpanIndex();
        try {
            Response response = restClient.performRequest("GET", "/" + spanIndex + "/_count");
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode jsonPayload = objectMapper.readTree(responseBody);
            JsonNode count = jsonPayload.get("count");
            int spansCount = count.asInt();
            logger.info("found {} traces in ES", spansCount);
            return spansCount;
        } catch (IOException ex) {
            logger.error("Could not make request to count span index", ex);
            return -1;
        }
    }

    public void refreshSpanIndex() {
        try {
            Response response = restClient.performRequest("GET", "/" + spanIndex + "/_refresh");
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new RuntimeException("Could not refresh span index");
            }
        } catch (IOException ex) {
            logger.error("Could not make request to refresh span index", ex);
            //      throw new RuntimeException("Could not make request to refresh span index", ex);
        }
    }
}
