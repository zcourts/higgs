package io.higgs.core.reflect;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

public class ReflectionUtil {
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
