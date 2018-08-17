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

public class QueryStatus {
    private String name;
    private String queryParameters;
    private long timetaken;

    public QueryStatus(String name, String queryParameters, long timetaken) {
        this.name = name;
        this.queryParameters = queryParameters;
        this.timetaken = timetaken;
    }

    public String getName() {
        return name;
    }

    public String getQueryParameters() {
        return queryParameters;
    }

    public long getTimetaken() {
        return timetaken;
    }
}
