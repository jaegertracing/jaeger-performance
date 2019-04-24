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
package io.jaegertracing.tests.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.ToString;

import lombok.Data;

@Data
@ToString
public class Jenkins {
    private String buildDate;
    private String buildNumber;
    private String buildId;
    private String buildUrl;
    private String nodeName;
    private String jobName;

    public Jenkins() {
        update();
    }

    public void update() {
        buildDate = new SimpleDateFormat("YYYYMMdd").format(new Date());
        buildNumber = System.getenv("BUILD_NUMBER");
        buildId = System.getenv("BUILD_ID");
        buildUrl = System.getenv("BUILD_URL");
        nodeName = System.getenv("NODE_NAME");
        jobName = System.getenv("JOB_NAME");
    }
}
