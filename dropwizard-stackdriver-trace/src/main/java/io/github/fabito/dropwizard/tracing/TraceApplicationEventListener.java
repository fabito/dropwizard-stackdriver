package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.service.TraceService;
import com.google.common.collect.Lists;
import org.glassfish.jersey.server.internal.monitoring.CompositeRequestEventListener;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceApplicationEventListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceApplicationEventListener.class);

    private Map<Method, Span> methodMap = new HashMap<>();
    private final TraceService traceService;

    @Inject
    public TraceApplicationEventListener(TraceService traceService) {
        this.traceService = traceService;
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

    final TraceRequestEventListener traceRequestEventListener = new TraceRequestEventListener();

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        final SpanAwareTraceRequestEventListener spanAwareTraceRequestEventListener = new SpanAwareTraceRequestEventListener(this.methodMap, this.traceService);
        return new CompositeRequestEventListener(Lists.newArrayList(traceRequestEventListener, spanAwareTraceRequestEventListener));
    }

    private static class TraceRequestEventListener implements RequestEventListener {
        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                LOGGER.info("############# INIT");
            } else if (event.getType() == RequestEvent.Type.RESP_FILTERS_START) {
                LOGGER.info("############# END");
            }
        }
    }

    private static class SpanAwareTraceRequestEventListener implements RequestEventListener {

        private final Map<Method, Span> methodMap;
        private final TraceService traceService;
        private TraceAroundAdvice traceAspect;

        private SpanAwareTraceRequestEventListener(Map<Method, Span> methodMap, TraceService traceService) {
            this.methodMap = methodMap;
            this.traceService = traceService;
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                final Method method = event.getUriInfo()
                        .getMatchedResourceMethod().getInvocable().getDefinitionMethod();
                Span span = methodMap.get(method);
                if (span != null) {

                    this.traceAspect = new TraceAspect2(span, method, traceService.getTracer());
                    this.traceAspect.beforeStart();
                }
            } else if (event.getType() == RequestEvent.Type.RESP_FILTERS_START) {
                if (traceAspect != null) {
                    traceAspect.afterEnd();
                }
            } else if (event.getType() == RequestEvent.Type.ON_EXCEPTION) {
                if (traceAspect != null) {
                    traceAspect.onError(event.getException());
                }
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
