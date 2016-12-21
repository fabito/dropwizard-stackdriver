package io.github.fabito.dropwizard.samples.resources;

import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;
import com.google.cloud.trace.guice.servlet.RequestLabeler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api/echo")
public class EchoResource {

    @GET
    @Span(callLabels = Option.TRUE, labels = { RequestLabeler.KEY })
    public Response getV(@QueryParam("echo") String echo) {
        return Response.ok(echo).build();
    }

}
