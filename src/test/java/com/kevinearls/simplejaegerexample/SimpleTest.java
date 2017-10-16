package com.kevinearls.simplejaegerexample;

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

import java.util.Map;

public class SimpleTest {
    private static final Map<String, String> envs = System.getenv();

    private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "100"));
    private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "1000"));
    private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
    private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("MY_JAEGER_COLLECTOR_PORT", "14268");
    private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static final Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "50"));
    private static final Double JAEGER_SAMPLING_RATE = new Double(envs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static final Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    private static final String USE_AGENT_OR_COLLECTOR = envs.getOrDefault("USE_AGENT_OR_COLLECTOR", "AGENT");
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "standalone");

    private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class.getName());
    private static Tracer tracer;

    @BeforeClass
    public static void setUp() {
        tracer = jaegerTracer();
    }

    @After
    public  void tearDown() {
        com.uber.jaeger.Tracer jaegerTracer = (com.uber.jaeger.Tracer) tracer;
        jaegerTracer.close();
    }


    @Test
    public void writeSomeTraces() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            ActiveSpan span = tracer.buildSpan("blah").startActive();
            Thread.sleep(DELAY);
            span.close();
        }
    }

    public static Tracer jaegerTracer() {
        Tracer tracer;
        Sender sender;

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
        Reporter loggingRepoter = new LoggingReporter(logger);
        CompositeReporter compositeReporter = new CompositeReporter(remoteReporter, loggingRepoter);
        Sampler sampler = new ProbabilisticSampler(JAEGER_SAMPLING_RATE);
        tracer = new com.uber.jaeger.Tracer.Builder(TEST_SERVICE_NAME, compositeReporter, sampler)
                .build();


        return tracer;
    }
}
