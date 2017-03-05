package io.github.fabito.dropwizard.samples.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api")
public class PingPongResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingPongResource.class);
    private static final String PING = "PING";
    private static final String PONG = "PONG";

    @GET
    @Path("/ping")
//    @Metered(name = "ping.meterer")
//    @Timed(name = "ping.timer")
//    @ExceptionMetered(name = "ping.exception.meterer")
    public Response ping() {
        log(PING);
        return Response.ok(PING).build();
    }

    @GET
    @Path("/pong")
//    @Metered(name = "pong.meterer")
//    @Timed(name = "pong.timer")
//    @ExceptionMetered(name = "pong.exception.meterer")
    public Response pong() {
        log(PONG);
        return Response.ok(PONG).build();
    }

    private void log(String msg) {
        LOGGER.trace(msg);
        LOGGER.debug(msg);
        LOGGER.info(msg);
        LOGGER.warn(msg);
        LOGGER.error(msg);
    }

}
