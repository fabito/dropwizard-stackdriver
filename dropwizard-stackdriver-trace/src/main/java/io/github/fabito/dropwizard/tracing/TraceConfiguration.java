package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.util.List;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceConfiguration {

    Integer numThreads;
    String name;
    String projectId;
    Integer bufferSize;

    // Patterns for filter
    // defaults to /*
    List<String> urlPatterns;

    // RateLimitingTraceOptionsFactory

    // NaiveSamplingTraceOptionsFactory
    // Double samplingRate

    // ConstantTraceOptionsFactory
    Boolean enabled;
    Boolean stackTraceEnabled;

    TraceService traceService(Environment environment) throws IOException {
        return TraceGrpcApiService.builder()
                .setProjectId(projectId)
                .setScheduledDelay(1)
                //.setBufferSize()
                //.setTraceOptionsFactory(TraceOptionsFactory)
                //.setScheduledExecutorService(scheduledExecutorService)
                .build();
    };

}
