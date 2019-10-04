package io.github.fabito.dropwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.cloud.logging.logback.LoggingAppender;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;

@JsonTypeName("stackdriver-appender")
public class GkeAppenderFactory<E extends DeferredProcessingAware> extends AbstractAppenderFactory<E> {

    private String resourceType;

    @Override
    public Appender<E> build(
            LoggerContext context,
            String applicationName,
            LayoutFactory<E> layoutFactory,
            LevelFilterFactory<E> levelFilterFactory,
            AsyncAppenderFactory<E> asyncAppenderFactory
    ) {
        LoggingAppender appender = new LoggingAppender();
        appender.setLog(applicationName);
        appender.setContext(context);

        appender.addFilter((Filter<ILoggingEvent>) levelFilterFactory.build(threshold));
        getFilterFactories().forEach(f -> appender.addFilter((Filter<ILoggingEvent>) f.build()));

        appender.start();

        return (Appender<E>) appender;
    }

    @JsonProperty
    public String getResourceType() {
        return resourceType;
    }

    @JsonProperty
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
