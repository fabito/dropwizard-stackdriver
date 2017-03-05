package io.github.fabito.dropwizard.samples.resources;

import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by fabio on 07/12/16.
 */
@Path("/api")
@Service
public class EchoResource {

    private final EchoService echoService;
    private final HttpClient httpClient;

    @Inject
    public EchoResource(EchoService echoService, HttpClient httpClient) {
        this.echoService = echoService;
        this.httpClient = httpClient;
    }

    @GET
    @Path("/echo")
    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public Response echo(@QueryParam("echo") String echo) throws InterruptedException {
        Thread.sleep((long) (Math.random() * 2000));
        return Response.ok(echoService.echo(echo)).build();
    }

    @GET
    @Path("/longgecho")
    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public Response longgecho(@QueryParam("echo") String echo) throws InterruptedException, IOException {
        Thread.sleep((long) (Math.random() * 2000));
        final String echo1 = echoService.echo(echo);
        final HttpGet httpGet = new HttpGet("http://localhost:8080/api/echo?echo=TRALALAL");
        httpClient.execute(httpGet);
        return Response.ok(echo1).build();
    }

    @GET
    @Path("/echofail")
    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public Response echofail(@QueryParam("echo") String echo) {
        throw new IllegalArgumentException("Caramba!!!");
    }
}
