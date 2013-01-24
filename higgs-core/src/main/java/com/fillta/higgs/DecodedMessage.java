package com.fillta.higgs;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DecodedMessage <T,M>{
	protected T topic;
	protected M message;

	public DecodedMessage(final T topic, final M message) {
		this.topic = topic;
		this.message = message;
	}

	public T getTopic() {
		return topic;
	}

	public void setTopic(final T topic) {
		this.topic = topic;
	}

	public M getMessage() {
		return message;
	}

	public void setMessage(final M message) {
		this.message = message;
	}
}
