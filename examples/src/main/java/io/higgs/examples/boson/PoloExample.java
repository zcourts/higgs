package io.higgs.examples.boson;

import io.higgs.boson.serialization.BosonProperty;

import java.lang.reflect.Field;

public class PoloExample {
    public static boolean dont = true;
    public static boolean stillDont;
    @BosonProperty
    int i;
    String name = "Test non-annotated field";
    @BosonProperty(ignore = true)
    String ignored;
    Nested nested = new Nested();
    private String str = "Test private non-annotated field";

    public PoloExample() {
    }

    public PoloExample(int j) {
        i = j;
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
