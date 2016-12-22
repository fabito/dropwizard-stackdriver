package io.github.fabito.dropwizard.samples;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.servlets.PingServlet;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.services.monitoring.v3.Monitoring;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.metrics.StackdriverMonitoringReporter;
import io.github.fabito.dropwizard.samples.resources.EchoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SampleApplication extends Application<SampleConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApplication.class);

    public static void main(final String[] args) throws Exception {
        new SampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "StackDriveMetricsReporterSample";
    }

    @Override
    public void initialize(final Bootstrap<SampleConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(final SampleConfiguration configuration,
                    final Environment environment) throws IOException {

        final Monitoring monitoring = new Monitoring.Builder(
                Utils.getDefaultTransport(),
                Utils.getDefaultJsonFactory(), GoogleCredential.getApplicationDefault())
                .setApplicationName(getName())
                .build();

        final StackdriverMonitoringReporter reporter = StackdriverMonitoringReporter.forRegistry(environment.metrics())
                //.prefixedWith("web1.example.com")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build("sc-core-dev", monitoring);
        reporter.start(1, TimeUnit.MINUTES);

        environment.jersey().register(new EchoResource());
        environment.servlets().addServlet("ping", new PingServlet()).addMapping("/ping");
    }

}
