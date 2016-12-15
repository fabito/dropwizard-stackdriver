package io.github.fabito.dropwizard.samples;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.github.fabito.dropwizard.samples.infrastructure.TraceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SampleConfiguration extends Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleConfiguration.class);

    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

    private TraceConfiguration traceConfiguration;

    @JsonProperty("httpClient")
    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }

    @JsonProperty("httpClient")
    public void setHttpClientConfiguration(HttpClientConfiguration httpClient) {
        this.httpClient = httpClient;
    }

    @JsonProperty("trace")
    public TraceConfiguration getTraceConfiguration() {
        return traceConfiguration;
    }

    @JsonProperty("trace")
    public void setTraceConfiguration(TraceConfiguration traceConfiguration) {
        this.traceConfiguration = traceConfiguration;
    }
}



