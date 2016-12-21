package io.github.fabito.dropwizard.metrics;

import com.codahale.metrics.*;
import com.google.api.client.util.DateTime;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.*;
import com.google.api.services.monitoring.v3.model.Metric;
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

    private StackdriverMonitoringReporter(MetricRegistry registry, Monitoring monitoring, String projectId, Clock clock, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter) {
        super(registry, "stackdriver-reporter", filter, rateUnit, durationUnit);
        this.monitoring = monitoring;
        this.clock = clock;
        this.timeSeriesName = "projects/" + projectId;
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
                if (timeSeriesList != null) {
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
                CreateTimeSeriesRequest timeSeriesRequest = new CreateTimeSeriesRequest();
                timeSeriesRequest.setTimeSeries(timeSeriesList);
                final Empty empty = monitoring.projects().timeSeries().create(this.timeSeriesName, timeSeriesRequest).execute();
                if (!empty.isEmpty()) {
                    LOGGER.warn(empty.toPrettyString());
                }
            }

        } catch (IOException e) {
            LOGGER.warn("Unable to report to Stackdriver",  e);
        }
    }

    private List<TimeSeries> reportTimer(String name, Timer timer, String timestamp) {
        final Snapshot snapshot = timer.getSnapshot();
        final List<TimeSeries> timerTimeSeries = Lists.newArrayList(
                timeSeries(name, point(timestamp, typedValue(snapshot.getMax())), "max", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.getMean())), "mean", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.getMin())), "min", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.getStdDev())), "stddev", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.getMedian())), "p50", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.get75thPercentile())), "p75", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.get95thPercentile())), "p95", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.get98thPercentile())), "p98", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.get99thPercentile())), "p99", GAUGE),
                timeSeries(name, point(timestamp, typedValue(snapshot.get999thPercentile())), "p999", GAUGE)
                );
        timerTimeSeries.addAll(reportMetered(name, timer, timestamp));
        return timerTimeSeries;
    }

    private List<TimeSeries> reportMetered(String name, Metered meter, String timestamp) {
        return Lists.newArrayList(
            timeSeries(name, point(timestamp, typedValue(meter.getCount())), "count", CUMULATIVE),
            timeSeries(name, point(timestamp, typedValue(meter.getOneMinuteRate())), "m1_rate", GAUGE),
            timeSeries(name, point(timestamp, typedValue(meter.getFiveMinuteRate())), "m5_rate", GAUGE),
            timeSeries(name, point(timestamp, typedValue(meter.getFifteenMinuteRate())), "m15_rate", GAUGE),
            timeSeries(name, point(timestamp, typedValue(meter.getMeanRate())), "mean_rate", GAUGE)
        );
    }

    private List<TimeSeries> reportHistogram(String name, Histogram histogram, String timestamp) {
        final Snapshot snapshot = histogram.getSnapshot();
        return Lists.newArrayList(
            timeSeries(name, point(timestamp, typedValue(histogram.getCount())), "count", CUMULATIVE),
            timeSeries(name, point(timestamp, typedValue(snapshot.getMax())), "max", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.getMean())), "mean", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.getMin())), "min", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.getStdDev())), "stddev", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.getMedian())), "p50", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.get75thPercentile())), "p75", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.get95thPercentile())), "p95", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.get98thPercentile())), "p98", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.get99thPercentile())), "p99", GAUGE),
            timeSeries(name, point(timestamp, typedValue(snapshot.get999thPercentile())), "p999", GAUGE)
        );
    }

    private TimeSeries reportCounter(String name, Counter counter, String now) {
        final TypedValue value = new TypedValue().setInt64Value(counter.getCount());
        Point point = point(now, value);
        return timeSeries(name, point, "count", CUMULATIVE);
    }

    private TimeSeries reportGauge(String name, Gauge gauge, String now) {
        final TypedValue value = typedValue(gauge.getValue());
        if (value != null) {
            Point point = point(now, value);
            return new TimeSeries()
                    .setMetricKind(GAUGE)
                    .setMetric(metric(prefix(name)))
                    //.setResource(new MonitoredResource().setType("gke_container"))
                    .setPoints(Lists.newArrayList(point));
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

    private Point point(String timestamp, TypedValue value) {
        return new Point()
                .setInterval(timeInterval(timestamp))
                .setValue(value);
    }

    private TimeInterval timeInterval(String timestamp) {
        return new TimeInterval().setEndTime(timestamp);
    }

    private TimeSeries timeSeries(String name, Point count, String subType, String metricKind) {
        return new TimeSeries()
                .setMetricKind(metricKind)
                .setMetric(metric(prefix(name, subType)))
                .setPoints(Lists.newArrayList(count));
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
}
