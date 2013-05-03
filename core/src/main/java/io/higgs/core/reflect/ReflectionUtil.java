package io.higgs.core.reflect;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

public class ReflectionUtil {
    private final int MAX_RECURSION_DEPTH;

    public ReflectionUtil() {
        this(10);
    }

    public ReflectionUtil(int maxRecursionDepth) {
        MAX_RECURSION_DEPTH = maxRecursionDepth;
    }

    public Set<Field> getAllFields(Set<Field> fields, Class<?> type, int depth) {
        //first get inherited fields
        if (type.getSuperclass() != null && depth <= MAX_RECURSION_DEPTH) {
            Set<Field> superFields = getAllFields(fields, type.getSuperclass(), ++depth);
            for (Field field : superFields) {
                if (!fields.contains(field)) {
                    fields.add(field);
                }
            }
        }
        //now add all "local" fields
        Collections.addAll(fields, type.getDeclaredFields());
        return fields;
    }

    /**
     * @param klass
     * @return true if klass represents a numeric type, including byte. Both boxed and unboxed.
     */
    public boolean isNumeric(Class<?> klass) {
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
