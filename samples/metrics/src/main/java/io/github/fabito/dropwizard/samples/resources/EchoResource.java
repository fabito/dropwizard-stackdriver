package io.github.fabito.dropwizard.samples.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api")
public class EchoResource {

    @GET
    @Path("/echo")
    @Metered(name = "echo.meterer")
    @Timed(name = "echo.timer")
    @ExceptionMetered(name = "echo.exception.meterer")
    public Response echo(@QueryParam("echo") String echo) {
        return Response.ok(echo).build();
    }

    @GET
    @Path("/badecho")
    @Metered(name = "badecho.meterer")
    @Timed(name = "badecho.timer")
    @ExceptionMetered(name = "badecho.exception.meterer")
    public Response badecho() {
        return Response.serverError().build();
    }



}
