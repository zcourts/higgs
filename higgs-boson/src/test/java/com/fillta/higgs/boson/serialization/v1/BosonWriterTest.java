package com.fillta.higgs.boson.serialization.v1;

import com.fillta.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

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
		BosonWriter writer = new BosonWriter(new BosonMessage(b, "test", "callback"));
		ByteBuf obj = writer.serialize();
		BosonReader reader = new BosonReader(obj);
		BosonMessage msg = reader.deSerialize();
		System.out.println(msg.arguments);
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
