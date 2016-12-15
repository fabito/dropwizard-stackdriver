package io.github.fabito.dropwizard.samples.infrastructure;

import com.google.cloud.trace.SpanContextHandler;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class TraceAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceAspect.class);

    private final SpanContextFactory spanContextFactory;
    private final SpanContextHandler spanContextHandler;
    private final Tracer tracer;

    private SpanContextHandle handle;
    private SpanContext context;
    private Method method;

    private TraceContext traceContext;
    private Labels.Builder labelsBeforeCallBuilder = Labels.builder();
    private Labels.Builder labelsAfterCallBuilder = Labels.builder();
    private Span span;

    public TraceAspect(SpanContextFactory spanContextFactory, SpanContextHandler spanContextHandler, Tracer tracer) {
        this.spanContextFactory = spanContextFactory;
        this.spanContextHandler = spanContextHandler;
        this.tracer = tracer;
    }

    public void beforeStart(Method method2, Span span2) {
        LOGGER.info("start");
        method = method2;
        context = spanContextFactory.initialContext();
        handle = spanContextHandler.attach(context);
        span = span2;
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


    public void afterEnd() {
        LOGGER.info("end");
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


    public void onError(Throwable t) {
        LOGGER.info("error");

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
