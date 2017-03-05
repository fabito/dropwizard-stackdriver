package io.github.fabito.dropwizard.samples;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.servlets.PingServlet;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.cloud.trace.apachehttp.TraceRequestInterceptor;
import com.google.cloud.trace.apachehttp.TraceResponseInterceptor;
import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.metrics.StackdriverMonitoringReporter;
import io.github.fabito.dropwizard.samples.resources.PingPongResource;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PingPongApplication extends Application<PingPongConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingPongApplication.class);

    public static void main(final String[] args) throws Exception {
        new PingPongApplication().run(args);
    }

    @Override
    public String getName() {
        return "PingPongApplicationSample";
    }

    @Override
    public void initialize(final Bootstrap<PingPongConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(final PingPongConfiguration configuration,
                    final Environment environment) throws IOException {

        final HttpClient httpClient = new HttpClientBuilder(environment)
                .using(configuration.getHttpClientConfiguration())
                .using(new ImmutableHttpProcessor(
                        Lists.newArrayList(new TraceRequestInterceptor()),
                        Lists.newArrayList(new TraceResponseInterceptor())))
                .build("");

        final HttpTransport httpTransport = new ApacheHttpTransport(httpClient);

        final Monitoring monitoring = new Monitoring.Builder(
                httpTransport,
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

        environment.jersey().register(new PingPongResource());
        environment.servlets().addServlet("ping", new PingServlet()).addMapping("/ping");
    }

}
