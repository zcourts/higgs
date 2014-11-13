package io.higgs.events;

import io.higgs.core.MethodProcessor;
import io.higgs.core.ObjectFactory;

import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class EventMethodProcessor implements MethodProcessor<EventMethod> {
    @Override
    public EventMethod process(Method method, Class<?> klass, Queue<ObjectFactory> factories) {
        return newMethod(method, klass, factories);
    }

    @Override
    public EventMethod newMethod(Method method, Class<?> klass, Queue<ObjectFactory> factories) {
        return new EventMethod(factories, klass, method);
    }
}
