package com.fillta.higgs.http.client;

import com.fillta.functional.Function1;
import com.fillta.higgs.*;
import com.fillta.higgs.events.ChannelMessage;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class RequestProcessor extends HiggsClient<String, HttpRequest, HTTPResponse, Object> {
	private static RequestProcessor instance;

	private RequestProcessor() {
		this(1);
	}

	private RequestProcessor(int max) {
		super.maxThreads = max;
	}


	/**
	 * Only a single instance must exist so that resources that can be shared amongst requests
	 * are used.
	 *
	 * @return
	 */
	public static RequestProcessor getInstance() {
		return getInstance(1);
	}

	/**
	 * Only a single instance must exist so that resources that can be shared amongst requests
	 * are used.
	 *
	 * @return
	 */
	public static RequestProcessor getInstance(int maxThreads) {
		if (instance == null) {
			instance = new RequestProcessor(maxThreads);
		}
		return instance;
	}

	@Override
	public MessageTopicFactory<String, HTTPResponse> topicFactory() {
		return new MessageTopicFactory<String, HTTPResponse>() {
			@Override
			public String extract(HTTPResponse msg) {
				return msg.getRequestID();
			}
		};
	}

	@Override
	public MessageConverter<HTTPResponse, HttpRequest, Object> serializer() {
		return new HttpMessageConverter();
	}


	public void getOrDelete(final HttpRequestBuilder req, final Function1<HTTPResponse> callback) {
		try {
			URI uri = req.url().toURI();

			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			String host = uri.getHost() == null ? "localhost" : uri.getHost();
			int port = uri.getPort();
			if (port == -1) {
				if ("http".equalsIgnoreCase(scheme)) {
					port = 80;
				} else if ("https".equalsIgnoreCase(scheme)) {
					port = 443;
				}
			}
			boolean ssl = "https".equalsIgnoreCase(scheme);
			final HttpRequest request = createRequest(req);
			final HiggsClientConnection<String, HttpRequest, HTTPResponse, Object>[] conn = new HiggsClientConnection[1];
			listen(request.getId(), new Function1<ChannelMessage<HTTPResponse>>() {
				@Override
				public void apply(ChannelMessage<HTTPResponse> a) {
					if (conn[0] != null) {
						conn[0].getChannel().close();
					}
					callback.apply(a.message);
				}
			});
			connect(req.url().toString(), host, port, req.isCompressionEnabled(), ssl,
					new HttpClientInitializer(this, req.isCompressionEnabled(), ssl),
					new Function1<HiggsClientConnection<String, HttpRequest, HTTPResponse, Object>>() {
						@Override
						public void apply(HiggsClientConnection<String, HttpRequest, HTTPResponse, Object> clientConnection) {
							conn[0] = clientConnection;
							clientConnection.send(request);
						}
					});
		} catch (URISyntaxException e) {
			log.warn(String.format("Couldn't make request, malformed URI encountered: %s", req.url()), e);
		}
	}

	public void postOrPut(HttpRequestBuilder req, final Function1<HTTPResponse> callback) {
		try {
			URI uri = req.url().toURI();

			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			String host = uri.getHost() == null ? "localhost" : uri.getHost();
			int port = uri.getPort();
			if (port == -1) {
				if ("http".equalsIgnoreCase(scheme)) {
					port = 80;
				} else if ("https".equalsIgnoreCase(scheme)) {
					port = 443;
				}
			}
			boolean ssl = "https".equalsIgnoreCase(scheme);
			final HttpRequest request = createRequest(req);
			// setup the factory: here using a mixed memory/disk based on size threshold
			HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
			//if user explicitly sets multi-part to false then only file name is sent otherwise
			//if at least 1 file is supplied it is multi-part
			boolean multiPart = req.isMultiPart() && (req.getFormMultiFiles().size() > 0 || req.getFormFiles().size() > 0);
			//create a new POST encoder, if files are to be uploaded make it a multipart form (last param)
			final HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(factory, request, multiPart);
			//add form params
			for (String name : req.getFormParameters().keySet()) {
				Object value = req.getFormParameters().get(name);
				if (value != null) {
					encoder.addBodyAttribute(name, value.toString());
				}
			}
			//add form Files, if any
			for (HttpFile file : req.getFormFiles()) {
				encoder.addBodyFileUpload(file.getName(), file.getFile(), file.getContentType(), file.isText());
			}
			//add multiple files under the same name
			for (String name : req.getFormMultiFiles().keySet()) {
				List<PartialHttpFile> files = req.getFormMultiFiles().get(name);
				File[] arrFiles = new File[files.size()];
				String[] arrContentType = new String[files.size()];
				boolean[] arrIsText = new boolean[files.size()];
				for (int i = 0; i < files.size(); i++) {
					arrFiles[i] = files.get(i).getFile();
					arrContentType[i] = files.get(i).getContentType();
					arrIsText[i] = files.get(i).isText();
				}
				encoder.addBodyFileUploads(name, arrFiles, arrContentType, arrIsText);
			}
			encoder.finalizeRequest();
			listen(request.getId(), new Function1<ChannelMessage<HTTPResponse>>() {
				public void apply(ChannelMessage<HTTPResponse> a) {
					callback.apply(a.message);
				}
			});
			connect(req.url().toString(), host, port, req.isCompressionEnabled(), ssl,
					new HttpClientInitializer(this, req.isCompressionEnabled(), ssl),
					new Function1<HiggsClientConnection<String, HttpRequest, HTTPResponse, Object>>() {
						public void apply(HiggsClientConnection<String, HttpRequest, HTTPResponse, Object> clientConnection) {
							clientConnection.send(request);
							if (encoder.isChunked()) {
								clientConnection.getChannel().write(encoder);
							}
							encoder.cleanFiles();
						}
					});
		} catch (URISyntaxException e) {
			log.warn(String.format("Couldn't make request, malformed URI encountered: %s", req.url()), e);
		} catch (HttpPostRequestEncoder.ErrorDataEncoderException e) {
			log.warn(String.format("Unable to make POST request an encoding error occurred :%s", req.url()), e);
		}
	}

	/**
	 * Create an {@link HttpRequest} with {@link HttpHeaders} including (cookies, user agent etc),
	 * and query string parameters set
	 *
	 * @param req
	 * @return
	 * @tparam U
	 */
	public HttpRequest createRequest(HttpRequestBuilder req) throws URISyntaxException {
		QueryStringEncoder encoder = new QueryStringEncoder(req.path());
		for (String name : req.getUrlParameters().keySet()) {
			Object value = req.getUrlParameters().get(name);
			if (value != null) {
				encoder.addParam(name, value.toString());
			}
		}
		URI uriGet = new URI(encoder.toString());
		HttpRequest request = new HttpRequest(req, req.getHttpVersion(), req.getRequestMethod(), uriGet.toASCIIString());
		if (req.isAddDefaultHeaders()) {
			request.setHeader(HttpHeaders.Names.HOST, req.url().getHost());
			request.setHeader(HttpHeaders.Names.CONNECTION, req.getHeaderConnectionValue());
			request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, req.getHeaderAcceptEncoding());
			request.setHeader(HttpHeaders.Names.ACCEPT_CHARSET, req.getHeaderAcceptCharset());
			request.setHeader(HttpHeaders.Names.ACCEPT_LANGUAGE, req.getHeaderAcceptLang());
			request.setHeader(HttpHeaders.Names.REFERER, req.url().toString());
			request.setHeader(HttpHeaders.Names.USER_AGENT, req.getUserAgent());
			request.setHeader(HttpHeaders.Names.ACCEPT, req.getRequestContentType());
		}
		for (String name : req.getRequestHeaders().keySet()) {
			Object value = req.getRequestHeaders().get(name);
			if (name != null) {
				request.setHeader(name, value);
			}
		}
		DefaultCookie[] cookieList = new DefaultCookie[req.getRequestCookies().size()];
		int i = 0;
		for (String name : req.getRequestCookies().keySet()) {
			Object value = req.getRequestCookies().get(name);
			if (value != null) {
				cookieList[i++] = new DefaultCookie(name, value.toString());
			}
		}
		//set cookies
		request.setHeader(HttpHeaders.Names.COOKIE,
				//we can safely cast an array list of default cookies to Iterable<Cookie>
				ClientCookieEncoder.encode(cookieList));
		return request;
	}

	@Override
	protected <H extends HiggsClientConnection<String, HttpRequest, HTTPResponse, Object>> H newClientRequest(HiggsClient<String, HttpRequest, HTTPResponse, Object> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
		H request = (H) new HiggsClientConnection<>(client, serviceName, host, port, decompress, useSSL, initializer);
		request.setAutoReconnect(false);
		return request;
	}

	@Override
	public <H extends HiggsInitializer<HTTPResponse, HttpRequest>> H newInitializer(final boolean inflate, final boolean deflate, final boolean ssl) {
		return (H) new HttpClientInitializer<>(this, deflate, ssl);
	}
}