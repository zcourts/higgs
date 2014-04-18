package io.higgs.core.reflect.dependency;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.Map;

/**
 * A class capable of storing and providing instances of objects for injection before method invocations
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DependencyProvider {
    private static final DependencyProvider global = new DependencyProvider();
    private NonBlockingHashMap<Class<?>, Object> instances = new NonBlockingHashMap<>();

    static {
        //automatically register the global provider
        Injector.register(global);
    }

    /**
     * Adds a set of instances to this provider
     *
     * @param dependency the dependency to add
     * @return false if instance is null, true otherwise
     */
    public boolean add(Object... dependency) {
        if (dependency == null) {
            return false;
        }
        for (Object o : dependency) {
            instances.put(o.getClass(), o);
        }
        return true;
    }

    /**
     * Returns true if this provider has an instance for the class provided
     * More specifically, if it contains at least one dependency from which the provided class is assignable
     *
     * @param klass the class to check for
     * @return true if an instance of the class exists in this provider
     */
    public boolean has(Class<?> klass) {
        for (Map.Entry<Class<?>, Object> e : instances.entrySet()) {
            if (klass.isAssignableFrom(e.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the instance available for the provided class
     * More specifically, the first dependency from which the provided class is assignable
     *
     * @param klass the class to get the instance for
     * @return the instance or null if no instance exists, use {@link #has(Class)} to ensure an instance exists first
     */
    public Object get(Class<?> klass) {
        for (Map.Entry<Class<?>, Object> e : instances.entrySet()) {
            if (klass.isAssignableFrom(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * @return A singleton provider that can be used globally
     */
    public static DependencyProvider global() {
        return global;
    }

    /**
     * @param objs a set of dependencies
     * @return a new provider with the dependencies provided
     */
    public static DependencyProvider from(Object... objs) {
        DependencyProvider provider = new DependencyProvider();
        for (Object o : objs) {
            provider.add(o);
        }
        return provider;
    }

    public void take(DependencyProvider provider) {
        if (provider != null) {
            instances.putAll(provider.instances);
        }
    }
}
