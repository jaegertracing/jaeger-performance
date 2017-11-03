package com.kevinearls.simplejaegerexample;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.HttpSender;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;
import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class SimpleTest {
    private static final Map<String, String> envs = System.getenv();

    private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "localhost");
    private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");

    private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "100"));

    private static final String ES_HOST = envs.getOrDefault("ES_HOST", "localhost");
    private static final String ES_PORT = envs.getOrDefault("ES_PORT", "9200");

    private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "1000"));
    private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("MY_JAEGER_COLLECTOR_PORT", "14268");
    private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static final Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "100000"));
    private static final Double JAEGER_SAMPLING_RATE = new Double(envs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static final String JAEGER_STORAGE = envs.getOrDefault("JAEGER_STORAGE", "cassandra");
    private static final Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "standalone");
    private static final Integer THREAD_COUNT = new Integer(envs.getOrDefault("THREAD_COUNT", "10"));
    private static final String USE_AGENT_OR_COLLECTOR = envs.getOrDefault("USE_AGENT_OR_COLLECTOR", "AGENT");
    private static final String USE_LOGGING_REPORTER = envs.getOrDefault("USE_LOGGING_REPORTER", "false");

    private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class.getName());
    private static Tracer tracer;

    @BeforeClass
    public static void setUp() {
        tracer = jaegerTracer();
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(JAEGER_FLUSH_INTERVAL);
        com.uber.jaeger.Tracer jaegerTracer = (com.uber.jaeger.Tracer) tracer;
        jaegerTracer.close();
    }

    public static Tracer jaegerTracer() {
        Tracer tracer;
        Sender sender;
        CompositeReporter compositeReporter;

        if (USE_AGENT_OR_COLLECTOR.equalsIgnoreCase("agent")) {
            sender = new UdpSender(JAEGER_AGENT_HOST, JAEGER_UDP_PORT, JAEGER_MAX_PACKET_SIZE);
            logger.info("Using JAEGER tracer using agent on host [" + JAEGER_AGENT_HOST + "] port [" + JAEGER_UDP_PORT +
                    "] Service Name [" + TEST_SERVICE_NAME + "] Sampling rate [" + JAEGER_SAMPLING_RATE
                    + "] Max queue size: [" + JAEGER_MAX_QUEUE_SIZE + "]");
        } else {
            // use the collector
            String httpEndpoint = "http://" + JAEGER_COLLECTOR_HOST + ":" + JAEGER_COLLECTOR_PORT + "/api/traces";
            sender = new HttpSender(httpEndpoint);
            logger.info("Using JAEGER tracer using collector on host [" + JAEGER_COLLECTOR_HOST + "] port [" + JAEGER_COLLECTOR_PORT +
                    "] Service Name [" + TEST_SERVICE_NAME + "] Sampling rate [" + JAEGER_SAMPLING_RATE
                    + "] Max queue size: [" + JAEGER_MAX_QUEUE_SIZE + "]");
        }

        Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
        Reporter remoteReporter = new RemoteReporter(sender, JAEGER_FLUSH_INTERVAL, JAEGER_MAX_QUEUE_SIZE, metrics);
        if (USE_LOGGING_REPORTER.equalsIgnoreCase("true")) {
            Reporter loggingRepoter = new LoggingReporter(logger);
            compositeReporter = new CompositeReporter(remoteReporter, loggingRepoter);
        } else {
            compositeReporter = new CompositeReporter(remoteReporter);
        }

        Sampler sampler = new ProbabilisticSampler(JAEGER_SAMPLING_RATE);
        tracer = new com.uber.jaeger.Tracer.Builder(TEST_SERVICE_NAME, compositeReporter, sampler)
                .build();

        return tracer;
    }

    @Test
    public void countTraces() throws Exception {
        Session cassandraSession = getCassandraSession();
        int traceCount = countTracesInCassandra(cassandraSession);
        logger.info("Traces contains " + traceCount + " entries");
    }

    /**
     * This is the primary test.  Create the specified number of traces, and then verify that they exist in
     * whichever storage back end we have selected.
     *
     * @throws Exception
     */
    @Test
    public void createTracesTest() throws Exception {
        logger.info("Starting with " + THREAD_COUNT + " threads for " + ITERATIONS + " iterations with a delay of " + DELAY);
        AtomicInteger threadId = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            Runnable worker = new WriteSomeTraces(tracer, ITERATIONS, threadId.incrementAndGet());
            executor.execute(worker);
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Finished all " + THREAD_COUNT + " threads; Created " + THREAD_COUNT * ITERATIONS + " spans" + " in " + duration/1000 + " seconds") ;

        // Validate trace count here
        int expectedTraceCount = THREAD_COUNT * ITERATIONS;
        int actualTraceCount;

        if (JAEGER_STORAGE.equalsIgnoreCase("cassandra")) {
            logger.info("Validating Cassandra Traces");
            actualTraceCount = validateCassandraTraces(expectedTraceCount);
        } else {
            logger.info("Validating ES Traces");
            actualTraceCount = validateElasticSearchTraces(expectedTraceCount);
        }

        assertEquals("Did not find expected number of traces", expectedTraceCount, actualTraceCount);
    }


    /**
     * It can take a while for traces to actually get written to storage, so both this and the Cassandra validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount
     * @return
     * @throws Exception
     */
    private int validateElasticSearchTraces(int expectedTraceCount) throws Exception {
        // curl command needs to look like http://localhost:9200/jaeger-span-2017-11-02/_count
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = now.format(formatter);
        String targetUrlString = "http://" + ES_HOST + ":" + ES_PORT + "/jaeger-span-" + formattedDate + "/_count";
        logger.info("Using ElasticSearch URL : [" + targetUrlString + "]" );

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(targetUrlString);
        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_JSON);

        int previousTraceCount = -1;
        int actualTraceCount = getElasticSearchTraceCount(builder);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        logger.info("Actual Trace count " + actualTraceCount);

        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            logger.info("FOUND " + actualTraceCount + " traces in ElasticSearch");
            Thread.sleep(5000);
            previousTraceCount = actualTraceCount;
            actualTraceCount = getElasticSearchTraceCount(builder);
            iterations++;
        }

        logger.info("It took " + iterations  + " iterations to go from " + startTraceCount + " to " + actualTraceCount + " traces");
        logger.info("FOUND " + actualTraceCount + " traces in ElasticSearch");
        return actualTraceCount;
    }


    /**
     * Perform a GET on the ES server (something like http://localhost:9200/jaeger-span-2017-11-02/_count) and
     * extract the count from the response
     * @param builder
     * @return
     * @throws Exception
     */
    private int getElasticSearchTraceCount(Invocation.Builder builder) throws Exception {
        String result = builder.get(String.class);
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        JsonNode jsonPayload = jsonObjectMapper.readTree(result);
        JsonNode data = jsonPayload.get("count");
        int traceCount = data.asInt();

        return traceCount;
    }


    /**
     * It can take a while for traces to actually get written to storage, so both this and the ElasticSearch validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount
     * @return
     * @throws InterruptedException
     */
    private int validateCassandraTraces(int expectedTraceCount) throws InterruptedException {
        Session cassandraSession = getCassandraSession();
        int previousTraceCount = -1;
        int actualTraceCount = countTracesInCassandra(cassandraSession);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
            Thread.sleep(1000);
            previousTraceCount = actualTraceCount;
            actualTraceCount = countTracesInCassandra(cassandraSession);
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
     * @param session
     * @return
     */
    private int countTracesInCassandra(Session session) {
        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();

        return totalTraceCount;
    }

    class WriteSomeTraces implements Runnable {
        Tracer tracer;
        int iterations;
        int id;

        public WriteSomeTraces(Tracer tracer, int iterations, int id) {
            this.tracer = tracer;
            this.iterations = iterations;
            this.id = id;
        }

        @Override
        public void run() {
            String s = "Thread " + id;
            logger.debug("Starting " + s);
            for (int i = 0; i < iterations; i++) {
                ActiveSpan span = tracer.buildSpan(s).startActive();
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                }
                span.close();
            }
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
