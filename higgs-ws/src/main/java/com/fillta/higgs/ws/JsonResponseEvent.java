package com.fillta.higgs.ws;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface JsonResponseEvent {
	/**
	 * Get the name of the client callback
	 *
	 * @return
	 */
	public String getCallback();

	/**
	 * The callback function that will receive responses...if any
	 *
	 * @param callback
	 */
	public void setCallback(String callback);

	public Object getMessage();

	public void setMessage(Object o);

}
