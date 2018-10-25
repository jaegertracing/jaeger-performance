/**
 * Copyright ${license.git.copyrightYears} The Jaeger Authors
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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import io.jaegertracing.tests.model.TestConfig;
import io.jaegertracing.tests.report.ReportFactory;
import io.jaegertracing.tests.report.model.JaegerTestReport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParseReport {
    private static JaegerTestReport _REPORT;

    public static JaegerTestReport report() {
        if (_REPORT == null) {
            TestConfig config = TestConfig.get();
            // check is this local run on OpenShift run
            if (config.getRunningOnOpenshift()) {
                // parse report from log file
                String[] filter = new String[] { "log" };
                List<File> files = (List<File>) FileUtils.listFiles(new File(config.getLogsDirectory()), filter,
                        true);
                for (File _file : files) {
                    if (_file.getName().contains("jaeger-performance-test-job")) {
                        try {
                            logger.info("Report found, location: {}", _file.getCanonicalPath());
                            String content = FileUtils.readFileToString(_file, "UTF-8");
                            int beginIndex = content.indexOf("@@START@@");
                            int endIndex = content.indexOf("@@END@@");
                            if (beginIndex != -1 && endIndex != -1) {
                                beginIndex += 9;
                                logger.debug("File content:{}", content.substring(beginIndex, endIndex));
                                _REPORT = (JaegerTestReport) JsonUtils.loadFromString(
                                        content.substring(beginIndex, endIndex),
                                        JaegerTestReport.class);
                                // dump this json
                                JsonUtils.dumps(_REPORT, _file.getParent(), _file.getName().replace("log", "json"));
                                break;
                            }
                        } catch (IOException ex) {
                            logger.error("Exception,", ex);
                        }
                    }
                }
            } else {
                _REPORT = ReportFactory.getFinalReport(config);
            }
        }
        return _REPORT;
    }
}
