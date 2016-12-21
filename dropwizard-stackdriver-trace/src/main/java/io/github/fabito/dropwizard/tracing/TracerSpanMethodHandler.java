package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.annotation.Label;
import com.google.cloud.trace.annotation.Name;
import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.core.*;
import com.google.cloud.trace.guice.annotation.Labeler;
import com.google.common.base.CaseFormat;
import javassist.util.proxy.MethodHandler;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

public class TracerSpanMethodHandler implements MethodHandler {

  private final Tracer tracer;
  private final Provider<Map<String, Labeler>> labelerMapProvider;
  private final String labelHost;

  public TracerSpanMethodHandler(
      Tracer tracer,
      Provider<Map<String, Labeler>> labelerMapProvider,
      String labelHost) {
    this.tracer = tracer;
    this.labelerMapProvider = labelerMapProvider;
    this.labelHost = labelHost;
  }

  @Override
  public Object invoke(Object self, Method overridden, Method proceed, Object[] args) throws Throwable {
    Span span = overridden.getAnnotation(Span.class);

    Labels.Builder labelsBeforeCallBuilder = Labels.builder();

    if (span.callLabels() == Option.TRUE) {
      labelsBeforeCallBuilder
              .add("trace.cloud.google.com/call/class",
                      overridden.getDeclaringClass().getName())
              .add("trace.cloud.google.com/call/method",
                      overridden.getName())
              .add("trace.cloud.google.com/call/package",
                      overridden.getDeclaringClass().getPackage().getName());
    }
    if (span.entry()) {
      labelsBeforeCallBuilder.add("trace.cloud.google.com/agent", "cloud-trace-java/0.1");
    }

    String methodName;
    Name name = overridden.getAnnotation(Name.class);
    if (name != null) {
      methodName = name.value();
    } else {
      String overrideName = null;
      for (int i = 0; i < span.labels().length; i++) {
        Labeler labeler = labelerMapProvider.get().get(span.labels()[i]);
        if (labeler != null) {
          labeler.addLabelsBeforeCall(labelsBeforeCallBuilder);
          String labelerName = labeler.overrideName();
          if (labelerName != null) {
            overrideName = labelerName;
          }
        }
      }
      if (overrideName != null) {
        methodName = overrideName;
      } else {
        methodName = getMethodName(overridden);
      }
    }

    Boolean enableTrace = span.trace().getBooleanValue();
    Boolean enableStackTrace = span.stackTrace().getBooleanValue();
    TraceContext traceContext = tracer.startSpan(methodName, new StartSpanOptions()
            .setEnableTrace(enableTrace).setEnableStackTrace(enableStackTrace));
    boolean stackTraceEnabled = traceContext.getHandle().getCurrentSpanContext().getTraceOptions()
            .getStackTraceEnabled();

    boolean labelAllParams = span.labelAll();
    String labelPrefix;
    if (span.labelPrefix().equals("/")) {
      labelPrefix = getMethodLabelPrefix(overridden);
    } else {
      labelPrefix = span.labelPrefix();
    }
    addParameterAnnotations(
            overridden, args, labelsBeforeCallBuilder, labelAllParams, labelHost + labelPrefix);

    Labels labelsBeforeCall = labelsBeforeCallBuilder.build();
    if (labelsBeforeCall.getLabels().size() > 0) {
      tracer.annotateSpan(traceContext, labelsBeforeCall);
    }

    Labels.Builder labelsAfterCallBuilder = Labels.builder();
    // This won't be returned if an exception is throw, so I'll set it to null here to keep the
    // compiler happy.
    Object result = null;
    // This should be set below so the stack frame will be on the same line as the invocation, but
    // the compiler complains if I don't initialize this.
    Throwable throwable = new Exception();
    try {
      throwable = new Exception(); result = proceed.invoke(self, args);
    } catch (Throwable t) {
      if (span.callLabels() == Option.TRUE) {
        labelsAfterCallBuilder
                .add("trace.cloud.google.com/exception/class", t.getClass().getName())
                .add("trace.cloud.google.com/exception/message", t.getMessage());
      }
    } finally {
      if (stackTraceEnabled) {
        StackTrace.Builder builder = StackTrace.builder();
        builder.add(overridden.getDeclaringClass().getName(),
                overridden.getName(), null, null, null);
        ThrowableStackTraceHelper.addFrames(builder, throwable);
        tracer.setStackTrace(traceContext, builder.build());
      }

      for (int i = span.labels().length; i > 0; i--) {
        Labeler labeler = labelerMapProvider.get().get(span.labels()[i - 1]);
        if (labeler != null) {
          labeler.addLabelsAfterCall(labelsAfterCallBuilder);
        }
      }
      Labels labelsAfterCall = labelsAfterCallBuilder.build();
      if (labelsAfterCall.getLabels().size() > 0) {
        tracer.annotateSpan(traceContext, labelsAfterCall);
      }
      tracer.endSpan(traceContext);
    }
    return result;
  }

  private String getMethodName(Method method) {
    return String.format("%s.%s", method.getDeclaringClass().getSimpleName(),
        method.getName());
  }

  private String getMethodLabelPrefix(Method method) {
    String className = method.getDeclaringClass().getSimpleName();
    String methodName = method.getName();
    if (className.isEmpty()) {
      return String.format("/%s", convertJavaName(methodName));
    } else {
      return String.format("/%s/%s", convertJavaName(className), convertJavaName(methodName));
    }
  }

  private String convertJavaName(String name) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
  }

  private void addParameterAnnotations(Method invocation,
                                       Object[] args, Labels.Builder labelsBuilder, boolean labelAllParams, String labelPrefix) {
    Annotation[][] annotationsArray = invocation.getParameterAnnotations();
    for (int i = 0; i < annotationsArray.length; i++) {
      Label label = null;
      Name name = null;
      for (Annotation annotation : annotationsArray[i]) {
        if (annotation.annotationType() == Label.class) {
          label = (Label)annotation;
        } else if (annotation.annotationType() == Name.class) {
          name = (Name)annotation;
        }
      }
      boolean enabled;
      String parameterName;
      if (label != null) {
        enabled = label.enabled();
        if (label.name().equals("/")) {
          parameterName = String.format("%s/arg%d", labelPrefix, i);
        } else if (label.name().isEmpty()) {
          parameterName = labelPrefix;
        } else {
          parameterName = String.format("%s/%s", labelPrefix, label.name());
        }
      } else {
        enabled = false;
        parameterName = String.format("%s/arg%d", labelPrefix, i);
      }
      if (labelAllParams) {
        enabled = true;
      }
      if (name != null) {
        parameterName = name.value();
      }
      if (enabled) {
        labelsBuilder.add(parameterName, args[i].toString());
      }
    }
  }

}
