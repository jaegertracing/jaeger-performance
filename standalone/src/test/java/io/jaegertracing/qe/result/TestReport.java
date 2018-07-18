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

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class TestReport {
    private static TestReport _instance = new TestReport();

    private TestReport() {

    }

    public static TestReport getInstance() {
        return _instance;
    }

    DecimalFormat decimalFormat = new DecimalFormat("#.000");

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
        builder.append("\n\n======================= TEST SUMMARY REPORT =======================\n");

        builder.append("Span count status: \n");
        builder.append("------------------\n");
        builder.append("   Sent   : ").append(spanCountSent).append("\n");
        builder.append("   Found  : ").append(spanCountFound).append("\n");
        double dropPercentage = 100.0 - (((double) spanCountFound / spanCountSent) * 100.0);
        builder.append("   Drop % : ").append(decimalFormat.format(dropPercentage)).append("\n");
        builder.append("------------------\n\n");

        builder.append("Query execution status: \n");
        builder.append("-----------------------\n");
        for (QueryStatus status : statusList) {
            builder.append("   Name       : ").append(status.getName()).append("\n");
            builder.append("   Timetaken  : ").append(status.getTimetaken()).append("\n");
            builder.append("   Parameters : ").append(status.getQueryParameters()).append("\n\n");
        }

        builder.append("=============================== END ===============================\n\n");

        return builder.toString();
    }
}
