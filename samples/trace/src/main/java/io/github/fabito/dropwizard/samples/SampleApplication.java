package io.github.fabito.dropwizard.samples;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.samples.resources.EchoResource;
import io.github.fabito.dropwizard.samples.resources.EchoService;
import io.github.fabito.dropwizard.samples.resources.PingResource;
import io.github.fabito.dropwizard.tracing.TraceBundle;
import io.github.fabito.dropwizard.tracing.TraceConfiguration;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SampleApplication extends Application<SampleConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApplication.class);

    private final TraceBundle<SampleConfiguration> traceBundle =
        new TraceBundle<SampleConfiguration>() {
            @Override
            public TraceConfiguration getTraceConfiguration(SampleConfiguration configuration) {
                return configuration.getTraceConfiguration();
            }
    };

    public static void main(final String[] args) throws Exception {
        new SampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "Trace";
    }

    @Override
    public void initialize(final Bootstrap<SampleConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(traceBundle);
    }

    @Override
    public void run(final SampleConfiguration configuration,
                    final Environment environment) throws IOException {

        final HttpClient httpClient = traceBundle.httpClientBuilder(environment)
                .using(configuration.getHttpClientConfiguration())
                .build("echo-http-client");

        LOGGER.info("Registering resources");
        environment.jersey().register(PingResource.class);
        environment.jersey().register(new EchoResource(new EchoService(), httpClient));
    }
}
