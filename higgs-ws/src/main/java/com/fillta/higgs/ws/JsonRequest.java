package com.fillta.higgs.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
public class JsonRequest {
	@JsonIgnore
	protected final Logger log = LoggerFactory.getLogger(getClass());
	@JsonProperty
	protected JsonNode message;
	@JsonProperty
	protected String topic = "";
	@JsonProperty
	protected String client_callback = "";

	public JsonRequest(String topic, JsonNode obj) {
		this.topic = topic;
		message = obj;
	}

	public JsonRequest() {
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public JsonNode getMessage() {
		return message;
	}

	public void setMessage(JsonNode data) {
		message = data;
	}

	public String getClient_callback() {
		return client_callback;
	}

	public void setClient_callback(final String client_callback) {
		this.client_callback = client_callback;
	}

	/**
	 * Get the Message this JSON event represents as the given type or null
	 * if klass is null or conversion fails
	 *
	 * @param klass
	 * @param <T>
	 * @return
	 */
	public <T> T as(Class<T> klass) {
		if (klass == null)
			return null;
		try {
			return WebSocketServer.mapper.readValue(message.traverse(), klass);
		} catch (IOException e) {
			log.warn(String.format("Unable to decode message to type. Type : %s\n Message string : %s",
					klass.getName(), message.toString()), e);
			return null;
		}
	}

	public String toString() {
		return "JsonEvent{" +
				"\nTopic='" + topic + '\'' +
				"\nClient callback='" + client_callback + '\'' +
				",\nMessage='" + message + '\'' +
				"\n}";
	}
}
