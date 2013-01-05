package com.fillta.higgs.sniffing;

import com.fillta.functional.res.Function1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * A protocol detector allows a server to create dynamic pipelines. Automatically adding
 * SSL,GZip, HTTP and/or custom protocol handlers.
 * By default {@link ProtocolSniffer} adds SSL, GZip and HTTP decoders,encoders and any other
 * handler required to handle those types.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class ProtocolDetector implements Function1<Boolean, ByteBuf> {
	/**
	 * Detects a protocol from the given buffer.
	 * If SSL or GZip is required they would have already been applied.
	 *
	 * @param a
	 * @return
	 */
	public abstract Boolean apply(final ByteBuf a);

	/**
	 * If the {@link #apply(ByteBuf)} function returns true, this method will be invoked.
	 * On invocation, the method should add its custom handlers to the pipeline.
	 *
	 * @param ctx The channel context whose pipeline should be modified
	 * @return true if the protocol sniffer should remove itself from the pipeline after invoking this.
	 */
	public abstract boolean setupPipeline(final ChannelHandlerContext ctx);

	/**
	 * At least 2 bytes is required to detect an HTTP or GZipped request. 5 bytes are required
	 * to detect if SSL is enabled. This imposes a minimum limit of 5 bytes to do detection.
	 * However, this is a minimum and custom protocols can require as many bytes as needed to be
	 * detected. Just bare the 5 byte minimum in mind.
	 * This means the {@link ByteBuf} passed to the {@link #apply(ByteBuf)} method will always
	 * have a minimum of 5 readable bytes. If this method returns a number greater than 5 then
	 * the byte buf will always have a minimum of that amount.
	 *
	 * @return The number of bytes required to detect the protocol this detector is for
	 */
	public abstract int bytesRequired();
}
