package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.TraceContext;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;

import java.io.IOException;

/**
 * Created by fabio on 15/12/16.
 */
class TraceHttpProcessor implements HttpProcessor {

    private static final String TRACE_CONTEXT = "traceContext";
    private final Tracer tracer;

    TraceHttpProcessor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

        request.getRequestLine().getMethod();
        request.getRequestLine().getUri();
        request.getRequestLine().getProtocolVersion();

        TraceContext ctx = tracer.startSpan("");
        context.setAttribute(TRACE_CONTEXT, ctx);
        //tracer.annotateSpan();
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        TraceContext ctx = (TraceContext) context.getAttribute(TRACE_CONTEXT);

        response.getStatusLine().getStatusCode();
        response.getStatusLine().getReasonPhrase();
        response.getFirstHeader("");

        tracer.endSpan(ctx);
    }
}
