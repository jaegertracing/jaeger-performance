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

package io.jaegertracing.qe.result;

import io.jaegertracing.qe.CreateTraces;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class TestReport {
    private static TestReport _instance = new TestReport();

    private TestReport() {

    }

    public static TestReport getInstance() {
        return _instance;
    }

    private static final Map<String, String> envs = System.getenv();

    DecimalFormat decimalFormat = new DecimalFormat("#0.000");

    private long spanCountSent = -1;
    private long spanCountFound = -1;

    private List<QueryStatus> statusList = new LinkedList<>();

    public void updateSpanCount(long sent, long found) {
        this.spanCountSent = sent;
        this.spanCountFound = found;
    }

    public void addQueryStatus(QueryStatus status) {
        statusList.add(status);
    }

    public String getStringReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n========================= TEST SUMMARY REPORT =========================\n");

        builder.append("Test configuration: \n");
        builder.append("-----------------------------------------------------------------------\n");
        builder.append("   Test duration           : ")
                .append(timetaken(CreateTraces.DURATION_IN_MINUTES * 1000 * 60)).append("\n");
        builder.append("   Thread count            : ").append(CreateTraces.THREAD_COUNT).append("\n");
        builder.append("   Delay b/w span creation : ").append(CreateTraces.DELAY).append(" ms\n");
        builder.append("   Workers pod count       : ").append(envs.get("WORKER_PODS")).append("\n");
        builder.append("   Tracers per pod         : ").append(CreateTraces.TRACERS_PER_POD).append("\n");
        builder.append("   Collector pod count     : ").append(envs.get("COLLECTOR_PODS")).append("\n");
        builder.append("   Collector queue size    : ").append(envs.get("COLLECTOR_QUEUE_SIZE")).append("\n");
        builder.append("   Collector workers nums  : ").append(envs.get("COLLECTOR_NUM_WORKERS")).append("\n");
        builder.append("   Query static files      : ").append(envs.get("QUERY_STATIC_FILES")).append("\n");
        builder.append("   Storage type            : ").append(envs.get("SPAN_STORAGE_TYPE")).append("\n");
        builder.append("   ES memory               : ").append(envs.get("ES_MEMORY")).append("\n");
        builder.append("   ES bulk size            : ").append(envs.get("ES_BULK_SIZE")).append("\n");
        builder.append("   ES bulk workers         : ").append(envs.get("ES_BULK_WORKERS")).append("\n");
        builder.append("   ES bulk flush interval  : ").append(envs.get("ES_BULK_FLUSH_INTERVAL")).append("\n");
        builder.append("   Jaeger sampling rate    : ").append(CreateTraces.JAEGER_SAMPLING_RATE).append("\n");
        builder.append("   Jaeger flush interval   : ").append(CreateTraces.JAEGER_FLUSH_INTERVAL).append(" ms\n");
        builder.append("   Jaeger max queue size   : ").append(CreateTraces.JAEGER_MAX_QUEUE_SIZE).append("\n");
        builder.append("   Collector host          : ").append(CreateTraces.JAEGER_COLLECTOR_HOST).append(":")
                .append(CreateTraces.JAEGER_COLLECTOR_PORT).append("\n");
        builder.append("   Agent host(UDP)         : ").append(CreateTraces.JAEGER_AGENT_HOST).append(":")
                .append(CreateTraces.JAEGER_UDP_PORT).append("\n");
        builder.append("   Agent image             : ").append(envs.get("JAEGER_AGENT_IMAGE")).append("\n");
        builder.append("   Collector image         : ").append(envs.get("JAEGER_COLLECTOR_IMAGE")).append("\n");
        builder.append("   Query image             : ").append(envs.get("JAEGER_QUERY_IMAGE")).append("\n");
        builder.append("   Elasticsearch image     : ").append(envs.get("ES_IMAGE")).append("\n");

        builder.append("-----------------------------------------------------------------------\n\n");

        final double dropPercentage = 100.0 - (((double) spanCountFound / spanCountSent) * 100.0);
        final int tracesPersecond = Double.valueOf(((CreateTraces.THREAD_COUNT * (1000.0 / CreateTraces.DELAY))
                * new Integer(envs.getOrDefault("WORKER_PODS", "1")))).intValue();

        builder.append("Traces count status: \n");
        builder.append("-----------------------------------------------------------------------\n");
        builder.append("   Traces sent to  : ").append(CreateTraces.USE_AGENT_OR_COLLECTOR).append("\n");
        builder.append("   Traces / second : ").append(tracesPersecond).append(" (aprox)\n");
        builder.append("   Traces / minute : ").append(tracesPersecond * 60).append(" (aprox)\n");
        builder.append("   Sent            : ").append(spanCountSent).append("\n");
        builder.append("   Found           : ").append(spanCountFound).append("\n");
        builder.append("   Dropped %       : ").append(decimalFormat.format(dropPercentage)).append("\n");
        builder.append("-----------------------------------------------------------------------\n\n");

        builder.append("Query execution status: \n");
        builder.append("-----------------------------------------------------------------------\n");
        for (QueryStatus status : statusList) {
            builder.append("   Name       : ").append(status.getName()).append("\n");
            builder.append("   Timetaken  : ").append(timetaken(status.getTimetaken())).append("\n");
            builder.append("   Parameters : ").append(status.getQueryParameters()).append("\n\n");
        }

        builder.append("================================= END =================================\n\n");

        return builder.toString();
    }

    private String timetaken(long durationMillis) {
        return DurationFormatUtils.formatDurationHMS(durationMillis);
    }
}
