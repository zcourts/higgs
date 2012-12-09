package com.fillta.higgs;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MessageConverter<IM, OM, SM> {
	/**
	 * Convert a message from its outgoing message "OM" format
	 * into its serialized message "SM" format
	 *
	 * @param ctx
	 * @param msg the message to be converted
	 * @return
	 */
	public SM serialize(Channel ctx, OM msg);

	/**
	 * Convert a message from its serialized form into the incoming message, "IM" form.
	 *
	 * @param ctx The Netty channel context
	 * @param msg the serialized message
	 * @return The converted message  OR null if the message is not complete and
	 *         should not be queued to pass to listeners yet
	 */
	public IM deserialize(ChannelHandlerContext ctx, SM msg);
}
