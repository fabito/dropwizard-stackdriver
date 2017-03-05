package io.github.fabito.dropwizard.tracing;

import com.codahale.metrics.MetricRegistry;
import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.apachehttp.TraceRequestInterceptor;
import com.google.cloud.trace.apachehttp.TraceResponseInterceptor;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import com.google.cloud.trace.instrumentation.servlet.TraceServletFilter;
import com.google.cloud.trace.service.TraceService;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

/**
 * Created by fabio on 15/12/16.
 */
public abstract class TraceBundle<T> implements ConfiguredBundle<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceBundle.class);

    private TraceService traceService;
    private TraceHttpRequestInterceptor traceHttpRequestInterceptor;
    private TraceHttpResponseInterceptor traceHttpResponseInterceptor;

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        LOGGER.debug("Start bundle");
        TraceConfiguration traceConfiguration = getTraceConfiguration(configuration);
        traceService = traceConfiguration.traceService(environment);

        final SpanContextHandler spanContextHandler = traceService.getSpanContextHandler();
        final SpanContextFactory spanContextFactory = traceService.getSpanContextFactory();
        traceHttpRequestInterceptor = new TraceHttpRequestInterceptor(traceService.getTracer());
        traceHttpResponseInterceptor = new TraceHttpResponseInterceptor(traceService.getTracer());

        final FilterRegistration.Dynamic tracingFilter = environment.servlets().addFilter("tracing-filter", new TraceServletFilter(spanContextHandler, spanContextFactory, traceHttpRequestInterceptor, traceHttpResponseInterceptor));
        tracingFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, traceConfiguration.getUrlPatterns());

        environment.jersey().register(new TracingFeature(traceHttpRequestInterceptor, traceHttpResponseInterceptor));

//        final TraceInterceptorBinder traceInterceptorBinder = new TraceInterceptorBinder(traceService);
//        environment.jersey().register(traceInterceptorBinder);

//        environment.jersey().register(new TraceApplicationEventListener(traceService));

        LOGGER.debug("End bundle");
    }

    public abstract TraceConfiguration getTraceConfiguration(T configuration);

    public Tracer getTracer() {
        return traceService.getTracer();
    }

    public HttpClientBuilder httpClientBuilder(Environment environment) {
        return new TracedHttpClientBuilder(environment, traceHttpRequestInterceptor, traceHttpResponseInterceptor);
    }

    public TraceService getTraceService() {
        return traceService;
    }

    public static class TracedHttpClientBuilder extends HttpClientBuilder {

        private final TraceHttpRequestInterceptor traceHttpRequestInterceptor;
        private final TraceHttpResponseInterceptor traceHttpResponseInterceptor;

        public TracedHttpClientBuilder(MetricRegistry metricRegistry, TraceHttpRequestInterceptor traceHttpRequestInterceptor, TraceHttpResponseInterceptor traceHttpResponseInterceptor) {
            super(metricRegistry);
            this.traceHttpRequestInterceptor = traceHttpRequestInterceptor;
            this.traceHttpResponseInterceptor = traceHttpResponseInterceptor;
        }

        public TracedHttpClientBuilder(Environment environment, TraceHttpRequestInterceptor traceHttpRequestInterceptor,
                TraceHttpResponseInterceptor traceHttpResponseInterceptor) {
            super(environment);
            this.traceHttpRequestInterceptor = traceHttpRequestInterceptor;
            this.traceHttpResponseInterceptor = traceHttpResponseInterceptor;
        }

        @Override
        protected org.apache.http.impl.client.HttpClientBuilder customizeBuilder(org.apache.http.impl.client.HttpClientBuilder builder) {
            builder.addInterceptorFirst(new TraceRequestInterceptor(traceHttpRequestInterceptor));
            builder.addInterceptorLast(new TraceResponseInterceptor(traceHttpResponseInterceptor));
            return builder;
        }
    }

}
