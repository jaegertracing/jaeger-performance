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

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;

import io.jaegertracing.tests.report.ReportFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class UntilNoChangeCounter implements ISpanCounter {

    private Timer queryTimer;
    private Timer queryUntilNoChangeTimer;

    private static final int MAX_ITERATION = 3;
    private static final long ITERATION_DELAY = 5 * 1000L;

    public UntilNoChangeCounter() {
        this.queryTimer = ReportFactory.timer("until-no-change-single-span-counter");
        this.queryUntilNoChangeTimer = ReportFactory.timer("until-no-change-span-counter");
    }

    @Override
    public int countUntilNoChange(int expected) {
        long startUntilNoChange = System.currentTimeMillis();
        int spansCountOld = 0;
        int spansCountFinal = 0;

        try {
            int iteration = 1;
            while (iteration <= MAX_ITERATION) {
                long start = System.currentTimeMillis();
                spansCountFinal = count();
                long duration = System.currentTimeMillis() - start;
                queryTimer.update(duration, TimeUnit.MILLISECONDS);
                logger.debug("Count took: {}s, spans status[returned:{}, expected:{}]",
                        TimeUnit.MILLISECONDS.toSeconds(duration), spansCountFinal, expected);
                if (spansCountOld != spansCountFinal) {
                    iteration = 1;
                    spansCountOld = spansCountFinal;
                } else if (expected <= spansCountFinal) {
                    break;
                } else {
                    iteration++;
                }
                Thread.sleep(ITERATION_DELAY);
            }
        } catch (Exception ex) {
            logger.error("Exception,", ex);
        }
        queryUntilNoChangeTimer.update(System.currentTimeMillis() - startUntilNoChange, TimeUnit.MILLISECONDS);
        return spansCountFinal;
    }
}
