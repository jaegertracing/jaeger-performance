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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import io.jaegertracing.tests.model.TestConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CassandraSpanCounter extends UntilNoChangeCounter {

    private final Session session;

    public CassandraSpanCounter(TestConfig config) {
        super();
        this.session = getCassandraSession(
                config.getStorageHost(), config.getStoragePort(), config.getStorageKeyspace());
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    public int count() {
        ResultSet result = session.execute("select * from traces");
        int spansCount = result.all().size();
        logger.info("found {} traces in Cassandra", spansCount);
        return spansCount;
    }

    private Session getCassandraSession(String contactPoint, Integer port, String keyspace) {
        Cluster cluster = Cluster.builder()
                .addContactPoint(contactPoint)
                .withPort(port)
                .build();
        return cluster.connect(keyspace);
    }
}
