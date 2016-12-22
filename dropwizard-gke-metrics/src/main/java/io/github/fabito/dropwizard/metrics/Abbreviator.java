package io.github.fabito.dropwizard.metrics;

/**
 * Created by fabio on 22/12/16.
 */
public interface Abbreviator {

    String DOT = ".";

    String abbreviate(String fqClassName);
}
