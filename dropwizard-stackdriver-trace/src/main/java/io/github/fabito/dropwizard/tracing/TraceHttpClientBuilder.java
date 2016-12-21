package io.github.fabito.dropwizard.tracing;

import com.codahale.metrics.MetricRegistry;
import com.google.cloud.trace.Tracer;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceHttpClientBuilder extends HttpClientBuilder {

    private final Tracer tracer;

    public TraceHttpClientBuilder(MetricRegistry metricRegistry, Tracer tracer) {
        super(metricRegistry);
        this.tracer = tracer;
    }

    public TraceHttpClientBuilder(Environment environment, Tracer tracer) {
        super(environment);
        this.tracer = tracer;
    }

    @Override
    protected org.apache.http.impl.client.HttpClientBuilder customizeBuilder(org.apache.http.impl.client.HttpClientBuilder builder) {
        return builder.setHttpProcessor(new TraceHttpProcessor(tracer));
    }

}
