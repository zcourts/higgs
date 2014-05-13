package io.higgs.boson.serialization.v1;

import io.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.higgs.boson.BosonType.MAP;
import static io.higgs.boson.BosonType.STRING;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonWriterTest {
    @Test
    public void testSerializeCircularReference() throws Exception {
        CircularReferenceB[] b = new CircularReferenceB[1];
        for (int i = 0; i < b.length; i++) {
            b[i] = new CircularReferenceB();
            b[i].init();
        }
        BosonMessage original = new BosonMessage(b, "test", "callback");
        BosonWriter writer = new BosonWriter();
        ByteBuf obj = writer.serialize(original);
        BosonReader reader = new BosonReader();
        BosonMessage msg = new BosonMessage();
        reader.deSerialize(obj, msg);
        //first verify what we serialize
        assertTrue("At least 1 instance required", original.arguments.length > 0);
        Object arg = original.arguments[0];
        assertTrue("Must be instance of CircularReferenceB", arg instanceof CircularReferenceB);
        CircularReferenceB b1 = (CircularReferenceB) arg;
        assertNotNull("CircularReferenceA not initialized", b1.a);
        CircularReferenceA a1 = b1.a;
        assertNotNull("CircularReferenceB not initialized", a1.b);
        assertTrue("CircularReferenceB and CircularReferenceA are not equal", a1.b == b1);
        //verify what we de-serialize
        assertTrue("At least 1 instance required", msg.arguments.length > 0);
        Object arg1 = msg.arguments[0];
        assertTrue("Must be instance of CircularReferenceB", arg1 instanceof CircularReferenceB);
        CircularReferenceB b2 = (CircularReferenceB) arg;
        assertNotNull("CircularReferenceA not initialized", b2.a);
        CircularReferenceA a2 = b2.a;
        assertNotNull("CircularReferenceB not initialized", a2.b);
        assertTrue("CircularReferenceB and CircularReferenceA are not equal", a2.b == b2);
    }

    @Test
    public void testSerialize() throws Exception {

    }

    @Test
    public void testWriteByte() throws Exception {

    }

    @Test
    public void testWriteNull() throws Exception {

    }

    @Test
    public void testWriteShort() throws Exception {

    }

    @Test
    public void testWriteInt() throws Exception {

    }

    @Test
    public void testWriteLong() throws Exception {

    }

    @Test
    public void testWriteFloat() throws Exception {

    }

    @Test
    public void testWriteDouble() throws Exception {

    }

    @Test
    public void testWriteBoolean() throws Exception {

    }

    @Test
    public void testWriteChar() throws Exception {

    }

    @Test
    public void testWriteString() throws Exception {

    }

    @Test
    public void testWriteList() throws Exception {

    }

    @Test
    public void testWriteArray() throws Exception {

    }

    @Test
    public void testWriteMap() throws Exception {
        BosonWriter writer = new BosonWriter();
        String intStr = "int";
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map intMap = new HashMap<>();
        Map longMap = new HashMap<>();
        Map stringMap = new HashMap<>();
        //
        map.put("int", intMap);
        map.put("long", longMap);
        map.put("string", stringMap);
        //
        intMap.put("a", 1);
        intMap.put("b", 2);
        longMap.put("a", 1L);
        longMap.put("b", 2L);
        longMap.put("c", 3L);
        stringMap.put("a", "a");
        stringMap.put("b", "ab");
        stringMap.put("c", "abc");
        ByteBuf buf = Unpooled.buffer();
        writer.writeMap(buf, map);
        assertEquals(MAP, buf.readByte());
        assertEquals(3, buf.readInt());    //we added 3 items (int,long and string)
        assertEquals(STRING, buf.readByte()); //"int" type
        assertEquals(intStr.getBytes().length, buf.readInt());
        byte[] tmp = new byte[intStr.getBytes().length];
        buf.readBytes(tmp);
        assertEquals(intStr, new String(tmp));
        assertEquals(MAP, buf.readByte()); //value = intMap
        assertEquals(2, buf.readInt());  //two ints added
        assertEquals(STRING, buf.readByte()); //first int  a -> 1
        assertEquals(1, buf.readInt()); //size
        //todo to be continued until entire nested map is deconstructed...
    }

    @Test
    public void testWriteReadMap() throws Exception {
        String intStr = "int";
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map intMap = new HashMap<>();
        Map longMap = new HashMap<>();
        Map stringMap = new HashMap<>();
        //
        map.put("int", intMap);
        map.put("long", longMap);
        map.put("string", stringMap);
        //
        intMap.put("a", 1);
        intMap.put("b", 2);
        longMap.put("a", 1L);
        longMap.put("b", 2L);
        longMap.put("c", 3L);
        stringMap.put("a", "a");
        stringMap.put("b", "ab");
        stringMap.put("c", "abc");
        BosonMessage msg = new BosonMessage(new Object[]{map}, "test");
        BosonWriter writer = new BosonWriter();
        BosonReader r = new BosonReader();
        BosonMessage m = new BosonMessage();
        ByteBuf buf = writer.serialize(msg);
        r.deSerialize(buf, m);
        assertEquals(map, m.arguments[0]);
    }

    @Test
    public void testWritePolo() throws Exception {

    }

    @Test
    public void testGetArrayComponent() throws Exception {

    }

    @Test
    public void testSerializingEnum() throws Exception {
        BosonWriter writer = new BosonWriter();
        BosonReader reader = new BosonReader();
        EnumEnclosingType obj = new EnumEnclosingType(SomeType.B);
        ByteBuf out = writer.serialize(obj);
        EnumEnclosingType in = reader.deSerialize(out);
        assertNotNull(in);
        assertEquals(obj.value, in.value);
    }

    @Test
    public void testSerializingEnumInNestedPOLO() throws Exception {
        BosonWriter writer = new BosonWriter();
        BosonReader reader = new BosonReader();
        OuterEnclosingType obj = new OuterEnclosingType();
        ByteBuf out = writer.serialize(obj);
        OuterEnclosingType in = reader.deSerialize(out);
        assertNotNull(in);
        assertEquals(obj.type.value, in.type.value);
    }

    public static class EnumEnclosingType {
        private SomeType value;

        EnumEnclosingType(SomeType value) {
            this.value = value;
        }

        EnumEnclosingType() {
        }
    }

    public static class OuterEnclosingType {
        private EnumEnclosingType type = new EnumEnclosingType(SomeType.C);
    }

    enum SomeType {
        A, B, C
    }
}
