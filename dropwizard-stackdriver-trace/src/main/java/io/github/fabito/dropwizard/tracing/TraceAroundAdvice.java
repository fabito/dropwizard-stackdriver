package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.annotation.Span;

import java.lang.reflect.Method;

/**
 * Created by fabio on 16/12/16.
 */
public interface TraceAroundAdvice {

    void beforeStart();

    void afterEnd();

    void onError(Throwable t);
}
