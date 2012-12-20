package com.fillta.higgs.boson.demo;

import com.fillta.higgs.boson.serialization.BosonProperty;

import java.lang.reflect.Field;

public class PoloExample {
	public PoloExample() {
	}

	public PoloExample(int j) {
		i = j;
	}

	public static boolean dont = true;
	public static boolean stillDont = false;
	@BosonProperty
	int i = 0;
	String name = "Test non-annotated field";
	private String str = "Test private non-annotated field";
	@BosonProperty(ignore = true)
	String ignored = null;
	Nested nested = new Nested();

	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("[");
		for (Field field : getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Object value = null;
			try {
				value = field.get(this);
			} catch (IllegalAccessException e) {
				//bleh!
			}
			if (field.getType().isArray()) {
				buf.append(field.getName()).append(" = ");
				for (Object v : (Object[]) value) {
					buf.append(v).append(",");
				}
			} else {
				buf.append(field.getName()).append(" = ").append(value);
			}
			buf.append(",\n");
		}
		buf.append("]");
		return buf.toString();
	}

}