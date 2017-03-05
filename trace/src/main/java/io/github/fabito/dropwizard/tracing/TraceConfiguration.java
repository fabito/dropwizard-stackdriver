package io.github.fabito.dropwizard.tracing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.trace.service.TraceGrpcApiService;
import com.google.cloud.trace.service.TraceService;
import com.google.common.base.MoreObjects;
import io.dropwizard.setup.Environment;
import io.dropwizard.validation.MinSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * The configuration class used by {@link TraceGrpcApiService.Builder} to build a {@link TraceService}.
 *
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>projectId</td>
 *         <td></td>
 *         <td>(Required) The Google Cloud Platform project id.</td>
 *     </tr>
 *     <tr>
 *         <td>bufferSize</td>
 *         <td>32 * 1024</td>
 *         <td>(Optional) </td>
 *     </tr>
 *     <tr>
 *         <td>numThreads</td>
 *         <td>1</td>
 *         <td>(Optional)</td>
 *     </tr>
 *     <tr>
 *         <td>scheduledDelay</td>
 *         <td>15</td>
 *         <td>(Optional)</td>
 *     </tr>
 *     <tr>
 *         <td>urlPatterns</td>
 *         <td>/*</td>
 *         <td>(Optional) Which endpoints will be traced. Defaults to all.</td>
 *     </tr>
 * </table>
 *
 * @author Fábio Franco Uechi
 */
public class TraceConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceConfiguration.class);

    @JsonProperty
    @Min(1)
    private Integer numThreads = 1;

    @JsonProperty
    @Min(1)
    private Integer scheduledDelay = 15;

    @JsonProperty
    private String name = "stackdriver-tracing";

    @JsonProperty
    @NotNull
    private String projectId;

    @JsonProperty
    @Min(0)
    private Integer bufferSize = 32 * 1024;

    @JsonProperty
    @MinSize(1)
    @NotNull
    private String[] urlPatterns = new String[] { "/*" };

    TraceService traceService(Environment environment) throws IOException {

        LOGGER.info("Setting up a new TraceService");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(this.toString());
        }

        return TraceGrpcApiService.builder()
                .setProjectId(this.projectId)
                .setScheduledDelay(this.scheduledDelay)
                .setBufferSize(this.bufferSize)
                .setScheduledExecutorService(
                        environment.lifecycle()
                                .scheduledExecutorService(this.name)
                                .threads(this.numThreads)
                                .build())
                .build();
    }

    String[] getUrlPatterns() {
        return urlPatterns;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("numThreads", numThreads)
                .add("scheduledDelay", scheduledDelay)
                .add("name", name)
                .add("projectId", projectId)
                .add("bufferSize", bufferSize)
                .add("urlPatterns", urlPatterns)
                .toString();
    }
}
