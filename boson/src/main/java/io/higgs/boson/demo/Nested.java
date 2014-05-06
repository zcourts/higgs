package io.higgs.boson.demo;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;


public class Nested {
    NestedField[] array = new NestedField[]{ new NestedField(), new NestedField(), new NestedField() };
    List list = Arrays.asList("a", "b", "c", "d");

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
