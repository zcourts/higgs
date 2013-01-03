package com.fillta.higgs.http.server;

import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.http.server.params.HttpCookie;
import com.fillta.higgs.http.server.params.HttpFile;
import com.fillta.higgs.http.server.params.HttpSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpChunk;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpConverter implements MessageConverter<HiggsHttpRequest, HiggsHttpResponse, Object> {
	public final static AttributeKey<Boolean> attChunks = new AttributeKey("reading-chunks");
	public final static AttributeKey<HttpPostRequestDecoder> attDecoder = new AttributeKey("files-decoder");
	public final static AttributeKey<HiggsHttpRequest> attRequest = new AttributeKey("channel-request");
	private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk
	private Logger log = LoggerFactory.getLogger(getClass());
	private final HiggsHttpServer server;

	public HttpConverter(final HiggsHttpServer server) {
		this.server = server;
	}

	public HiggsHttpResponse serialize(final Channel ctx, final HiggsHttpResponse msg) {
		return msg;
	}

	/**
	 * Deserialize an http request. if its a post or put request the de serialization can happen
	 * accross multiple instances of the http converter. to maintain state, data is associated with
	 * the channel which initiated the request.  Once all files and form parameters are received
	 * this method will return a fully de-serialized http request. Until then it will return null.
	 *
	 * @param ctx The Netty channel context
	 * @param msg the serialized message
	 * @return
	 */
	public HiggsHttpRequest deserialize(final ChannelHandlerContext ctx, final Object msg) {
		//in large post requests only the first object will be of type HttpRequest, others may be chunks
		if (msg instanceof HttpRequest) {
			//since we have the request see if we had one on the channel already, if not init session
			HttpRequest req = (HttpRequest) msg;
			Attribute<HiggsHttpRequest> requestAttribute = ctx.channel().attr(attRequest);
			HiggsHttpRequest request = requestAttribute.get();
			if (request == null) {
				//associate a request with the channel if not already done
				request = initHiggsRequest((HttpRequest) msg);
				requestAttribute.set(request);
			}
			if (!HttpMethod.POST.getName().equalsIgnoreCase(req.getMethod().getName()) &&
					!HttpMethod.PUT.getName().equalsIgnoreCase(req.getMethod().getName())) {
				//only post and put requests  are allowed to send form data so everything else just returns
				return getAndCleanRequest(requestAttribute, request);
			} else {
				//if its a post or put request and a decoder doesn't exist then create one.
				Attribute<HttpPostRequestDecoder> decoderAttribute = ctx.channel().attr(attDecoder);
				if (decoderAttribute.get() == null) {
					try {
						HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, req);
						decoderAttribute.set(decoder);
					} catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
						log.warn("Unable to decode data", e1);
						return null;
					} catch (HttpPostRequestDecoder.IncompatibleDataDecoderException e) {
						log.warn("Incompatible request type", e);
						return null;
					}
				}
				//decoder is created if it doesn't exist, can decode all if entire message received
				Attribute<Boolean> chunksAtt = ctx.channel().attr(attChunks);
				//if not set then its false
				chunksAtt.compareAndSet(null, false);
				boolean readingChunks = chunksAtt.get();
				if (!readingChunks) {
					if (req.getTransferEncoding().isMultiple()) {
						//reading chunks... won't come here again
						ctx.channel().attr(attChunks).set(true);
					} else {
						// Not chunk version
						return readAllHttpDataReceived(ctx.channel());
					}
				}
			}
		} else {
			//reading chunks, channel would have had decoder created already
			HttpPostRequestDecoder decoder = ctx.channel().attr(attDecoder).get();
			// New chunk is received
			HttpChunk chunk = (HttpChunk) msg;
			try {
				decoder.offer(chunk);
			} catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
				log.warn("Unable to decode HTTP chunk", e1);
				return null;  //TODO throw WebApplicationException to return error to client and stop
			}
			//reading chunk by chunk (minimize memory usage due to use of Factory)
			//readHttpDataChunkByChunk(ctx.channel());
			// reading all only if at the end, until we have all data do nothing
			if (chunk.isLast()) {
				//no longer reading chunks
				ctx.channel().attr(attChunks).set(false);
				return readAllHttpDataReceived(ctx.channel());
			}
		}
		//not done receiving post or put request so return null
		return null;
	}

	private HiggsHttpRequest readAllHttpDataReceived(final Channel channel) {
		//entire message/request received
		HttpPostRequestDecoder decoder = channel.attr(attDecoder).get();
		List<InterfaceHttpData> data;
		try {
			data = decoder.getBodyHttpDatas();
		} catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e1) {
			log.warn("Not enough data to decode", e1);
			return null;
		}
		//called when all data is received, go over request data and separate form fields from files
		Attribute<HiggsHttpRequest> requestAttribute = channel.attr(attRequest);
		HiggsHttpRequest request = requestAttribute.get();
		for (InterfaceHttpData httpData : data) {
			//check if is file upload or attribute, attributes go into form params and file uploads to file params
			if (httpData instanceof io.netty.handler.codec.http.multipart.Attribute) {
				io.netty.handler.codec.http.multipart.Attribute field =
						(io.netty.handler.codec.http.multipart.Attribute) httpData;
				try {
					//add form param
					request.addFormField(field.getName(), field.getValue());
				} catch (IOException e) {
					log.warn(String.format("unable to extract form field's value, field name = %s", field.getName()));
				}
			} else if (httpData instanceof FileUpload) {
				//add form file
				request.addFormFile(new HttpFile((FileUpload) httpData));
			} else {
				if (httpData != null) {
					log.warn(String.format("Unknown form type encountered Class: %s,data type:%s,name:%",
							httpData.getClass().getName(), httpData.getHttpDataType().name(), httpData.getName()));
				}
			}
		}
		return getAndCleanRequest(requestAttribute, request);
	}

	/**
	 * sets the given attribute to null and return the given request
	 *
	 * @param attribute
	 * @param request
	 * @return
	 */
	private HiggsHttpRequest getAndCleanRequest(final Attribute<HiggsHttpRequest> attribute, final HiggsHttpRequest request) {
		attribute.set(null);
		return request;
	}

	private HiggsHttpRequest initHiggsRequest(final HttpRequest req) {
		//convert Netty HttpRequest to a HiggsHttpRequest via copy constructor
		HiggsHttpRequest request = new HiggsHttpRequest(req);
		//if the user has no session available then set one
		if (!request.hasSessionID()) {
			SecureRandom random = new SecureRandom();
			String id = new BigInteger(130, random).toString(32);
			HttpCookie session = new HttpCookie(HiggsHttpServer.SID, id);
			session.setPath(server.getConfig().session_path);
			session.setMaxAge(server.getConfig().session_max_age);
			session.setHttpOnly(server.getConfig().session_http_only);
			if (server.getConfig().session_domain != null && !server.getConfig().session_domain.isEmpty())
				session.setDomain(server.getConfig().session_domain);
			String sp = server.getConfig().session_ports;
			if (sp != null && !sp.isEmpty()) {
				String[] ps = sp.split(",");
				List<Integer> ports = new ArrayList<>(ps.length);
				for (int i = 0; i < ps.length; i++) {
					String p = ps[i];
					try {
						ports.add(parseInt(p));
					} catch (NumberFormatException nfe) {
					}
				}
				session.setPorts(ports);
			}
			request.setCookie(session);
			request.setNewSession(true);
			server.getSessions().put(id, new HttpSession());
		}
		return request;
	}
}
