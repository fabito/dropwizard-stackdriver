package io.github.fabito.dropwizard.samples.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api/echo")
public class EchoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoResource.class);

    @GET
    public Response echo(@QueryParam("msg") String echo) throws IOException {

        LOGGER.trace(echo);
        LOGGER.debug(echo);
        LOGGER.info(echo);
        LOGGER.warn(echo);

        try {
            Preconditions.checkState(echo.equals("ha"));
        } catch (IllegalStateException t) {
            LOGGER.error("Error while echoing.", t);
        }

        InputStream is = this.getClass().getResourceAsStream("/banner.txt");
        String multiline = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        LOGGER.info(multiline);
        Closeables.closeQuietly(is);

        return Response.ok(echo).build();
    }

}
