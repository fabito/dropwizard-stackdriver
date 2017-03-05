package io.github.fabito.dropwizard.tracing;

import com.google.cloud.trace.annotation.Span;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Only services
 * that are created by HK2 are candidates for interception.
 */
@Service
public class TraceInterceptionService implements InterceptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceInterceptionService.class);


    @Override
    public Filter getDescriptorFilter() {
//        return StarFilter.getDescriptorFilter();
        return d -> {
            final String clazz = d.getImplementation();
            return clazz.startsWith("io.github.fabito");
        };
    }

    @Override
    public List<MethodInterceptor> getMethodInterceptors(Method method) {
        if (method.isAnnotationPresent(Span.class)) {
            LOGGER.debug(method.getName());
            return Arrays.asList(new TraceInterceptor());
        }
        return null;
//        return Stream.of(method, method.getDeclaringClass())
//                .filter(a -> a.isAnnotationPresent(Span.class))
//                .findAny()
//                .map(a -> Arrays.<MethodInterceptor> asList(new TraceInterceptor()))
//                .orElse(Collections.emptyList());
    }

    @Override
    public List<ConstructorInterceptor> getConstructorInterceptors(Constructor<?> constructor) {
        return Collections.emptyList();
    }
}
