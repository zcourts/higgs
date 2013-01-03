package com.fillta.higgs.reflect;

import java.lang.reflect.Field;
import java.util.List;

public class ReflectionUtil {
	private final int MAX_RECURSION_DEPTH;

	public ReflectionUtil() {
		this(10);
	}

	public ReflectionUtil(int maxRecursionDepth) {
		MAX_RECURSION_DEPTH = maxRecursionDepth;
	}

	public List<Field> getAllFields(List<Field> fields, Class<?> type, int depth) {
		for (Field field : type.getDeclaredFields()) {
			fields.add(field);
		}
		if (type.getSuperclass() != null && depth <= MAX_RECURSION_DEPTH) {
			fields = getAllFields(fields, type.getSuperclass(), ++depth);
		}
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