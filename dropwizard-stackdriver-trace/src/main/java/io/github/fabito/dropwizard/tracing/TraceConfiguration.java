package io.github.fabito.dropwizard.tracing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import io.dropwizard.setup.Environment;
import io.dropwizard.validation.MinSize;

import javax.validation.constraints.Min;
import java.io.IOException;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceConfiguration {

    @JsonProperty
    @Min(1)
    Integer numThreads = 1;

    @JsonProperty
    @Min(1)
    Integer scheduledDelay = 15;

    @JsonProperty
    String name = "stackdriver-tracing";

    @JsonProperty
    String projectId;

    @JsonProperty
    @Min(0)
    Integer bufferSize = 32 * 1024;

    @JsonProperty
    @MinSize(1)
    String[] urlPatterns = new String[] { "/*" };

    // RateLimitingTraceOptionsFactory
    // NaiveSamplingTraceOptionsFactory
    // Double samplingRate

    // ConstantTraceOptionsFactory
//    Boolean enabled;
//    Boolean stackTraceEnabled;

    TraceService traceService(Environment environment) throws IOException {
        return TraceGrpcApiService.builder()
                .setScheduledDelay(this.scheduledDelay)
                .setProjectId(this.projectId)
                .setBufferSize(this.bufferSize)
                .setScheduledExecutorService(
                        environment.lifecycle()
                                .scheduledExecutorService(this.name)
                                .threads(this.numThreads)
                                .build())
                .build();
    };

    String[] getUrlPatterns() {
        return urlPatterns;
    }

}
