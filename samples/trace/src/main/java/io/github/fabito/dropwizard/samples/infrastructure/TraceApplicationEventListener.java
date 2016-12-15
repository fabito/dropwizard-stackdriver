package io.github.fabito.dropwizard.samples.infrastructure;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.service.TraceService;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceApplicationEventListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceApplicationEventListener.class);

    private Map<Method, Span> methodMap = new HashMap<>();
    private final SpanContextFactory spanContextFactory;
    private final SpanContextHandler spanContextHandler;
    private final Tracer tracer;

    public TraceApplicationEventListener(TraceService traceService) {
        this.spanContextFactory = traceService.getSpanContextFactory();
        this.spanContextHandler = traceService.getSpanContextHandler();
        this.tracer = traceService.getTracer();
    }


    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            for (Resource resource : event.getResourceModel().getResources()) {
                for (ResourceMethod method : resource.getAllMethods()) {
                    registerSpanAnnotations(method);
                }

                for (Resource childResource : resource.getChildResources()) {
                    for (ResourceMethod method : childResource.getAllMethods()) {
                        registerSpanAnnotations(method);
                    }
                }
            }
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new TraceRequestEventListener();
    }

    private class TraceRequestEventListener implements RequestEventListener {

        private final TraceAspect traceAspect;

        private TraceRequestEventListener() {
            this.traceAspect = new TraceAspect(spanContextFactory, spanContextHandler, tracer);
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                final Method method = event.getUriInfo()
                        .getMatchedResourceMethod().getInvocable().getDefinitionMethod();
                Span span = methodMap.get(method);
                traceAspect.beforeStart(method, span);
            } else if (event.getType() == RequestEvent.Type.RESP_FILTERS_START) {
                traceAspect.afterEnd();
            } else if (event.getType() == RequestEvent.Type.ON_EXCEPTION) {
                traceAspect.onError(event.getException());
            }
        }
    }

    private void registerSpanAnnotations(ResourceMethod method) {
        Span annotation = method.getInvocable().getDefinitionMethod().getAnnotation(Span.class);

        if (annotation == null) {
            annotation = method.getInvocable().getHandlingMethod().getAnnotation(Span.class);
        }

        if (annotation != null) {
            this.methodMap.put(method.getInvocable().getDefinitionMethod(), annotation);
        }

    }

}
