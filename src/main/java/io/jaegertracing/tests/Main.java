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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.codahale.metrics.Timer;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import io.jaegertracing.tests.smoke.TestSuiteSmoke;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    static final String SERVICE_NAME = "performance-test";
    static final String TRACER_PREFIX = "tracer_";
    // trigger external spans reporter
    JaegerQEControllerClient qeClient = null;

    public static void main(String[] args) throws Exception {
        Main instance = new Main();
        instance.execute();
    }

    TestConfig config;

    private final int expectedSpansCountByReporter;

    public Main() {
        config = TestConfig.get();
        // custom
        //config.setTestDuration(60 * 3);
        //config.setTracersCount(6);
        //config.setSpansCount(100);
        //config.setQueryInterval(30);
        //config.setSpansCountFrom("jaeger-query");

        qeClient = new JaegerQEControllerClient(config.getJaegerqeControllerUrl());

        // wait for jaeger agent to get ready
        if (config.getRunningOnOpenshift()) {
            if (config.getSender().equalsIgnoreCase("udp")) {
                try {
                    logger.info("This test is running on OpenShift environment and jaeger-agent deployed as a sidecar."
                            + " There is pre-defined delay(30 seconds) to get ready 'jager-agent'.");
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ex) {
                    logger.error("Exception,", ex);
                }
            }
        }

        if (config.getUseInternalReporter()) {
            expectedSpansCountByReporter = config.getNumberOfTracers() * config.getNumberOfSpans()
                    * config.getSpansReportDurationInSecond();
        } else {
            expectedSpansCountByReporter = config.getNodeCountSpansReporter() * config.getNumberOfTracers()
                    * config.getNumberOfSpans() * (config.getSpansReportDurationInSecond());
        }
        ReportFactory.updateSpansCountByReporter(expectedSpansCountByReporter);

    }

    public void execute() throws Exception {
        ReportEngineClient reClient = new ReportEngineClient(config.getReportEngineUrl());

        if (config.isPerformanceTestEnabled()) {
            // update report status to report engine
            reClient.addTestData(ReportFactory.getFinalReport(config));

            triggerCreateSpans();

            // report drop status
            logger.info("[DROP_COUNT={}]", ReportFactory.getDroupCount());
            logger.info("[DROP_PERCENTAGE={}]", ReportFactory.getDroupPercentage());
            logger.info("[SPANS_SENT={}]", ReportFactory.getSpansSent());
            logger.info("[SPANE_FOUND={}]", ReportFactory.getSpansFound());
            if (ReportFactory.getDroupCount() > 0) {
                logger.info("[TEST_STATUS=PASSED]", ReportFactory.getSpansFound());
            } else {
                logger.error("[TEST_STATUS=FAILED]", ReportFactory.getSpansFound());
            }
        } else {
            logger.info("Performance test is disabled.");
        }
        executeSmokeTests();

        logger.info("Final Report as json:\n@@START@@\n{}\n@@END@@", ReportFactory.getFinalReportAsString(config));
        ReportFactory.saveFinalReport(config, "/tmp/performance_report.json");
    }

    private void executeSmokeTests() {
        // execute smoke tests, if enabled
        if (config.isSmokeTestEnabled()) {
            logger.info("Execute Smoke tests enabled. Triggering smoke tests");
            JUnitCore jUnitCore = new JUnitCore();
            Result testResult = jUnitCore.run(TestSuiteSmoke.class);
            ReportFactory.updateTestSuiteStatus(TestSuiteSmoke.SUITE_NAME, testResult);
            logger.info("Smoke test status:{}", ReportFactory.gettestSuiteStatus(TestSuiteSmoke.SUITE_NAME));
        } else {
            logger.info("Execute Smoke tests are disabled.");
        }
    }

    private JaegerTracer createJaegerTracer(String serviceName) {
        Sender sender;
        if (config.getSender().equalsIgnoreCase("udp")) {
            sender = new UdpSender(
                    config.getJaegerAgentHost(),
                    config.getJaegerAgentPort(),
                    config.getJaegerClientMaxPocketsize());
            logger.info("Using UDP sender, sending to: {}:{}",
                    config.getJaegerAgentHost(), config.getJaegerAgentPort());
        } else {
            // use the collector
            String httpEndpoint = "http://" + config.getJaegerCollectorHost() + ":" + config.getJaegerCollectorPort()
                    + "/api/traces";
            logger.info("Using HTTP sender, sending to endpoint: {}", httpEndpoint);
            sender = new HttpSender.Builder(httpEndpoint).build();
        }

        logger.info("Flush interval {}, queue size {}", config.getJaegerClientFlushInterval(),
                config.getJaegerClientMaxQueueSize());
        RemoteReporter reporter = new RemoteReporter.Builder()
                .withSender(sender)
                .withMaxQueueSize(config.getJaegerClientMaxQueueSize())
                .withFlushInterval(config.getJaegerClientFlushInterval())
                .build();

        return new JaegerTracer.Builder(serviceName)
                .withReporter(reporter)
                .withSampler(new ConstSampler(true))
                .build();
    }

    private ISpanCounter getSpanCounter(Set<String> serviceNames) {
        ISpanCounter spanCounter;
        if (config.getSpansCountFrom().equalsIgnoreCase("storage")) {
            spanCounter = new ElasticsearchSpanCounter(config);
        } else {
            spanCounter = new JaegerQuerySpanCounter(config, serviceNames, false);
        }
        return spanCounter;
    }

    private void triggerCreateSpans() throws Exception {
        logger.debug("{}", config);

        // external reporter
        if (!config.getUseInternalReporter()) {
            long waitTime = config.getSpansReportDurationInMillisecond();
            // trigger external spans reporter
            HashMap<String, Object> data = config.getMap();
            data.put("serviceName", SERVICE_NAME);
            data.put("jobId", UUID.randomUUID().toString());
            data.put("useHostname", false);
            data.put("reportEngineSuiteId", ReportFactory.getReSuiteId());
            data.put("reporterReference", ReportFactory.getReporterReference());

            data.put("startTime", System.currentTimeMillis() + (15 * 1000L)); // 15 seconds from now
            waitTime += 20000L;

            qeClient.startSpansReporter(data);
            logger.info("Waiting to complte spans report. Wait time:{} ms", waitTime);

            // run query
            if (config.getQueryInterval() > 0) {
                long queryEnd = System.currentTimeMillis() + waitTime;
                int iteration = 1;
                do {
                    data.put("jaegerQueryIteration", iteration);
                    data.put("jaegerQueryOperation", "1");
                    qeClient.runSpansQuery(data);
                    Thread.sleep(config.getQueryInterval() * 1000L);
                    iteration++;
                    // update spans count by api query
                    ReportFactory.triggeredJaegerApiQuery();

                } while (queryEnd > System.currentTimeMillis());
            } else {
                Thread.sleep(waitTime);
            }

            // trigger spans count
            spansCount();
            return;
        }

        // internal reporter

        Timer reportingTimer = ReportFactory.timer("report-spans");

        long startTime = System.currentTimeMillis();
        // + 1, for query interval execution
        ExecutorService executor = Executors.newFixedThreadPool(config.getNumberOfTracers() + 1);
        List<Future<?>> futures = new ArrayList<>(config.getNumberOfTracers());
        Set<String> serviceNames = new LinkedHashSet<>();
        for (int tracerNumber = 1; tracerNumber <= config.getNumberOfTracers(); tracerNumber++) {
            String name = TRACER_PREFIX + tracerNumber;
            JaegerTracer tracer = createJaegerTracer(SERVICE_NAME + "-" + name);
            serviceNames.add(tracer.getServiceName());
            Runnable worker = new CreateSpansRunnable(tracer, name, config, true);
            futures.add(executor.submit(worker));
        }
        // add a worker for query execution time, in long run performance mode
        if (config.getQueryInterval() != -1) {
            Runnable queryWorker = new JaegerQueryRunnable(config, new ArrayList<>(serviceNames).get(0),
                    TRACER_PREFIX + 1);
            futures.add(executor.submit(queryWorker));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdownNow();
        executor.awaitTermination(3, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        reportingTimer.update(duration, TimeUnit.MILLISECONDS);
        logger.info("Finished sending spans to jaeger-{}. Spans sent:{}, timetaken:{}",
                config.getSender().equalsIgnoreCase("http") ? "collector(http)" : "agent(udp)",
                config.getNumberOfTracers() * config.getNumberOfSpans(), TimeUnit.MILLISECONDS.toSeconds(duration));
        ISpanCounter spanCounter = getSpanCounter(serviceNames);
        startTime = System.currentTimeMillis();
        int spansCount = spanCounter.countUntilNoChange((int) ReportFactory.getSpansSent());
        duration = System.currentTimeMillis() - startTime;
        ReportFactory.updateSpansLatency(duration);
        ReportFactory.updateSpansCount(expectedSpansCountByReporter, spansCount);
        logger.info("Exceuted spans count. timetaken:{}, spans[expected:{}, actual:{}]",
                TimeUnit.MILLISECONDS.toSeconds(duration), (int) ReportFactory.getSpansSent(), spansCount);

        JaegerQueryRunnable jaegerQuery = new JaegerQueryRunnable(config,
                new ArrayList<>(serviceNames).get(0), TRACER_PREFIX + 1);
        jaegerQuery.execute("FINAL_");

        // elasticsearch storage stats
        ElasticsearchStatsGetter esStatsGetter = new ElasticsearchStatsGetter(
                config.getStorageHost(), config.getStoragePort());
        esStatsGetter.printStats();
        esStatsGetter.close();

        spanCounter.close();
        jaegerQuery.close();
    }

    private void spansCount() {
        long startTimestamp = System.currentTimeMillis();
        try {
            ISpanCounter spanCounter = getSpanCounter(null);
            long startTime = System.currentTimeMillis();
            int spansCount = spanCounter.countUntilNoChange((int) ReportFactory.getSpansSent());
            long duration = System.currentTimeMillis() - startTime;
            ReportFactory.updateSpansCount(expectedSpansCountByReporter, spansCount);
            logger.info("Exceuted spans count. timetaken:{}, spans[expected:{}, actual:{}]",
                    TimeUnit.MILLISECONDS.toSeconds(duration), (int) ReportFactory.getSpansSent(), spansCount);

            // elasticsearch storage 
            ElasticsearchStatsGetter esStatsGetter = new ElasticsearchStatsGetter(
                    config.getStorageHost(), config.getStoragePort());
            esStatsGetter.printStats();

            esStatsGetter.close();
            spanCounter.close();

        } catch (IOException ex) {
            logger.error("Exception,", ex);
        }
        ReportFactory.updateSpansLatency(System.currentTimeMillis() - startTimestamp);
    }
}
