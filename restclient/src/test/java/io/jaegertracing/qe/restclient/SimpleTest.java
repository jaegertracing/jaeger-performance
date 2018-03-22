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
package io.jaegertracing.qe.restclient;

import io.jaegertracing.qe.restclient.model.Datum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class SimpleTest {
    @Ignore
    @Test
    public void simpleTest() {
        SimpleRestClient simpleRestClient = new SimpleRestClient();
        Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        queryParameters.put("limit", Arrays.asList("1"));

        List<Datum> traces = simpleRestClient.getTraces(queryParameters, 1);
        System.out.println("Got " + traces.size() + " Traces ");
    }
}
