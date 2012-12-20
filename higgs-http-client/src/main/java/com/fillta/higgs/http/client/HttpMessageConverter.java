package com.fillta.higgs.http.client;

import com.fillta.higgs.MessageConverter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunk;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpMessageConverter implements MessageConverter<HTTPResponse, HttpRequest, Object> {
	public final static AttributeKey<Boolean> attChunks = new AttributeKey("reading-chunks");
	public final static AttributeKey<String> attTopic = new AttributeKey("topic");
	public final static AttributeKey<HTTPResponse> attResponse = new AttributeKey("response");
	protected Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public Object serialize(Channel channel, HttpRequest msg) {
		channel.attr(attTopic).set(msg.getId());
		return msg;
	}

	@Override
	public HTTPResponse deserialize(ChannelHandlerContext ctx, Object msg) {
		Attribute<Boolean> chunksAtt = ctx.channel().attr(attChunks);
		//if not set then its false
		chunksAtt.compareAndSet(null, false);
		boolean readingChunks = chunksAtt.get();
		Attribute<HTTPResponse> resAttr = ctx.channel().attr(attResponse);
		resAttr.compareAndSet(null, new HTTPResponse());
		HTTPResponse response = resAttr.get();
		Attribute<String> topicAtt = ctx.channel().attr(attTopic);
		topicAtt.compareAndSet(null, "");
		String requestID = topicAtt.get();
		response.setRequestID(requestID);
		if (!readingChunks) {
			HttpResponse res = (HttpResponse) msg;
			//get headers
			if (res.getHeaderNames().size() > 0) {
				//get header names
				for (String name : res.getHeaderNames()) {
					//get header values
					for (String value : res.getHeaders(name)) {
						ArrayList<String> values = response.getHeaders().get(name);
						if (values == null) {
							values = new ArrayList();
							response.getHeaders().put(name, values);
						}
						values.add(value);
					}
				}
			}
			response.setStatus(res.getStatus());
			response.setProtocolVersion(res.getProtocolVersion());
			response.setTransferEncoding(res.getTransferEncoding());
			if (res.getTransferEncoding().isMultiple()) {
				//mark as reading chunks
				ctx.channel().attr(attChunks).set(true);
				if (res instanceof HttpChunk && ((HttpChunk) res).isLast()) {
					HttpChunk chunk = ((HttpChunk) res);
					//last chunk
					ctx.channel().attr(attChunks).set(false);
					response.append(chunk.getContent().toString(CharsetUtil.UTF_8));
					//clean up
					cleanUp(ctx);
					//fire message received
					return response;
				} else {
					if (res instanceof HttpChunk) {
						HttpChunk chunk = ((HttpChunk) res);
						response.append(chunk.getContent().toString(CharsetUtil.UTF_8));
						return null;//more to come
					} else if (res instanceof DefaultHttpResponse) {
						DefaultHttpResponse responseChunk = (DefaultHttpResponse) res;
						response.append(responseChunk.getContent().toString(CharsetUtil.UTF_8));
						return null;//more to come
					} else {
						log.warn(String.format("Unknown state encountered while de-serializing an Http message. Netty chunk marked as multiple but is not an instance of HttpChunk type = %s", res.getClass().getName()));
						return null;//more to come
					}
				}
			} else {
				ByteBuf content = res.getContent();
				if (content.readable()) {
					//fire message received
					response.append(content.toString(CharsetUtil.UTF_8));
					cleanUp(ctx);
					return response;
				} else {
					log.warn(String.format("Response received but is not readable. \nID:%s \ncontent:%s",
							requestID, content.toString(CharsetUtil.UTF_8)));
					return null;//more to come
				}
			}
		} else {
			HttpChunk chunk = (HttpChunk) msg;
			if (chunk.isLast()) {
				ctx.channel().attr(attChunks).set(false);
				response.append(chunk.getContent().toString(CharsetUtil.UTF_8));
				cleanUp(ctx);
				//fire message received
				return response;
			} else {
				//save chunk
				response.append(chunk.getContent().toString(CharsetUtil.UTF_8));
				return null;//more to come
			}
		}
	}

	private void cleanUp(final ChannelHandlerContext ctx) {
		ctx.channel().attr(attChunks).remove();
		ctx.channel().attr(attTopic).remove();
		ctx.channel().attr(attResponse).remove();
	}
}
