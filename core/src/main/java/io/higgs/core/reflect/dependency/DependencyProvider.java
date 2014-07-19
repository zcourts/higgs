package io.higgs.core.reflect.dependency;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class capable of storing and providing instances of objects for injection before method invocations
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DependencyProvider {
    private static final DependencyProvider global = new DependencyProvider();
    private NonBlockingHashMap<Class<?>, Object> instances = new NonBlockingHashMap<>();
    private NonBlockingHashMap<String, Object> named = new NonBlockingHashMap<>();

    static {
        //automatically register the global provider
        Injector.register(global);
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
            if (o != null) {
                instances.put(o.getClass(), o);
            }
        }
        return true;
    }

    /**
     * Adds a named dependency to this provider.
     * A named dependency is only injected if the name of the field matches the name of this parameter
     *
     * @param name  the name which must match the field's name for this parameter to be injected
     * @param value the value of the parameter to be injected if the name matches
     * @return false if name is null or empty or if the given value is null, true otherwise
     */
    public boolean put(String name, Object value) {
        if (name == null || name.isEmpty() || value == null) {
            return false;
        }
        named.put(name, value);
        return true;
    }

    /**
     * Remove a named dependency
     *
     * @param name the name of the dependency to remove
     * @return true if anything was removed, false otherwise
     */
    public boolean remove(String name) {
        return name != null && named.remove(name) != null;
    }

    /**
     * Removes all references of the given object from the dependency provider
     *
     * @param i the object to remove occurrences of
     * @return true if the object was found and removed, false otherwise
     */
    public boolean remove(Object i) {
        if (i == null) {
            return false;
        }
        List<Map.Entry<Class<?>, Object>> entries = new ArrayList<>();
        for (Map.Entry<Class<?>, Object> e : instances.entrySet()) {
            if (e.getValue() == i) {
                entries.add(e);
            }
        }
        for (Map.Entry<Class<?>, Object> e : entries) {
            instances.remove(e.getKey());
        }
        return entries.size() > 0;
    }

    /**
     * Removes the instance of the value for the given class
     *
     * @param key the key to remove
     * @return true if the value was removed, or false if key is null or doesn't exist
     */
    public boolean remove(Class<?> key) {
        return key != null && instances.remove(key) != null;
    }

    /**
     * Remove all dependencies (both named and unnamed)
     */
    public void removeAll() {
        instances.clear();
        named.clear();
    }

    /**
     * Remove all named dependencies
     */
    public void removeAllNamed() {
        named.clear();
    }

    /**
     * Remove all unnamed dependencies
     */
    public void removeAllUnnamed() {
        instances.clear();
    }

    /**
     * Returns true if this provider has an instance for the class provided
     * More specifically, if it contains at least one dependency from which the provided class is assignable
     * This check also queries any named dependencies this provider has
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
        for (Map.Entry<String, Object> e : named.entrySet()) {
            Object val = e.getValue();
            if (val != null && klass.isAssignableFrom(val.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a named dependency exists
     *
     * @param name the name of the dependency to check for
     * @return true if the dep exists
     */
    public boolean has(String name) {
        return named.containsKey(name);
    }

    /**
     * Get a named dependency
     *
     * @param name the name of the dependency to get
     * @param <T>  the expected type of the dependency
     * @return the dependency or null if a {@link java.lang.ClassCastException} occurs or the dependency doesn't exist
     */
    public <T> T get(String name) {
        try {
            return (T) named.get(name);
        } catch (ClassCastException cce) {
            return null;
        }
    }

    /**
     * Get the instance available for the provided class
     * More specifically, the first dependency from which the provided class is assignable
     * Like {@link #has(Class)} this method returns a match from named dependencies if one is found
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
        for (Map.Entry<String, Object> e : named.entrySet()) {
            Object val = e.getValue();
            if (val != null && klass.isAssignableFrom(val.getClass())) {
                return val;
            }
        }
        return null;
    }

    /**
     * Takes all dependencies, named and unnamed from the given provider and add them to this one.
     * This does not remove dependencies from the given provider, just takes a ref to everything it holds
     *
     * @param provider the provider to take dependencies from.
     */
    public void take(DependencyProvider provider) {
        if (provider != null) {
            instances.putAll(provider.instances);
            named.putAll(provider.named);
        }
    }
}
