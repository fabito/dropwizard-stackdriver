package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import com.google.cloud.trace.servlet.TraceServletFilter;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extras.ExtrasUtilities;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Created by fabio on 15/12/16.
 */
public abstract class TraceBundle<T> implements ConfiguredBundle<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceBundle.class);


    private TraceService traceService;

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        LOGGER.info("Running bundle");

        TraceConfiguration traceConfiguration = getTraceConfiguration(configuration);

//        ScheduledExecutorService scheduledExecutorService = environment.lifecycle()
//                .scheduledExecutorService("trace")
//                .threads(1)
//                .build();

        traceService = TraceGrpcApiService.builder()
                .setProjectId("sc-core-dev")
                .setScheduledDelay(1)
                //.setBufferSize()
                //.setTraceOptionsFactory(TraceOptionsFactory)
                //.setScheduledExecutorService(scheduledExecutorService)
                .build();

        final SpanContextHandler spanContextHandler = traceService.getSpanContextHandler();
        final SpanContextFactory spanContextFactory = traceService.getSpanContextFactory();
        final TraceHttpRequestInterceptor requestInterceptor = new TraceHttpRequestInterceptor(traceService.getTracer());
        final TraceHttpResponseInterceptor responseInterceptor = new TraceHttpResponseInterceptor(traceService.getTracer());

        final FilterRegistration.Dynamic tracingFilter = environment.servlets().addFilter("tracing-filter", new TraceServletFilter(spanContextHandler, spanContextFactory, requestInterceptor, responseInterceptor));
        tracingFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        final TraceInterceptorBinder traceInterceptorBinder = new TraceInterceptorBinder(traceService);

//        ServiceLocator serviceLocator = ServiceLocatorUtilities.bind(UUID.randomUUID().toString(), traceInterceptorBinder);
//        environment.getApplicationContext().getAttributes().setAttribute(ServletProperties.SERVICE_LOCATOR, serviceLocator);

//        ServiceLocator locator = ServiceLocatorUtilities.bind(traceInterceptorBinder);
//        InjectionBridge.setApplicationServiceLocator(locator);
//        environment.jersey().register(InjectionBridge.class);
        environment.jersey().register(traceInterceptorBinder);

//        environment.jersey().getResourceConfig().register(new ContainerLifecycleListener() {
//            public void onStartup(Container container) {
//                /*Get the HK2 Service Locator*/
//                ServiceLocator serviceLocator = container.getApplicationHandler().getServiceLocator();
//                //serviceLocator.getService()
//
//                //environment.jersey().register();
//            }
//
//            public void onReload(Container container) {/*...*/}
//
//            public void onShutdown(Container container) {/*...*/}
//        });

//        environment.jersey().register(new TracingFeature(requestInterceptor, responseInterceptor));
//        environment.jersey().register(new TraceApplicationEventListener(traceService));
    }

    public abstract TraceConfiguration getTraceConfiguration(T configuration);

    public Tracer getTracer() {
        return traceService.getTracer();
    }


    @Provider
    public static class InjectionBridge implements Feature
    {
        private static ServiceLocator _applicationServiceLocator;

        private final ServiceLocator _serviceLocator;

        @Inject
        private InjectionBridge(ServiceLocator serviceLocator)
        {
            _serviceLocator = serviceLocator;
        }

        @Override
        public boolean configure(FeatureContext context)
        {
            if (_applicationServiceLocator != null)
                ExtrasUtilities.bridgeServiceLocator(_serviceLocator, _applicationServiceLocator);
            return true;
        }

        public static void setApplicationServiceLocator(ServiceLocator applicationServiceLocator)
        {
            _applicationServiceLocator = applicationServiceLocator;
        }
    }
}
