package io.github.fabito.dropwizard.samples.infrastructure;

import com.google.cloud.trace.annotation.Span;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.glassfish.hk2.internal.StarFilter;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by fabio on 15/12/16.
 */
@Service
public class TraceInterceptionService implements InterceptionService {

    @Override
    public Filter getDescriptorFilter() {
        return StarFilter.getDescriptorFilter();
    }

    @Override
    public List<MethodInterceptor> getMethodInterceptors(Method method) {
        if (method.isAnnotationPresent(Span.class)) {
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
