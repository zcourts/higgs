package com.fillta.higgs.ws;

import com.fillta.higgs.method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface JsonResponseEvent {
	/**
	 * Un-marshall a JsonEvent's message into the given type returning null on failure or if klass is null
	 *
	 * @param klass
	 * @param <T>
	 * @return
	 */
	public <T> T as(Class<T> klass);

	/**
	 * Get the name of the client callback
	 *
	 * @return
	 */
	public String getClient_callback();

	/**
	 * Set the name of the server callback to be invoked
	 *
	 * @param topic
	 */
	public void setTopic(String topic);

	/**
	 * The callback function that will receive responses...if any
	 *
	 * @param callback
	 */
	public void setClient_callback(String callback);

	/**
	 * @return The name registered with {@link method} or a plain string.
	 */
	public String getTopic();
}
