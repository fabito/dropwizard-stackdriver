package io.github.fabito.dropwizard.samples.infrastructure;

import org.glassfish.hk2.api.InterceptionService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

/**
 * Created by fabio on 15/12/16.
 */
public class TraceInterceptorBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(TraceInterceptionService.class)
                .to(InterceptionService.class).in(Singleton.class);
    }
}
