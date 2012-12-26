package com.fillta.higgs.boson.serialization.v1;

import com.fillta.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonWriterTest {
	private class CircularReferenceA extends BosonMessage {
		CircularReferenceB b;

		public void init() {
			b = new CircularReferenceB();
			b.init();
		}

		public String toString() {
			return hashCode() + "-A";
		}
	}

	private class CircularReferenceB extends BosonMessage {
		CircularReferenceA a;

		public void init() {
			a = new CircularReferenceA();
		}

		public String toString() {
			return hashCode() + "-B";
		}
	}

	@Test
	public void testSerializeCircularReference() throws Exception {
		CircularReferenceA[] a = new CircularReferenceA[1];
		for (int i = 0; i < a.length; i++) {
			a[i] = new CircularReferenceA();
			a[i].init();
		}
		BosonWriter writer = new BosonWriter(new BosonMessage(a, "test", "callback"));
		ByteBuf obj = writer.serialize();
		BosonReader reader = new BosonReader(obj);
		BosonMessage msg = reader.deSerialize();
//		System.out.println(msg.arguments);
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
