package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
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
 * Adds <a href="https://cloud.google.com/trace/">Stackdriver Trace</a> tracing capabilities to an application.
 *
 * <code>
 *    private final TraceBundle<SampleConfiguration> traceBundle =
 *      new TraceBundle<SampleConfiguration>() {
 *         @Override
 *         public TraceConfiguration getTraceConfiguration(SampleConfiguration configuration) {
 *             return configuration.getTraceConfiguration();
 *      }
 *    };
 *
 *    @Override
 *    public void initialize(final Bootstrap<SampleConfiguration> bootstrap) {
 *      bootstrap.addBundle(traceBundle);
 *    }
 * </code>
 *
 * @author Fábio Franco Uechi
 */
public abstract class TraceBundle<T> implements ConfiguredBundle<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceBundle.class);

    private TraceService traceService;
    private TraceHttpRequestInterceptor traceHttpRequestInterceptor;
    private TraceHttpResponseInterceptor traceHttpResponseInterceptor;

    public abstract TraceConfiguration getTraceConfiguration(T configuration);

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        LOGGER.debug("Setting up Stackdriver tracing infrastructure");
        TraceConfiguration traceConfiguration = getTraceConfiguration(configuration);
        traceService = traceConfiguration.traceService(environment);

        final SpanContextHandler spanContextHandler = traceService.getSpanContextHandler();
        final SpanContextFactory spanContextFactory = traceService.getSpanContextFactory();
        LOGGER.debug("Creating request and response interceptors");
        traceHttpRequestInterceptor = new TraceHttpRequestInterceptor(traceService.getTracer());
        traceHttpResponseInterceptor = new TraceHttpResponseInterceptor(traceService.getTracer());

        final String[] urlPatterns = traceConfiguration.getUrlPatterns();
        LOGGER.debug("Registering tracing filter using patterns: {}", (Object[]) urlPatterns);
        final FilterRegistration.Dynamic tracingFilter = environment.servlets().addFilter("tracing-filter", new TraceServletFilter(spanContextHandler, spanContextFactory, traceHttpRequestInterceptor, traceHttpResponseInterceptor));
        tracingFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, urlPatterns);

        LOGGER.debug("Stackdriver tracing up and running");
    }

    public Tracer getTracer() {
        return traceService.getTracer();
    }

    public HttpClientBuilder httpClientBuilder(Environment environment) {
        return new TracedHttpClientBuilder(environment, traceHttpRequestInterceptor, traceHttpResponseInterceptor);
    }

    public TraceService getTraceService() {
        return traceService;
    }

}
