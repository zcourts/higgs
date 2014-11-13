package io.higgs.core.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ReflectionUtil {
    public static int MAX_RECURSION_DEPTH = 10;

    private ReflectionUtil() {
    }

    public static Set<Field> getAllFields(Set<Field> fields, Class<?> type) {
        return getAllFields(fields, type, 0);
    }

    public static Set<Field> getAllFields(Set<Field> fields, Class<?> type, int depth) {
        //first get inherited fields
        if (type.getSuperclass() != null && depth <= MAX_RECURSION_DEPTH) {
            getAllFields(fields, type.getSuperclass(), ++depth);
        }
        //now add all "local" fields
        Collections.addAll(fields, type.getDeclaredFields());
        return fields;
    }

    public static Method[] getAllMethods(Class<?> klass) {
        HashSet<Method> methods = new HashSet<>();
        getAllMethods(methods, klass);
        return methods.toArray(new Method[methods.size()]);
    }

    public static Set<Method> getAllMethods(Set<Method> methods, Class<?> type) {
        return getAllMethods(methods, type, 0);
    }

    public static Set<Method> getAllMethods(Set<Method> methods, Class<?> type, int depth) {
        if (type.getSuperclass() != null && depth <= MAX_RECURSION_DEPTH) {
            getAllMethods(methods, type.getSuperclass(), ++depth);
        }
        Collections.addAll(methods, type.getDeclaredMethods());
        return methods;
    }

    /**
     * @param klass
     * @return true if klass represents a numeric type, including byte. Both boxed and unboxed.
     */
    public static boolean isNumeric(Class<?> klass) {
        return Integer.class.isAssignableFrom(klass) ||
                int.class.isAssignableFrom(klass) ||
                Long.class.isAssignableFrom(klass) ||
                long.class.isAssignableFrom(klass) ||
                Double.class.isAssignableFrom(klass) ||
                double.class.isAssignableFrom(klass) ||
                Float.class.isAssignableFrom(klass) ||
                float.class.isAssignableFrom(klass) ||
                Short.class.isAssignableFrom(klass) ||
                Short.class.isAssignableFrom(klass) ||
                Byte.class.isAssignableFrom(klass) ||
                byte.class.isAssignableFrom(klass);
    }
}
