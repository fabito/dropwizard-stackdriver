package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.core.*;
import com.google.cloud.trace.service.TraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class TraceAspect implements TraceAroundAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceAspect.class);

    private final Method method;
    private final Span span;
    private final SpanContextFactory spanContextFactory;
    private final SpanContextHandler spanContextHandler;
    private final Tracer tracer;

    private SpanContextHandle handle;
    private TraceContext traceContext;
    private Labels.Builder labelsBeforeCallBuilder = Labels.builder();
    private Labels.Builder labelsAfterCallBuilder = Labels.builder();

    public TraceAspect(Span span, Method method, TraceService traceService) {
        this.spanContextFactory = traceService.getSpanContextFactory();
        this.spanContextHandler = traceService.getSpanContextHandler();
        this.tracer = traceService.getTracer();
        this.span = span;
        this.method = method;
    }

    @Override
    public void beforeStart() {
        SpanContext spanContext = spanContextFactory.initialContext();
        handle = spanContextHandler.attach(spanContext);
        if (span.callLabels() == Option.TRUE) {
            labelsBeforeCallBuilder
                    .add("trace.cloud.google.com/call/class",
                            method.getDeclaringClass().getName())
                    .add("trace.cloud.google.com/call/method",
                            method.getName())
                    .add("trace.cloud.google.com/call/package",
                            method.getDeclaringClass().getPackage().getName());
        }
        if (span.entry()) {
            labelsBeforeCallBuilder.add("trace.cloud.google.com/agent", "cloud-trace-java/0.1");
        }

        String methodName = getMethodName(method);
        Boolean enableTrace = span.trace().getBooleanValue();
        Boolean enableStackTrace = span.stackTrace().getBooleanValue();
        traceContext = tracer.startSpan(methodName, new StartSpanOptions()
                .setEnableTrace(enableTrace).setEnableStackTrace(enableStackTrace));


        Labels labelsBeforeCall = labelsBeforeCallBuilder.build();
        if (labelsBeforeCall.getLabels().size() > 0) {
            tracer.annotateSpan(traceContext, labelsBeforeCall);
        }
    }


    @Override
    public void afterEnd() {
        boolean stackTraceEnabled = traceContext.getHandle().getCurrentSpanContext().getTraceOptions()
                .getStackTraceEnabled();
        if (stackTraceEnabled) {
            StackTrace.Builder stackTraceBuilder = ThrowableStackTraceHelper.createBuilder(new Exception());
            tracer.setStackTrace(traceContext, stackTraceBuilder.build());
        }
        Labels labelsAfterCall = labelsAfterCallBuilder.build();
        if (labelsAfterCall.getLabels().size() > 0) {
            tracer.annotateSpan(traceContext, labelsAfterCall);
        }
        tracer.endSpan(traceContext);
        handle.detach();
    }


    @Override
    public void onError(Throwable t) {
        if (span.callLabels() == Option.TRUE) {
            labelsAfterCallBuilder
                    .add("trace.cloud.google.com/exception/class", t.getClass().getName())
                    .add("trace.cloud.google.com/exception/message", t.getMessage());
        }
    }


    private String getMethodName(Method method) {
        return String.format("%s.%s", method.getDeclaringClass().getSimpleName(),
                method.getName());
    }

}
