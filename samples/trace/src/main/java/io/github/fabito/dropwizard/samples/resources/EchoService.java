package io.github.fabito.dropwizard.samples.resources;

import com.google.cloud.trace.annotation.Label;
import com.google.cloud.trace.annotation.Option;
import com.google.cloud.trace.annotation.Span;

/**
 * Created by fabio on 15/12/16.
 */
public class EchoService {

    @Span(callLabels = Option.TRUE, stackTrace = Option.TRUE)
    public String echo(@Label(name = "echo") String echo) throws InterruptedException {
        Thread.sleep((long) (Math.random() * 2000));
        return echo;
    }

}
