package com.fillta.higgs.boson.serialization.v1;

import com.fillta.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

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
        BosonWriter writer = new BosonWriter(original);
        ByteBuf obj = writer.serialize();
        BosonReader reader = new BosonReader(obj);
        BosonMessage msg = reader.deSerialize();
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

    }

    @Test
    public void testWritePolo() throws Exception {

    }

    @Test
    public void testGetArrayComponent() throws Exception {

    }

    @Test
    public void testValidateAndWriteType() throws Exception {

    }
}
