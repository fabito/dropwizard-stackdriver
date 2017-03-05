package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.core.SpanContextFactory;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import com.google.cloud.trace.jaxrs.TraceContainerFilter;
import com.google.cloud.trace.jaxrs.TraceMessageBodyInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

/**
 * Created by fabio on 15/12/16.
 */
@Provider
public class TracingFeature implements DynamicFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(TracingFeature.class);
    private TraceContainerFilter traceContainerFilter;
    private boolean traceAll;

    public TracingFeature(TraceHttpRequestInterceptor requestInterceptor,
                          TraceHttpResponseInterceptor responseInterceptor) {

        traceContainerFilter = new TraceContainerFilter(requestInterceptor, responseInterceptor);
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
//        final Method method = resourceInfo.getResourceMethod();
//        if (resourceInfo.getResourceMethod().isAnnotationPresent(Span.class)) {
//            LOGGER.info(resourceInfo.getResourceClass().getSimpleName() + "." + method.getName());
//            Span span = method.getAnnotation(Span.class);
//            context.register(traceContainerFilter);
//        } else {
//            if (traceAll) {
//                context.register(traceContainerFilter);
//            }
//        }
        context.register(traceContainerFilter);
//        context.register(new TraceMessageBodyInterceptor());
    }
}
