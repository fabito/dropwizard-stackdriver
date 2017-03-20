package io.github.fabito.dropwizard.samples.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api/echo")
public class EchoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoResource.class);

    private static final String MULTILINE = "\n" +
            "   __    _____  ___   ___  ____  _  _  ___\n" +
            "  (  )  (  _  )/ __) / __)(_  _)( \\( )/ __)\n" +
            "   )(__  )(_)(( (_-.( (_-. _)(_  )  (( (_-.\n" +
            "  (____)(_____)\\___/ \\___/(____)(_)\\_)\\___/";

    @GET
    public Response echo(@QueryParam("msg") String echo) {

        LOGGER.trace(echo);
        LOGGER.debug(echo);
        LOGGER.info(echo);
        LOGGER.warn(echo);

        try {
            throw new IllegalArgumentException("Illegal message!!");
        } catch (IllegalArgumentException t) {
            LOGGER.error("Error while echoing.", t);
        }

        LOGGER.info(MULTILINE);

        return Response.ok(echo).build();
    }

}
