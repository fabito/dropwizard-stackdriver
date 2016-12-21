package io.github.fabito.dropwizard.samples;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Trace;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.core.TraceContext;
import com.google.cloud.trace.guice.annotation.TracerSpanModule;
import com.google.cloud.trace.guice.servlet.RequestLabelerModule;
import com.google.cloud.trace.guice.servlet.RequestTraceContextFilter;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.fabito.dropwizard.samples.resources.EchoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller;

import javax.inject.Singleton;
import java.io.IOException;

public class SampleApplication extends Application<SampleConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleApplication.class);

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

        bootstrap.addBundle(GuiceBundle.<SampleConfiguration>builder()
                .installers(ResourceInstaller.class)
                .modules(
                        new TracerSpanModule("dropwizard-gpc-sample"),
                        new RequestLabelerModule(),
                        new ServletModule() {
                            @Override
                            protected void configureServlets() {
                                filter("/*").through(RequestTraceContextFilter.class);
                            }

                            @Provides
                            @Singleton
                            TraceService traceService() throws IOException {
                                return TraceGrpcApiService.builder()
                                            .setProjectId("set-me-up")
                                            .setScheduledDelay(1)
                                            .build();
                            }

                            @Provides
                            @Singleton
                            SpanContextFactory spanContextFactory(TraceService traceService){
                                return  traceService.getSpanContextFactory();
                            }

                            @Provides
                            @Singleton
                            SpanContextHandler spanContextHandler(TraceService traceService){
                                return  traceService.getSpanContextHandler();
                            }

                            @Provides
                            @Singleton
                            Tracer tracer(TraceService traceService){
                                return  traceService.getTracer();
                            }

                        }
                )
                .extensions(EchoResource.class)
                .build()
        );

    }

    @Override
    public void run(final SampleConfiguration configuration,
                    final Environment environment) {
        Tracer tracer = Trace.getTracer();
        TraceContext context = tracer.startSpan("test-span");
        tracer.endSpan(context);
    }

}
