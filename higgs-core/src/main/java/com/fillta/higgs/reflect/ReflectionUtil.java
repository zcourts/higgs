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
}