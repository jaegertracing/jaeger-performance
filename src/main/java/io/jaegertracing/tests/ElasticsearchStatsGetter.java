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

import static io.jaegertracing.tests.ElasticsearchSpanCounter.getSpanIndex;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import io.jaegertracing.tests.osutils.OSCommandExecuter;

import io.jaegertracing.tests.osutils.OSResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticsearchStatsGetter implements Closeable {

    private static final String INDEX_STATS = "/_stats?pretty";
    private static final String NODES = "/_cat/nodes?v&h=hm,hc,rm,rc,iiti,iito,sqti,sqto";
    private static final String THREAD_POOL = "/_cat/thread_pool/generic?v&h=id,name,active,rejected,completed";

    private final String esProvider;
    private final String esHost;
    private final int esPort;
    private final RestClient restClient;
    private final String spanIndex;
    private final OSCommandExecuter cmdExecuter = new OSCommandExecuter();

    public ElasticsearchStatsGetter(String esProvider, String esHost, int esPort) {
        this.esProvider = esProvider;
        this.esHost = esHost;
        this.esPort = esPort;
        this.restClient = ElasticsearchSpanCounter.getESRestClient(esProvider, esHost, esPort);
        this.spanIndex = getSpanIndex();

    }

    @Override
    public void close() throws IOException {
        restClient.close();
    }

    private String execute(String request) {
        String command = String.format(
                "curl --cacert /certs/ca --key /certs/key --cert /certs/cert https://%s:%d%s",
                esHost, esPort, request);
        OSResponse response = cmdExecuter.executeLinuxCommand(command);
        logger.debug("Command:{}, {}", command, response);
        return response.getResult();
    }

    public void printStats() {
        try {
            if (esProvider.equals("es-operator")) {
                String indexStats = "/" + spanIndex + INDEX_STATS;
                logger.debug("{}: {}", indexStats, execute(indexStats));
                logger.debug("{}: {}", NODES, execute(NODES));
                logger.debug("{}: {}", THREAD_POOL, execute(THREAD_POOL));
            } else {
                Response indexStats = restClient.performRequest("GET", "/" + spanIndex + INDEX_STATS);
                // https://www.elastic.co/guide/en/elasticsearch/reference/6.3/cat-nodes.html
                Response nodeCat = restClient.performRequest("GET", NODES);
                // https://www.elastic.co/guide/en/elasticsearch/reference/6.3/cat-thread-pool.html
                Response threadPool = restClient.performRequest("GET", THREAD_POOL);

                logger.debug("{} --> {}",
                        nodeCat.getRequestLine().getUri(), EntityUtils.toString(nodeCat.getEntity()));
                logger.debug("{} --> {}",
                        threadPool.getRequestLine().getUri(), EntityUtils.toString(threadPool.getEntity()));
                logger.debug("{} --> {}",
                        indexStats.getRequestLine().getUri(), EntityUtils.toString(indexStats.getEntity()));
            }

        } catch (IOException ex) {
            logger.error("Exception,", ex);
        }
    }
}
