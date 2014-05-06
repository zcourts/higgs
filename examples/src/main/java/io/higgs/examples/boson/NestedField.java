package io.higgs.examples.boson;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class NestedField {
    int a;
    long b;
    double c;
    float d;
    Map map = new HashMap();

    public NestedField() {
        map.put("a", 2);
        map.put(1, "123");
    }

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
