package io.github.fabito.dropwizard.samples;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.samples.infrastructure.SpanAwareProxyFactory;
import io.github.fabito.dropwizard.samples.infrastructure.TraceBundle;
import io.github.fabito.dropwizard.samples.infrastructure.TraceConfiguration;
import io.github.fabito.dropwizard.samples.resources.EchoResource;
import io.github.fabito.dropwizard.samples.resources.EchoService;
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

//        environment.lifecycle()
//                .scheduledExecutorService("trace")
//                .threads(1)
//                .build();
//
//        final HttpClient httpClient = new HttpClientBuilder(environment)
//                .using(configuration.getHttpClientConfiguration())
//                .build("");

        SpanAwareProxyFactory factory = new SpanAwareProxyFactory(traceBundle);
        EchoService echoService = factory.create(EchoService.class);
        environment.jersey().register(new EchoResource(echoService));
    }

}
