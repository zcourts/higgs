package com.fillta.higgs.http.client;

import com.fillta.higgs.HiggsClient;
import com.fillta.higgs.HiggsClientRequest;
import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.util.Function1;
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

    public RequestProcessor() {
    }

    @Override
    public MessageTopicFactory<String, HTTPResponse> topicFactory() {
        return new MessageTopicFactory<String, HTTPResponse>() {
            @Override
            public String extract(HTTPResponse msg) {
                return msg.requestID;
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
            listen(request.id, new Function1<ChannelMessage<HTTPResponse>>() {
                @Override
                public void call(ChannelMessage<HTTPResponse> a) {
                    callback.call(a.message);
                }
            });
            connect(req.url().toString(), host, port, req.compressionEnabled, ssl,
                    new HttpClientInitializer(this, req.compressionEnabled, ssl),
                    new Function1<HiggsClientRequest<String, HttpRequest, HTTPResponse, Object>>() {
                        @Override
                        public void call(HiggsClientRequest<String, HttpRequest, HTTPResponse, Object> clientRequest) {
                            clientRequest.send(request);
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
            boolean multiPart = req.multiPart && (req.formMultiFiles.size() > 0 || req.formFiles.size() > 0);
            //create a new POST encoder, if files are to be uploaded make it a multipart form (last param)
            final HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(factory, request, multiPart);
            //add form params
            for (String name : req.formParameters.keySet()) {
                Object value = req.formParameters.get(name);
                if (value != null) {
                    encoder.addBodyAttribute(name, value.toString());
                }
            }
            //add form Files, if any
            for (HttpFile file : req.formFiles) {
                encoder.addBodyFileUpload(file.name, file.file, file.contentType, file.isText);
            }
            //add multiple files under the same name
            for (String name : req.formMultiFiles.keySet()) {
                List<PartialHttpFile> files = req.formMultiFiles.get(name);
                File[] arrFiles = new File[files.size()];
                String[] arrContentType = new String[files.size()];
                boolean[] arrIsText = new boolean[files.size()];
                for (int i = 0; i < files.size(); i++) {
                    arrFiles[i] = files.get(i).file;
                    arrContentType[i] = files.get(i).contentType;
                    arrIsText[i] = files.get(i).isText;
                }
                encoder.addBodyFileUploads(name, arrFiles, arrContentType, arrIsText);
            }
            encoder.finalizeRequest();
            listen(request.id, new Function1<ChannelMessage<HTTPResponse>>() {
                @Override
                public void call(ChannelMessage<HTTPResponse> a) {
                    callback.call(a.message);
                }
            });
            connect(req.url().toString(), host, port, req.compressionEnabled, ssl,
                    new HttpClientInitializer(this, req.compressionEnabled, ssl),
                    new Function1<HiggsClientRequest<String, HttpRequest, HTTPResponse, Object>>() {
                        @Override
                        public void call(HiggsClientRequest<String, HttpRequest, HTTPResponse, Object> clientRequest) {
                            clientRequest.send(request);
                            if (encoder.isChunked()) {
                                clientRequest.channel.write(encoder);
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
        for (String name : req.urlParameters.keySet()) {
            Object value = req.urlParameters.get(name);
            if (value != null) {
                encoder.addParam(name, value.toString());
            }
        }
        URI uriGet = new URI(encoder.toString());
        HttpRequest request = new HttpRequest(req, req.httpVersion, req.requestMethod, uriGet.toASCIIString());
        if (req.addDefaultHeaders) {
            request.setHeader(HttpHeaders.Names.HOST, req.url().getHost());
            request.setHeader(HttpHeaders.Names.CONNECTION, req.header_connection_value);
            request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, req.header_accept_encoding);
            request.setHeader(HttpHeaders.Names.ACCEPT_CHARSET, req.header_accept_charset);
            request.setHeader(HttpHeaders.Names.ACCEPT_LANGUAGE, req.header_accept_lang);
            request.setHeader(HttpHeaders.Names.REFERER, req.url().toString());
            request.setHeader(HttpHeaders.Names.USER_AGENT, req.USER_AGENT);
            request.setHeader(HttpHeaders.Names.ACCEPT, req.requestContentType);
        }
        for (String name : req.requestHeaders.keySet()) {
            Object value = req.requestHeaders.get(name);
            if (name != null) {
                request.setHeader(name, value);
            }
        }
        DefaultCookie[] cookieList = new DefaultCookie[req.requestCookies.size()];
        int i = 0;
        for (String name : req.requestCookies.keySet()) {
            Object value = req.requestCookies.get(name);
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
}