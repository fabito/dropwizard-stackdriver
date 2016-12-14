package io.github.fabito.dropwizard.samples;

import com.codahale.metrics.servlets.PingServlet;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.samples.resources.EchoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleApplication extends Application<SampleConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApplication.class);

    public static void main(final String[] args) throws Exception {
        new SampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "PeopleApi";
    }

    @Override
    public void initialize(final Bootstrap<SampleConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(final SampleConfiguration configuration,
                    final Environment environment) {

        environment.jersey().register(new EchoResource());
        environment.servlets().addServlet("ping", new PingServlet()).addMapping("/ping");
    }

}
