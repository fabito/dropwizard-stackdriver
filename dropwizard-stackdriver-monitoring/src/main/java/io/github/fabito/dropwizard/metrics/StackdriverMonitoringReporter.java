package io.github.fabito.dropwizard.metrics;

import com.codahale.metrics.*;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.util.DateTime;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.*;
import com.google.api.services.monitoring.v3.model.Metric;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fabio on 21/12/16.
 */
public class StackdriverMonitoringReporter extends ScheduledReporter {

    static final String CUMULATIVE = "CUMULATIVE";
    static final String GAUGE = "GAUGE";

    /**
     * Returns a new {@link Builder} for {@link StackdriverMonitoringReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link StackdriverMonitoringReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link StackdriverMonitoringReporter} instances.
     */
    public static class Builder {

        private final MetricRegistry registry;
        private Clock clock;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public StackdriverMonitoringReporter.Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public StackdriverMonitoringReporter.Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public StackdriverMonitoringReporter.Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public StackdriverMonitoringReporter.Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link ConsoleReporter} with the given properties.
         *
         * @return a {@link ConsoleReporter}
         */
        public StackdriverMonitoringReporter build(String projectId, Monitoring monitoring) {
            return new StackdriverMonitoringReporter(registry,
                    monitoring,
                    projectId,
                    clock,
                    rateUnit,
                    durationUnit,
                    filter);
        }
    }

    static String CUSTOM_METRIC_PREFIX = "custom.googleapis.com/dw";
    private static final Logger LOGGER = LoggerFactory.getLogger(StackdriverMonitoringReporter.class);
    private final Monitoring monitoring;
    private final Clock clock;
    private final String timeSeriesName;
    private final String startTime;

    private StackdriverMonitoringReporter(MetricRegistry registry, Monitoring monitoring, String projectId, Clock clock, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter) {
        super(registry, "stackdriver-reporter", filter, rateUnit, durationUnit);
        this.monitoring = monitoring;
        this.clock = clock;
        this.timeSeriesName = "projects/" + projectId;
        this.startTime = new DateTime(clock.getTime(), 0).toStringRfc3339();
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

        String now = new DateTime(clock.getTime(), 0).toStringRfc3339();

        try {

            List<TimeSeries> timeSeriesList = Lists.newArrayList();
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                final TimeSeries timeSeries = reportGauge(entry.getKey(), entry.getValue(), now);
                if (timeSeries != null) {
                    timeSeriesList.add(timeSeries);
                }
            }

            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                final TimeSeries timeSeries = reportCounter(entry.getKey(), entry.getValue(), now);
                if (timeSeries != null) {
                    timeSeriesList.add(timeSeries);
                }
            }

            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                final List<TimeSeries> histogramTimeSeries = reportHistogram(entry.getKey(), entry.getValue(), now);
                if (histogramTimeSeries != null) {
                    timeSeriesList.addAll(histogramTimeSeries);
                }
            }

            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                final List<TimeSeries> meterTimeSeries = reportMetered(entry.getKey(), entry.getValue(), now);
                if (meterTimeSeries != null) {
                    timeSeriesList.addAll(meterTimeSeries);
                }
            }

            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                final List<TimeSeries> timerTimeSeries = reportTimer(entry.getKey(), entry.getValue(), now);
                if (timerTimeSeries != null) {
                    timeSeriesList.addAll(timerTimeSeries);
                }
            }

            if (!timeSeriesList.isEmpty()) {
                if (timeSeriesList.size() > 200) {
                    final BatchRequest batch = monitoring.batch();
                    for (List<TimeSeries> partition : Iterables.partition(timeSeriesList, 200)) {
                        CreateTimeSeriesRequest timeSeriesRequest = new CreateTimeSeriesRequest();
                        timeSeriesRequest.setTimeSeries(partition);
                        Monitoring.Projects.TimeSeries.Create create = monitoring.projects().timeSeries().create(this.timeSeriesName, timeSeriesRequest);
                        create.queue(batch, new CreateTimeSeriesJsonBatchCallback(partition));
                    }
                    batch.execute();
                } else {
                    CreateTimeSeriesRequest timeSeriesRequest = new CreateTimeSeriesRequest();
                    timeSeriesRequest.setTimeSeries(timeSeriesList);
                    final Empty empty = monitoring.projects().timeSeries().create(this.timeSeriesName, timeSeriesRequest).execute();
                    if (!empty.isEmpty()) {
                        LOGGER.warn(empty.toPrettyString());
                    }
                }


            }

        } catch (IOException e) {
            LOGGER.warn("Unable to report to Stackdriver", e);
        }
    }

    private List<TimeSeries> reportTimer(String name, Timer timer, String endTime) {
        final Snapshot snapshot = timer.getSnapshot();
        final List<TimeSeries> timerTimeSeries = Lists.newArrayList(
                timeSeries(name, snapshot.getMax(), endTime, "max", GAUGE),
                timeSeries(name, snapshot.getMean(), endTime, "mean", GAUGE),
                timeSeries(name, snapshot.getMin(), endTime, "min", GAUGE),
                timeSeries(name, snapshot.getStdDev(), endTime, "stddev", GAUGE),
                timeSeries(name, snapshot.getMedian(), endTime, "p50", GAUGE),
                timeSeries(name, snapshot.get75thPercentile(), endTime, "p75", GAUGE),
                timeSeries(name, snapshot.get95thPercentile(), endTime, "p95", GAUGE),
                timeSeries(name, snapshot.get98thPercentile(), endTime, "p98", GAUGE),
                timeSeries(name, snapshot.get99thPercentile(), endTime, "p99", GAUGE),
                timeSeries(name, snapshot.get999thPercentile(), endTime, "p999", GAUGE)
        );
        timerTimeSeries.addAll(reportMetered(name, timer, endTime));
        return timerTimeSeries;
    }

    private List<TimeSeries> reportMetered(String name, Metered meter, String endTime) {
        return Lists.newArrayList(
                timeSeries(name, meter.getCount(), endTime, "count", CUMULATIVE),
                timeSeries(name, meter.getOneMinuteRate(), endTime, "m1_rate", GAUGE),
                timeSeries(name, meter.getFiveMinuteRate(), endTime, "m5_rate", GAUGE),
                timeSeries(name, meter.getFifteenMinuteRate(), endTime, "m15_rate", GAUGE),
                timeSeries(name, meter.getMeanRate(), endTime, "mean_rate", GAUGE)
        );
    }

    private List<TimeSeries> reportHistogram(String name, Histogram histogram, String endTime) {
        final Snapshot snapshot = histogram.getSnapshot();
        return Lists.newArrayList(
                timeSeries(name, histogram.getCount(), endTime, "count", CUMULATIVE),
                timeSeries(name, snapshot.getMax(), endTime, "max", GAUGE),
                timeSeries(name, snapshot.getMean(), endTime, "mean", GAUGE),
                timeSeries(name, snapshot.getMin(), endTime, "min", GAUGE),
                timeSeries(name, snapshot.getStdDev(), endTime, "stddev", GAUGE),
                timeSeries(name, snapshot.getMedian(), endTime, "p50", GAUGE),
                timeSeries(name, snapshot.get75thPercentile(), endTime, "p75", GAUGE),
                timeSeries(name, snapshot.get95thPercentile(), endTime, "p95", GAUGE),
                timeSeries(name, snapshot.get98thPercentile(), endTime, "p98", GAUGE),
                timeSeries(name, snapshot.get99thPercentile(), endTime, "p99", GAUGE),
                timeSeries(name, snapshot.get999thPercentile(), endTime, "p999", GAUGE)
        );
    }

    private TimeSeries reportCounter(String name, Counter counter, String endTime) {
        return timeSeries(name, counter.getCount(), endTime, "count", CUMULATIVE);
    }

    private TimeSeries reportGauge(String name, Gauge gauge, String endTime) {
        final TypedValue value = typedValue(gauge.getValue());
        if (value != null) {
            return timeSeries(name, gauge.getValue(), endTime, "", GAUGE);

        }
        return null;
    }

    private TypedValue typedValue(Object o) {
        if (o instanceof Float) {
            return new TypedValue().setDoubleValue(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return new TypedValue().setDoubleValue((Double) o);
        } else if (o instanceof Byte) {
            return new TypedValue().setInt64Value(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return new TypedValue().setInt64Value(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return new TypedValue().setInt64Value(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return new TypedValue().setInt64Value((Long) o);
        } else if (o instanceof BigInteger) {
            return new TypedValue().setDoubleValue(((BigInteger) o).doubleValue());
        } else if (o instanceof BigDecimal) {
            return new TypedValue().setDoubleValue(((BigDecimal) o).doubleValue());
        }
        return null;
    }

    private TimeSeries timeSeries(String name, Object pointValue, String endTime, String subType, String metricKind) {
        return new TimeSeries()
                .setMetricKind(metricKind)
                .setMetric(metric(prefix(name, subType)))
                .setPoints(Lists.newArrayList(point(metricKind, endTime, typedValue(pointValue))));
    }

    private Point point(String metricKind, String endTime, TypedValue value) {
        return new Point()
                .setInterval(timeInterval(endTime, metricKind))
                .setValue(value);
    }

    private TimeInterval timeInterval(String endTime, String metricKind) {
        final TimeInterval timeInterval = new TimeInterval().setEndTime(endTime);
        if (CUMULATIVE.equals(metricKind)) {
            timeInterval.setStartTime(this.startTime);
        }
        return timeInterval.setEndTime(endTime);
    }

    private Metric metric(String type) {
        return new Metric().setType(type);
    }

    private String prefix(String... components) {
        return name(CUSTOM_METRIC_PREFIX, components);
    }

    private String name(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        return builder.toString();
    }

    private void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(part);
        }
    }

    private static class CreateTimeSeriesJsonBatchCallback extends JsonBatchCallback<Empty> {

        private static final Pattern pattern = Pattern.compile("timeSeries\\[(\\d+)]");

        private final List<TimeSeries> batchItems;

        private CreateTimeSeriesJsonBatchCallback(List<TimeSeries> batchItems) {
            this.batchItems = batchItems;
        }

        @Override
        public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
            LOGGER.warn("Error sending batch (size={}) to Stackdriver", batchItems.size());
            if (e != null) {
                LOGGER.warn(e.toPrettyString());
                final Matcher matcher = pattern.matcher(e.getMessage());
                if (matcher.find(1)) {
                    try {
                        int index = Integer.valueOf(matcher.group(1));

                        if (index > 0) {
                            final TimeSeries timeSeriesBefore = batchItems.get(index-1);
                            LOGGER.debug("TimeSeries on index {}: {}", index-1, timeSeriesBefore.toPrettyString());
                        }

                        final TimeSeries timeSeries = batchItems.get(index);
                        LOGGER.debug("TimeSeries on index {}: {}", index, timeSeries.toPrettyString());

                        if (index < batchItems.size()) {
                            final TimeSeries timeSeriesAfter = batchItems.get(index+1);
                            LOGGER.debug("TimeSeries on index {}: {}", index+1, timeSeriesAfter.toPrettyString());
                        }

                    } catch(Throwable t) {
                        LOGGER.debug("Could not find problematic TimeSeries");
                    }
                } else {
                    LOGGER.debug("Could not find problematic TimeSeries");
                }
            }
        }

        @Override
        public void onSuccess(Empty empty, HttpHeaders responseHeaders) throws IOException {
            if (!empty.isEmpty()) {
                LOGGER.warn(empty.toPrettyString());
            } else {
                LOGGER.debug("Batch (size={}) sent to Stackdriver", batchItems.size());
            }
        }
    }

}
