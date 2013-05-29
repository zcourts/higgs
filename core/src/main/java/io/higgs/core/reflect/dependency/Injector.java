package io.higgs.core.reflect.dependency;

import io.higgs.core.reflect.ReflectionUtil;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * A registry for {@link DependencyProvider}s that can be used to inject objects into instances and parameters
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Injector {
    private static NonBlockingHashSet<DependencyProvider> providers = new NonBlockingHashSet<>();
    private static NonBlockingHashMap<String, DependencyProvider> namedProviders = new NonBlockingHashMap<>();

    /**
     * Register an un-named dependency provider
     *
     * @param providers the providers to register
     */
    public static void register(DependencyProvider... providers) {
        for (DependencyProvider o : providers) {
            Injector.providers.add(o);
        }
    }

    /**
     * de-register an un-named provider
     *
     * @param provider the provider to remove
     * @return false if the provider was either not in the set or the parameter is null, true if it was removed
     */
    public static boolean deRegister(DependencyProvider provider) {
        return provider != null && providers.remove(provider);
    }

    /**
     * Register named dependency provider.
     * Named providers are useful for separating sets of dependencies that should only be used under
     * certain conditions
     *
     * @param name     the name by which this provider can be retrieved and used later
     * @param provider the provider to register
     * @return false if the provider param is null or the provider was already registered, true otherwise
     */
    public static boolean register(String name, DependencyProvider provider) {
        return provider != null && namedProviders.put(name, provider) == null;
    }

    /**
     * de-register a named provider
     *
     * @param name the name of the provider to remove
     * @return false if the provider was either not in the set or the parameter is null, true if it was removed
     */
    public static boolean deRegister(String name) {
        return name != null && namedProviders.remove(name) == null;
    }

    /**
     * Get a named dependency provider
     *
     * @param name the name of he provider
     * @return the provider or null if the provider isn't registered
     */
    public DependencyProvider get(String name) {
        return namedProviders.get(name);
    }

    /**
     * Given all the known providers (both named and unnamed) AND
     * given the set of expected parameters as well as the set of provided parameters
     * populate the set of expected parameters such that:
     * <p/>
     * the order of expected parameters remains unchanged
     * <p/>
     * the provided parameters if first checked for a match to each expected parameters
     * <p/>
     * the order of provided parameters must be maintained
     * <p/>
     * IF AND ONLY IF an expected parameter is not found in the provided parameters should a dependency provider be used
     * <p/>
     * where necessary, if an expected parameter is a primitive and the provided parameter is a boxed version
     * of the primitive it must be unboxed before being injected
     * <p/>
     *
     * @param accepts        the set of expected parameters
     * @param providedParams the ordered set of parameters to use
     * @param local          a local set of unordered dependencies that can be used
     * @return an array of objects injected with known parameters
     */
    public static Object[] inject(Class<?>[] accepts, Object[] providedParams, DependencyProvider local) {
        Object[] p = new Object[accepts.length];
        if (p.length > 0) {
            int incomingIdx = -1;
            for (int i = 0; i < accepts.length; i++) {
                Class<?> expectedClass = accepts[i];
                //try to inject provided parameters
                Object param = incomingIdx + 1 < providedParams.length ? providedParams[incomingIdx + 1] : null;
                if (param != null) {
                    boolean added = false;
                    if (!ReflectionUtil.isNumeric(expectedClass)) {
                        if (expectedClass.isAssignableFrom(param.getClass())) {
                            p[i] = param;    //directly assign non-numeric field
                            added = true;
                        }
                    } else {
                        //if it is a numeric field we need to check primitive types
                        //and the param type must also be numeric
                        if (ReflectionUtil.isNumeric(param.getClass())) {
                            added = assignParam(expectedClass, p, i, param);
                        }
                    }
                    if (added) {
                        //only move through provided param when one matches
                        incomingIdx += 1;
                        continue; //next
                    }
                }
                //try to inject from local dependencies
                if (local.has(expectedClass)) {
                    param = local.get(expectedClass);
                    p[i] = param;
                }
                //could not inject from provided parameters or local deps - use dependency providers
                for (DependencyProvider provider : providers) {
                    if (provider.has(expectedClass)) {
                        param = provider.get(expectedClass);
                        p[i] = param;
                        break;
                    }
                }
            }
        }
        return p;
    }

    private static boolean assignParam(Class<?> expectedClass, Object[] p, int i, Object param) {
        Class<?> paramClass = param.getClass();

        if (!ReflectionUtil.isNumeric(expectedClass)) {
            return false;
        } else {
            if (Integer.class.isAssignableFrom(paramClass)) {
                p[i] = (Integer) param;
            } else if (int.class.isAssignableFrom(expectedClass)) {
                p[i] = (int) param;
            } else if (Long.class.isAssignableFrom(expectedClass)) {
                p[i] = (Long) param;
            } else if (long.class.isAssignableFrom(expectedClass)) {
                p[i] = (long) param;
            } else if (Double.class.isAssignableFrom(expectedClass)) {
                p[i] = (Double) param;
            } else if (double.class.isAssignableFrom(expectedClass)) {
                p[i] = (double) param;
            } else if (Float.class.isAssignableFrom(expectedClass)) {
                p[i] = (Float) param;
            } else if (float.class.isAssignableFrom(expectedClass)) {
                p[i] = (float) param;
            } else if (Short.class.isAssignableFrom(expectedClass)) {
                p[i] = (Short) param;
            } else if (short.class.isAssignableFrom(expectedClass)) {
                p[i] = (short) param;
            } else if (Byte.class.isAssignableFrom(expectedClass)) {
                p[i] = (Byte) param;
            } else if (byte.class.isAssignableFrom(expectedClass)) {
                p[i] = (byte) param;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Inject fields of an object with all known un-named dependencies
     * i.e. Named dependencies are not used...
     *
     * @param instance the instance to inject
     * @param local    a local set of dependencies
     */
    public static void inject(Object instance, DependencyProvider local) {
        if (instance == null) {
            return;
        }
        Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), instance.getClass());
        for (Field field : fields) {
            try {
                field.setAccessible(true);
            } catch (SecurityException se) {
                return;
            }
            try {
                //only null fields are injected
                if (field.get(instance) != null) {
                    continue;
                }
            } catch (IllegalAccessException e) {
                return;
            }
            Class<?> fieldType = field.getType();
            Object param;
            //try to inject from local dependencies
            if (local.has(fieldType) && (param = local.get(fieldType)) != null) {
                if (setField(instance, field, param)) {
                    continue;
                }
            }
            //could not inject from local deps - use other dependency providers
            for (DependencyProvider provider : providers) {
                if (provider.has(fieldType) && (param = provider.get(fieldType)) != null) {
                    if (setField(instance, field, param)) {
                        break;
                    }
                }
            }
        }
    }

    private static boolean setField(Object instance, Field field, Object param) {
        try {
            field.set(instance, param);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
