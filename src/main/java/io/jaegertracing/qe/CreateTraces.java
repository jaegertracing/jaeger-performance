package io.jaegertracing.qe;

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
import io.opentracing.Tracer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTraces {
    private static final Map<String, String> envs = System.getenv();

    private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "1"));
    private static final Integer DURATION_IN_MINUTES = new Integer(envs.getOrDefault("DURATION_IN_MINUTES", "5"));
    private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("MY_JAEGER_COLLECTOR_PORT", "14268");
    private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static final Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "100000"));
    private static final Double JAEGER_SAMPLING_RATE = new Double(envs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static final Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "6831"));
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "standalone");
    private static final Integer THREAD_COUNT = new Integer(envs.getOrDefault("THREAD_COUNT", "100"));
    private static final String USE_AGENT_OR_COLLECTOR = envs.getOrDefault("USE_AGENT_OR_COLLECTOR", "COLLECTOR");
    private static final String USE_LOGGING_REPORTER = envs.getOrDefault("USE_LOGGING_REPORTER", "false");

    private static final Logger logger = LoggerFactory.getLogger(CreateTraces.class);
    private static Tracer tracer;

    /**
     *
     * Create a Jaeger tracer and configure it based on values of selected Environment variables.
     *
     * @return The tracer
     */
    private static Tracer jaegerTracer() {
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


    /**
     * Create THREAD_COUNT writer threads and run them for DURATION_IN_MINUTES to create threads.  At the end
     * report the number of threads and write that count to the file traceCount.txt
     *
     * @throws InterruptedException thrown by executor.awaitTermination
     * @throws ExecutionException thrown by worker.get
     * @throws IOException thrown by Files.Write
     */
    private void go() throws InterruptedException, ExecutionException, IOException {
        logger.info("Starting with " + THREAD_COUNT + " threads for " + DURATION_IN_MINUTES + " minutes with a delay of " + DELAY);
        final Instant createStartTime = Instant.now();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> workers = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Callable<Integer> worker = new TraceWriter(tracer, DURATION_IN_MINUTES, i);
            Future<Integer> created = executor.submit(worker);
            workers.add(created);
        }
        executor.shutdown();
        executor.awaitTermination(DURATION_IN_MINUTES + 1, TimeUnit.MINUTES);

        NumberFormat numberFormat = NumberFormat.getInstance();
        int tracesCreated = 0;
        for (Future<Integer> worker : workers) {
            int traceCount = worker.get();
            logger.info("Got " + traceCount + " traces");
            tracesCreated += traceCount;
        }
        logger.info("Got a total of " + numberFormat.format(tracesCreated) + " traces");
        Files.write(Paths.get("traceCount.txt"), Long.toString(tracesCreated).getBytes(), StandardOpenOption.CREATE);

        final Instant createEndTime = Instant.now();
        long duration = Duration.between(createStartTime, createEndTime).toMillis();
        logger.info("Finished all " + THREAD_COUNT + " threads; Created " + numberFormat.format(tracesCreated) + " traces" + " in " + duration + " milliseconds");

        System.setProperty("EATME", "" + tracesCreated);

        closeTracer();
    }


    /**
     * Close the tracer when done to make sure all traces are flushed.
     */
    private void closeTracer() {
        try {
            Thread.sleep(JAEGER_FLUSH_INTERVAL);
        } catch (InterruptedException e) {
            logger.warn("Interrupted Exception", e);
        }
        com.uber.jaeger.Tracer jaegerTracer = (com.uber.jaeger.Tracer) tracer;
        jaegerTracer.close();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException,IOException {
        CreateTraces createTraces = new CreateTraces();
        tracer = jaegerTracer();
        createTraces.go();
    }
}
