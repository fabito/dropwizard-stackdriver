package io.github.fabito.dropwizard.samples.resources;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.core.Labels;
import com.google.cloud.trace.core.TraceContext;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 21/12/16.
 */
@Path("/api")
public class PingResource {

    private final Tracer tracer;

    @Inject
    public PingResource(Tracer tracer) {
        this.tracer = tracer;
    }

    @GET
    @Path("/ping")
    public Response get() throws InterruptedException {
        TraceContext traceContext = tracer.startSpan("ping");
        final long sleepTime = (long) (Math.random() * 5000);
        tracer.annotateSpan(traceContext, Labels.builder().add("ping/sleepTime", String.valueOf(sleepTime)).build());
        Thread.sleep(sleepTime);
        tracer.endSpan(traceContext);
        return Response.ok("pong").build();
    }

}
