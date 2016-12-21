package io.github.fabito.dropwizard.metrics;

import com.codahale.metrics.*;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.CreateTimeSeriesRequest;
import com.google.api.services.monitoring.v3.model.Empty;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.exceptions.base.MockitoException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static io.github.fabito.dropwizard.metrics.StackdriverMonitoringReporter.CUMULATIVE;
import static io.github.fabito.dropwizard.metrics.StackdriverMonitoringReporter.GAUGE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by fabio on 21/12/16.
 */
public class StackdriverMonitoringReporterTest {

    private static final String T0 = "1970-01-01T00:00:00.000Z";
    private final MetricRegistry registry = mock(MetricRegistry.class);
    private final Clock clock = mock(Clock.class);
    private final Monitoring monitoringMock = mock(Monitoring.class);

    private StackdriverMonitoringReporter reporter = StackdriverMonitoringReporter
            .forRegistry(registry)
            .withClock(clock)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .filter(MetricFilter.ALL)
            .build("my-gcp-project-id", monitoringMock);

    private ArgumentCaptor<CreateTimeSeriesRequest> argumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(clock.getTime()).thenReturn(0L);
        argumentCaptor = ArgumentCaptor.forClass(CreateTimeSeriesRequest.class);
        Monitoring.Projects projectsMock = mock(Monitoring.Projects.class);
        when(monitoringMock.projects()).thenReturn(projectsMock);
        Monitoring.Projects.TimeSeries timeSeriesMock = mock(Monitoring.Projects.TimeSeries.class);
        when(projectsMock.timeSeries()).thenReturn(timeSeriesMock);
        Monitoring.Projects.TimeSeries.Create create = mock(Monitoring.Projects.TimeSeries.Create.class);
        when(timeSeriesMock.create(anyString(), argumentCaptor.capture())).thenReturn(create);
        when(create.execute()).thenReturn(new Empty());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void doesNotReportStringGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge("value")),
                this.map(),
                this.map(),
                this.map(),
                this.map());

        exception.expect(MockitoException.class);
        exception.expectMessage("No argument value was captured!");
        argumentCaptor.getValue();
    }

    @Test
    public void reportsByteGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge((byte) 1)),
                this.map(),
                this.map(),
                this.map(),
                this.map());

        assertTimeSeries(1, "gauge", GAUGE, T0, (byte) 1 );

    }

    @Test
    public void reportsIntegerGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1)),
                this.map(),
                this.map(),
                this.map(),
                this.map());
        assertTimeSeries(1, "gauge", GAUGE, T0, 1 );
    }

    @Test
    public void reportsLongGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(25L)),
                this.map(),
                this.map(),
                this.map(),
                this.map());
        assertTimeSeries(1, "gauge", GAUGE, T0, 25L );
    }

    @Test
    public void reportsFloatGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1f)),
                this.map(),
                this.map(),
                this.map(),
                this.map());

        assertTimeSeries(1, "gauge", GAUGE, T0, 1.1f );
    }

    @Test
    public void reportsDoubleGaugeValues() throws Exception {
        reporter.report(map("gauge", gauge(1.1)),
                this.map(),
                this.map(),
                this.map(),
                this.map());

        assertTimeSeries(1, "gauge", GAUGE, T0, 1.1 );
    }

    @Test
    public void reportsCounters() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        reporter.report(this.map(),
                this.map("counter", counter),
                this.map(),
                this.map(),
                this.map());

        assertTimeSeries(1, "counter/count", "CUMULATIVE", T0, 100L );
    }

    @Test
    public void reportsHistograms() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(7.0);
        when(snapshot.get95thPercentile()).thenReturn(8.0);
        when(snapshot.get98thPercentile()).thenReturn(9.0);
        when(snapshot.get99thPercentile()).thenReturn(10.0);
        when(snapshot.get999thPercentile()).thenReturn(11.0);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.map(),
                this.map(),
                this.map("histogram", histogram),
                this.map(),
                this.map());

        int total = 11;
        assertTimeSeries(total, "histogram/count", CUMULATIVE, T0, 1 );
        assertTimeSeries(total, "histogram/max", GAUGE, T0, 2 );
        assertTimeSeries(total, "histogram/mean", GAUGE, T0, 3.00 );
        assertTimeSeries(total, "histogram/min", GAUGE, T0, 4 );
        assertTimeSeries(total, "histogram/stddev", GAUGE, T0, 5.00 );

        assertTimeSeries(total, "histogram/p50", GAUGE, T0, 6.00 );
        assertTimeSeries(total, "histogram/p75", GAUGE, T0, 7.00 );
        assertTimeSeries(total, "histogram/p95", GAUGE, T0, 8.00 );
        assertTimeSeries(total, "histogram/p98", GAUGE, T0, 9.00 );
        assertTimeSeries(total, "histogram/p99", GAUGE, T0, 10.00 );
        assertTimeSeries(total, "histogram/p999", GAUGE, T0, 11.00 );
    }

    @Test
    public void reportsMeters() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        reporter.report(this.map(),
                this.map(),
                this.map(),
                this.map("meter", meter),
                this.map());

        assertTimeSeries(5, "meter/count", CUMULATIVE, T0, 1 );
        assertTimeSeries(5, "meter/m1_rate", GAUGE, T0, 2.00 );
        assertTimeSeries(5, "meter/m5_rate", GAUGE, T0, 3.00 );
        assertTimeSeries(5, "meter/m15_rate", GAUGE, T0, 4.00 );
        assertTimeSeries(5, "meter/mean_rate", GAUGE, T0, 5.00 );
    }

    @Test
    public void reportsTimers() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        final long max = TimeUnit.MILLISECONDS.toNanos(100);
        when(snapshot.getMax()).thenReturn(max);
        final double mean = TimeUnit.MILLISECONDS.toNanos(200);
        when(snapshot.getMean()).thenReturn(mean);
        final long min = TimeUnit.MILLISECONDS.toNanos(300);
        when(snapshot.getMin()).thenReturn(min);
        final double stdDev = TimeUnit.MILLISECONDS.toNanos(400);
        when(snapshot.getStdDev()).thenReturn(stdDev);
        final double median = TimeUnit.MILLISECONDS.toNanos(500);
        when(snapshot.getMedian()).thenReturn(median);
        final double p75 = TimeUnit.MILLISECONDS.toNanos(600);
        when(snapshot.get75thPercentile()).thenReturn(p75);
        final double p95 = TimeUnit.MILLISECONDS.toNanos(700);
        when(snapshot.get95thPercentile()).thenReturn(p95);
        final double p98 = TimeUnit.MILLISECONDS.toNanos(800);
        when(snapshot.get98thPercentile()).thenReturn(p98);
        final double p99 = TimeUnit.MILLISECONDS.toNanos(900);
        when(snapshot.get99thPercentile()).thenReturn(p99);
        final double p999 = TimeUnit.MILLISECONDS.toNanos(1000);
        when(snapshot.get999thPercentile()).thenReturn(p999);

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.map(),
                this.map(),
                this.map(),
                this.map(),
                map("timer", timer));

        int total = 15;
        assertTimeSeries(total, "timer/count", CUMULATIVE, T0, 1 );
        assertTimeSeries(total, "timer/m1_rate", GAUGE, T0, 3.00 );
        assertTimeSeries(total, "timer/m5_rate", GAUGE, T0, 4.00 );
        assertTimeSeries(total, "timer/m15_rate", GAUGE, T0, 5.00 );
        assertTimeSeries(total, "timer/mean_rate", GAUGE, T0, 2.00 );

        assertTimeSeries(total, "timer/max", GAUGE, T0, max );
        assertTimeSeries(total, "timer/mean", GAUGE, T0, mean );
        assertTimeSeries(total, "timer/min", GAUGE, T0, min );
        assertTimeSeries(total, "timer/stddev", GAUGE, T0, stdDev );

        assertTimeSeries(total, "timer/p50", GAUGE, T0, median );
        assertTimeSeries(total, "timer/p75", GAUGE, T0, p75 );
        assertTimeSeries(total, "timer/p95", GAUGE, T0, p95 );
        assertTimeSeries(total, "timer/p98", GAUGE, T0, p98 );
        assertTimeSeries(total, "timer/p99", GAUGE, T0, p99 );
        assertTimeSeries(total, "timer/p999", GAUGE, T0, p999 );

    }

    private Point assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime) {
        final CreateTimeSeriesRequest createTimeSeriesRequest = argumentCaptor.getValue();
        final List<TimeSeries> timeSeriesList = createTimeSeriesRequest.getTimeSeries();

        assertThat(timeSeriesList, hasSize(size));

        final Optional<TimeSeries> timeSeries = FluentIterable.from(timeSeriesList).firstMatch(new Predicate<TimeSeries>() {
            @Override
            public boolean apply(@Nullable TimeSeries input) {
                return input.getMetric().getType().equals(StackdriverMonitoringReporter.CUSTOM_METRIC_PREFIX+"/"+metricType) && input.getMetricKind().equals(metricKind);
            }
        });

        assertTrue(String.format("Could not find timeSeries with metricType=%s and metricKind=%s", metricType, metricKind), timeSeries.isPresent());
        final TimeSeries series = timeSeries.get();
        final List<Point> points = series.getPoints();
        assertThat(points, hasSize(1));

        Point point = points.get(0);
        assertThat(point.getInterval().getEndTime(), is(pointIntervalEndTime));

        return point;
    }

    private void assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime, Integer pointValue) {
        Point point = assertTimeSeries(size, metricType, metricKind, pointIntervalEndTime);
        assertThat(point.getValue().getInt64Value(), is((long)pointValue));
    }

    private void assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime, Long pointValue) {
        Point point = assertTimeSeries(size, metricType, metricKind, pointIntervalEndTime);
        assertThat(point.getValue().getInt64Value(), is(pointValue));
    }

    private void assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime, Double pointValue) {
        Point point = assertTimeSeries(size, metricType, metricKind, pointIntervalEndTime);
        assertThat(point.getValue().getDoubleValue(), is(pointValue));
    }

    private void assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime, Float pointValue) {
        Point point = assertTimeSeries(size, metricType, metricKind, pointIntervalEndTime);
        assertThat(point.getValue().getDoubleValue(), is((double)pointValue));
    }

    private void assertTimeSeries(int size, String metricType, String metricKind, String pointIntervalEndTime, Byte pointValue) {
        Point point = assertTimeSeries(size, metricType, metricKind, pointIntervalEndTime);
        assertThat(point.getValue().getInt64Value(), is( ((Byte)pointValue).longValue()));
    }

    private <T> SortedMap<String, T> map() {
        return new TreeMap<>();
    }

    private <T> SortedMap<String, T> map(String name, T metric) {
        final TreeMap<String, T> map = new TreeMap<>();
        map.put(name, metric);
        return map;
    }

    private <T> Gauge gauge(T value) {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(value);
        return gauge;
    }

}