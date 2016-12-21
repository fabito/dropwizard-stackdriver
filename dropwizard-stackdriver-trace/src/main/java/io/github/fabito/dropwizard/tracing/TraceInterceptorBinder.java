package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.service.TraceService;
import org.glassfish.hk2.api.InterceptionService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;

import javax.inject.Singleton;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceInterceptorBinder extends AbstractBinder {

    private final TraceService traceService;

    TraceInterceptorBinder(TraceService traceService) {
        this.traceService = traceService;
    }

    @Override
    protected void configure() {
        bind(this.traceService).to(TraceService.class);
        bind(this.traceService.getTracer()).to(Tracer.class);
        bind(TraceInterceptionService.class)
                .to(InterceptionService.class).in(Singleton.class);
        bind(TraceApplicationEventListener.class)
                .to(ApplicationEventListener.class).in(Singleton.class);
    }
}