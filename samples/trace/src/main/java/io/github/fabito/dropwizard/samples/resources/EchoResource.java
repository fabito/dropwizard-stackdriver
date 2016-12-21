package io.github.fabito.dropwizard.samples.resources;

import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api")
public class EchoResource {

    private final EchoService echoService;

    @Inject
    public EchoResource(EchoService echoService) {
        this.echoService = echoService;
    }

    @GET
    @Path("/echo")
    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public Response get(@QueryParam("echo") String echo) throws InterruptedException {
        Thread.sleep((long) (Math.random() * 5000));
        return Response.ok(echoService.echo(echo)).build();
    }

    @GET
    @Path("/echofail")
    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public Response getEx(@QueryParam("echo") String echo) {
        throw new IllegalArgumentException("Caramba!!!");
    }
}
