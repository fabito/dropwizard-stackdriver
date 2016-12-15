package io.github.fabito.dropwizard.samples.infrastructure;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceInterceptor.class);


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        LOGGER.info("begin trace");
        try {
            Object returnValue = invocation.proceed();
            LOGGER.info("end trace");
            return returnValue;
        } catch (Throwable t) {
            LOGGER.info("trace exception");
            throw t;
        }
    }

}
