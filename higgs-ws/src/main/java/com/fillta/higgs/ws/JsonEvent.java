package com.fillta.higgs.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A simple web socket event with a topic and message property.
 * topic is the method to be invoked on either the client or server
 * message is a JSON string object
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonEvent implements Event<String, ByteBuf> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final static ObjectMapper mapper = new ObjectMapper();
	private ByteBuf message;
	private String topic = "";

	public JsonEvent(final String topic, ByteBuf obj) {
		this.topic = topic;
		message = obj;
	}

	public JsonEvent() {
	}

	public JsonEvent(final String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(final String topic) {
		this.topic = topic;
	}

	public ByteBuf getMessage() {
		return message;
	}

	public void setMessage(ByteBuf data) {
		message = data;
	}

	public ByteBuf serialize() {
		try {
			return Unpooled.wrappedBuffer(mapper.writeValueAsBytes(this));
		} catch (JsonProcessingException e) {
			log.warn(String.format("Failed to serialize text event %s", toString()));
			return Unpooled.buffer();
		}
	}

	public <T> T from(Class<T> type) {
		try {
			byte[] data = new byte[message.writerIndex()];
			message.readBytes(data);
			return mapper.readValue(data, type);
		} catch (IOException e) {
			log.warn(String.format("Unable to de-serialize message from {%s}", type.getName()));
			return null;
		}
	}

	public String toString() {
		return "TextEvent{" +
				"message='" + message + '\'' +
				", topic='" + topic + '\'' +
				'}';
	}
}
