package io.jaegertracing.qe.tests;

import static org.junit.Assert.assertEquals;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.Span;
import io.opentracing.Tracer;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateTracesTest {
    private static final Map<String, String> envs = System.getenv();

    private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "cassandra");
    private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");
    private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "1"));
    private static final String ELASTICSEARCH_HOST = envs.getOrDefault("ELASTICSEARCH_HOST", "elasticsearch");
    private static final Integer ELASTICSEARCH_PORT = new Integer(envs.getOrDefault("ELASTICSEARCH_PORT", "9200"));
    private static final String SPAN_STORAGE_TYPE = envs.getOrDefault("SPAN_STORAGE_TYPE", "cassandra");

    private static final Logger logger = LoggerFactory.getLogger(ValidateTracesTest.class.getName());
    private NumberFormat numberFormat = NumberFormat.getInstance();

    @Test
    public void countTraces() throws Exception {
        int expectedTraceCount = Integer.valueOf(System.getProperty("expectedTraceCount"));
        logger.info("EXPECTED_TRACE_COUNT " + numberFormat.format(expectedTraceCount));

        Instant startTime = Instant.now();
        int actualTraceCount = 0;
        if (SPAN_STORAGE_TYPE.equalsIgnoreCase("cassandra")) {
            logger.info("Validating Cassandra Traces");
            actualTraceCount = validateCassandraTraces(expectedTraceCount);
        } else  {
            logger.info("Validating ES Traces");
            actualTraceCount = validateElasticSearchTraces(expectedTraceCount);
        }

        Instant countEndTime = Instant.now();
        long countDuration = Duration.between(startTime, countEndTime).toMillis();
        logger.info("Counting " + numberFormat.format(actualTraceCount) + " traces took " + countDuration / 1000 + "." + countDuration % 1000 + " seconds.");
        assertEquals("Did not find expected number of traces", expectedTraceCount, actualTraceCount);
    }


    /**
     * It can take a while for traces to actually get written to storage, so both this and the Cassandra validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount number of traces we expect to find
     * @return actual number of traces found in ElasticSearch
     */
    private int validateElasticSearchTraces(int expectedTraceCount) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = now.format(formatter);
        String targetUrlString = "/jaeger-span-" + formattedDate + "/_count";
        logger.info("Using ElasticSearch URL : [" + targetUrlString + "]");

        RestClient restClient = getESRestClient();

        int previousTraceCount = -1;
        int actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
        final int startTraceCount = actualTraceCount;
        int iterations = 0;
        final long sleepDelay = 10L;

        logger.info("Setting SLEEP DELAY " + sleepDelay + " seconds");
        logger.info("Actual Trace count " + numberFormat.format(actualTraceCount));
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            try {
                TimeUnit.SECONDS.sleep(sleepDelay);
            } catch (InterruptedException e) {
                logger.warn("Got interrupted exception", e);
            }
            previousTraceCount = actualTraceCount;
            actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
            logger.info("FOUND " + numberFormat.format(actualTraceCount) + " traces in ElasticSearch");
            iterations++;
        }

        logger.info("It took " + iterations  + " iterations to go from " + numberFormat.format(startTraceCount)
                + " to " + numberFormat.format(actualTraceCount) + " traces");
        logger.info("FOUND " + numberFormat.format(actualTraceCount) + " traces in ElasticSearch");
        return actualTraceCount;
    }

    private RestClient getESRestClient() {
        logger.debug("Connecting to elasticsearch using host " + ELASTICSEARCH_HOST + " and port " + ELASTICSEARCH_PORT);
        return RestClient.builder(
                    new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT, "http"),
                    new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT + 1, "http"))
                    .build();
    }

    private int getElasticSearchTraceCount(RestClient restClient, String targetUrlString) throws IOException {
        Response response = restClient.performRequest("GET", targetUrlString);
        String responseBody = EntityUtils.toString(response.getEntity());
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        JsonNode jsonPayload = jsonObjectMapper.readTree(responseBody);
        JsonNode count = jsonPayload.get("count");
        int traceCount = count.asInt();

        return traceCount;
    }

    /**
     * It can take a while for traces to actually get written to storage, so both this and the ElasticSearch validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount number of traces we expect to find
     * @return final trace count found in Cassandra
     */
    private int validateCassandraTraces(int expectedTraceCount) {
        Session cassandraSession = getCassandraSession();
        int previousTraceCount = -1;
        int actualTraceCount = countTracesInCassandra(cassandraSession);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Got interrupted exception", 3);
            }
            previousTraceCount = actualTraceCount;
            actualTraceCount = countTracesInCassandra(cassandraSession);
            logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
            iterations++;
        }

        logger.info("It took " + iterations  + " iterations to go from " + startTraceCount + " to " + actualTraceCount + " traces");
        logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
        return actualTraceCount;
    }

    private Session getCassandraSession() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CASSANDRA_CLUSTER_IP);
        Cluster cluster = builder.build();
        Session session = cluster.connect(CASSANDRA_KEYSPACE_NAME);

        return session;
    }

    /**
     * For performance reasons Cassandra won't let us do a "select count(*) from traces" so instead we just have to do
     * "select * from traces" and count the number of rows it returns.
     *
     * @param session An open Cassandra session to use for queries
     * @return current number of traces found in Cassandra
     */
    private int countTracesInCassandra(Session session) {
        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();

        return totalTraceCount;
    }

    class WriteTraces implements Callable<Integer> {
        Tracer tracer;
        int id;
        int durationInMinutes;

        public WriteTraces(Tracer tracer, int durationInMinutes, int id) {
            this.tracer = tracer;
            this.durationInMinutes = durationInMinutes;
            this.id = id;
        }

        @Override
        public Integer call() throws Exception {
            int  spanCount = 0;
            String s = "Thread " + id;
            logger.debug("Starting " + s);

            Instant finish = Instant.now().plus(durationInMinutes, ChronoUnit.MINUTES);
            while (Instant.now().isBefore(finish)) {
                Span span = tracer.buildSpan(s).start();
                try {
                    span.setTag("iteration", spanCount);
                    spanCount++;
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    logger.warn("Got interrupted exception", 3);
                }
                span.finish();
            }

            return spanCount;
        }
    }


    class RowCountingConsumer implements Consumer<Row> {
        AtomicInteger rowCount = new AtomicInteger(0);

        @Override
        public void accept(Row r) {
            rowCount.getAndIncrement();
        }

        public int getRowCount() {
            return rowCount.get();
        }
    }

}
