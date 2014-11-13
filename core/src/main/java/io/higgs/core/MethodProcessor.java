package io.higgs.core;

import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MethodProcessor<T extends InvokableMethod> {
    /**
     * Given the set of parameters, construct an {@link InvokableMethod}
     *
     * @param method    the Java class method which is to be processed
     * @param klass     the class to which the method belongs
     * @param factories a factories, which are registered with the server
     * @return an invokable method
     */
    T process(Method method, Class<?> klass, Queue<ObjectFactory> factories);

    /**
     * Used to create new instances of the {@link io.higgs.core.InvokableMethod} this process handles
     */
    T newMethod(Method method, Class<?> klass, Queue<ObjectFactory> factories);

}
