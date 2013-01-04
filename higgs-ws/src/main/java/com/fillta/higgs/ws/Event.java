package com.fillta.higgs.ws;

import io.netty.buffer.ByteBuf;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Event<T, M> {
	public T getTopic();

	public void setTopic(T topic);

	public M getMessage();

	public void setMessage(M data);

//	public ByteBuf serialize();
//
//	public <T> T from(Class<T> type);
}
