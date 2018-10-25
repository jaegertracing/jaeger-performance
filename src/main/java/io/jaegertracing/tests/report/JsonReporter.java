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
package io.jaegertracing.tests.report;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import io.jaegertracing.tests.report.model.BaseModel;
import io.jaegertracing.tests.report.model.HistogramModel;
import io.jaegertracing.tests.report.model.MeterModel;
import io.jaegertracing.tests.report.model.MetricReport;
import io.jaegertracing.tests.report.model.TimerModel;

public class JsonReporter {

    public static class Builder {
        private final MetricRegistry registry;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private Set<MetricAttribute> disabledMetricAttributes;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            disabledMetricAttributes = Collections.emptySet();
        }

        public JsonReporter build() {
            return new JsonReporter(
                    registry,
                    rateUnit,
                    durationUnit,
                    filter,
                    disabledMetricAttributes);
        }

        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder disabledMetricAttributes(Set<MetricAttribute> disabledMetricAttributes) {
            this.disabledMetricAttributes = disabledMetricAttributes;
            return this;
        }

        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }
    }

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    private final MetricRegistry registry;
    private final MetricFilter filter;
    private final long durationFactor;
    private final String durationUnit;
    private final long rateFactor;
    private final String rateUnit;
    private final Set<MetricAttribute> disabledMetricAttributes;

    private JsonReporter(
            MetricRegistry registry,
            TimeUnit rateUnit,
            TimeUnit durationUnit,
            MetricFilter filter,
            Set<MetricAttribute> disabledMetricAttributes) {
        this.registry = registry;
        this.filter = filter;
        this.rateFactor = rateUnit.toSeconds(1);
        this.rateUnit = calculateRateUnit(rateUnit);
        this.durationFactor = durationUnit.toNanos(1);
        this.durationUnit = durationUnit.toString().toLowerCase(Locale.US);
        this.disabledMetricAttributes = disabledMetricAttributes;

    }

    private void addIfEnabled(MetricAttribute attribute, String key, Object value, Map<String, Object> data) {
        if (!getDisabledMetricAttributes().contains(attribute)) {
            data.put(key, value);
        }
    }

    private String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.US);
        return s.substring(0, s.length() - 1);
    }

    private double convertDuration(double duration) {
        return duration / durationFactor;
    }

    private double convertRate(double rate) {
        return rate * rateFactor;
    }

    private Set<MetricAttribute> getDisabledMetricAttributes() {
        return disabledMetricAttributes;
    }

    private String getDurationUnit() {
        return durationUnit;
    }

    private String getRateUnit() {
        return rateUnit;
    }

    public MetricReport getReport() {
        synchronized (this) {
            MetricReport metricReport = new MetricReport();
            report(metricReport,
                    registry.getGauges(filter),
                    registry.getCounters(filter),
                    registry.getHistograms(filter),
                    registry.getMeters(filter),
                    registry.getTimers(filter));
            return metricReport;
        }
    }

    @SuppressWarnings("rawtypes")
    public void report(
            MetricReport metricReport,
            SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            metricReport.getGauges().add(BaseModel.builder()
                    .name(entry.getKey())
                    .key("value")
                    .value(entry.getValue().getValue())
                    .build());
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            metricReport.getCounters().add(BaseModel.builder()
                    .name(entry.getKey())
                    .key("count")
                    .value(entry.getValue().getCount())
                    .build());
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            Histogram histogram = entry.getValue();
            Map<String, Object> data = new HashMap<>();
            updateHistogram(data, histogram.getSnapshot());
            metricReport.getHistograms().add(HistogramModel.builder()
                    .name(entry.getKey())
                    .count(histogram.getCount())
                    .histogram(data)
                    .build());
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            Meter meter = entry.getValue();
            Map<String, Object> data = new HashMap<>();
            updateMeter(data, meter.getMeanRate(), meter.getOneMinuteRate(), meter.getFiveMinuteRate(),
                    meter.getFifteenMinuteRate());
            metricReport.getMeters().add(MeterModel.builder()
                    .name(entry.getKey())
                    .count(meter.getCount())
                    .meter(data)
                    .build());
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            Map<String, Object> meterData = new HashMap<>();
            updateMeter(meterData, timer.getMeanRate(), timer.getOneMinuteRate(), timer.getFiveMinuteRate(),
                    timer.getFifteenMinuteRate());
            Map<String, Object> histogramData = new HashMap<>();
            updateHistogram(histogramData, timer.getSnapshot());
            metricReport.getTimers().add(TimerModel.builder()
                    .name(entry.getKey())
                    .count(timer.getCount())
                    .rate(meterData)
                    .duration(histogramData)
                    .build());
        }
    }

    private void updateHistogram(Map<String, Object> data, Snapshot snapshot) {
        addIfEnabled(MetricAttribute.MAX, "max", convertDuration(snapshot.getMax()), data);
        addIfEnabled(MetricAttribute.MEAN, "mean", convertDuration(snapshot.getMean()), data);
        addIfEnabled(MetricAttribute.MIN, "min", convertDuration(snapshot.getMin()), data);
        addIfEnabled(MetricAttribute.STDDEV, "stdDev", convertDuration(snapshot.getStdDev()), data);
        addIfEnabled(MetricAttribute.P50, "median", convertDuration(snapshot.getMedian()), data);
        addIfEnabled(MetricAttribute.P75, "75thPercentile", convertDuration(snapshot.get75thPercentile()), data);
        addIfEnabled(MetricAttribute.P95, "95thPercentile", convertDuration(snapshot.get95thPercentile()), data);
        addIfEnabled(MetricAttribute.P98, "98thPercentile", convertDuration(snapshot.get98thPercentile()), data);
        addIfEnabled(MetricAttribute.P99, "99thPercentile", convertDuration(snapshot.get99thPercentile()), data);
        addIfEnabled(MetricAttribute.P999, "999thPercentile", convertDuration(snapshot.get999thPercentile()), data);
        data.put("unit", getDurationUnit());
    }

    private void updateMeter(Map<String, Object> data, double meanRate, double oneMinuteRate, double fiveMinuteRate,
            double fifteenMinuteRate) {
        addIfEnabled(MetricAttribute.MEAN_RATE, "meanRate", convertRate(meanRate), data);
        addIfEnabled(MetricAttribute.M1_RATE, "oneMinuteRate", convertRate(oneMinuteRate), data);
        addIfEnabled(MetricAttribute.M5_RATE, "fiveMinuteRate", convertRate(fiveMinuteRate), data);
        addIfEnabled(MetricAttribute.M15_RATE, "fifteenMinuteRate", convertRate(fifteenMinuteRate), data);
        if (!data.isEmpty()) {
            data.put("unit", getRateUnit());
        }
    }

}
