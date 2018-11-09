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
package io.jaegertracing.tests;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class TestUtils {

    public static Boolean getBooleanEnv(String key, String defaultValue) {
        return Boolean.valueOf(getStringEnv(key, defaultValue));
    }

    public static Float getFloatEnv(String key, String defaultValue) {
        return Float.valueOf(getStringEnv(key, defaultValue));
    }

    public static Integer getIntegerEnv(String key, String defaultValue) {
        return Integer.valueOf(getStringEnv(key, defaultValue));
    }

    public static String getStringEnv(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    public static String timeTaken(long durationMillis) {
        return DurationFormatUtils.formatDurationHMS(durationMillis);
    }
}
