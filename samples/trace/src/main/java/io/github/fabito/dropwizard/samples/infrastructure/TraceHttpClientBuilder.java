package io.github.fabito.dropwizard.samples.infrastructure;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceHttpClientBuilder extends HttpClientBuilder {

    public TraceHttpClientBuilder(MetricRegistry metricRegistry) {
        super(metricRegistry);
    }

    public TraceHttpClientBuilder(Environment environment) {
        super(environment);
    }




}
