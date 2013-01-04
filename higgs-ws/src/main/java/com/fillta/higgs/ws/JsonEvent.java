package com.fillta.higgs.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple web socket event with a topic and message property.
 * topic is the method to be invoked on either the client or server
 * message is a JSON string object
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonEvent implements Event<String, Object> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private Object message;
	private String topic = "";

	public JsonEvent(String topic, Object obj) {
		this.topic = topic;
		message = obj;
	}

	public JsonEvent() {
	}

	public JsonEvent(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object data) {
		message = data;
	}

	public String toString() {
		return "TextEvent{" +
				"message='" + message + '\'' +
				", topic='" + topic + '\'' +
				'}';
	}
}
