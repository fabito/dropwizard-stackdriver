package io.github.fabito.dropwizard.samples.infrastructure;

import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Created by fabio on 15/12/16.
 */
public abstract class TraceBundle<T> implements ConfiguredBundle<T> {

    private TraceService traceService;

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        TraceConfiguration traceConfiguration = getTraceConfiguration(configuration);

//        ScheduledExecutorService scheduledExecutorService = environment.lifecycle()
//                .scheduledExecutorService("trace")
//                .threads(1)
//                .build();

        traceService = TraceGrpcApiService.builder()
                .setProjectId("sc-core-dev")
                .setScheduledDelay(1)
                //.setScheduledExecutorService(scheduledExecutorService)
                .build();

        environment.jersey().register(new TraceApplicationEventListener(traceService));
    }

    public abstract TraceConfiguration getTraceConfiguration(T configuration);

    public Tracer getTracer() {
        return traceService.getTracer();
    }

}
