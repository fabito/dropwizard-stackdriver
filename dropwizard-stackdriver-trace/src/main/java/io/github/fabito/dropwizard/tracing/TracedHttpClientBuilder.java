package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.apachehttp.TraceRequestInterceptor;
import com.google.cloud.trace.apachehttp.TraceResponseInterceptor;
import com.google.cloud.trace.http.TraceHttpRequestInterceptor;
import com.google.cloud.trace.http.TraceHttpResponseInterceptor;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;

/**
 * {@link HttpClientBuilder} extension which adds tracing
 *
 * @author Fábio Franco Uechi
 */
public class TracedHttpClientBuilder extends HttpClientBuilder {

    private final TraceHttpRequestInterceptor traceHttpRequestInterceptor;
    private final TraceHttpResponseInterceptor traceHttpResponseInterceptor;

    public TracedHttpClientBuilder(Environment environment, TraceHttpRequestInterceptor traceHttpRequestInterceptor,
                                   TraceHttpResponseInterceptor traceHttpResponseInterceptor) {
        super(environment);
        this.traceHttpRequestInterceptor = traceHttpRequestInterceptor;
        this.traceHttpResponseInterceptor = traceHttpResponseInterceptor;
    }

    @Override
    protected org.apache.http.impl.client.HttpClientBuilder customizeBuilder(org.apache.http.impl.client.HttpClientBuilder builder) {
        builder.addInterceptorFirst(new TraceRequestInterceptor(traceHttpRequestInterceptor));
        builder.addInterceptorLast(new TraceResponseInterceptor(traceHttpResponseInterceptor));
        return builder;
    }
}