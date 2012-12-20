package com.fillta.higgs.reflect;

import java.lang.reflect.Field;
import java.util.List;

public class ReflectionUtil {
	public ReflectionUtil() {
	}

	public List<Field> getAllFields(List<Field> fields, Class<?> type, int depth) {
		for (Field field : type.getDeclaredFields()) {
			fields.add(field);
		}
		if (type.getSuperclass() != null && depth <= BosonWriter.MAX_RECURSION_DEPTH) {
			fields = getAllFields(fields, type.getSuperclass(), ++depth);
		}
		return fields;
	}
}