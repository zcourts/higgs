package com.fillta.higgs.http.client;

import com.fillta.functional.Function1;
import com.fillta.higgs.http.client.oauth.v1.OAuth1RequestBuilder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;

import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequestBuilder {

    /**
     * Only a single instance of the requester.
     * EventProcessor uses a thread pool to process requests/responses
     * so its more efficient to only use one.
     */
    public static HttpClient getRequester() {
        return HttpClient.getInstance();
    }

    private URL requestURL;
    private HttpMethod requestMethod = HttpMethod.GET;
    private Map<String, Object> urlParameters = new HashMap<String, Object>();
    /**
     * A key value pair of form fields  to send in a POST or PUT request.
     * Values can be strings (numbers get .toString() automatically) NOT FILES
     */
    private Map<String, Object> formParameters = new HashMap<String, Object>();
    /**
     * Set of files to upload
     */
    private ArrayList<HttpFile> formFiles = new ArrayList<HttpFile>();
    /**
     * Set of files to upload where 1 name/key has many files
     */
    private Map<String, List<PartialHttpFile>> formMultiFiles = new HashMap<String, List<PartialHttpFile>>();
    /**
     * Defaults to true. If set to false and files are provided in a PUT or POST
     * request then only file names will be sent.
     */
    private boolean multiPart = true;
    private Map<String, Object> requestHeaders = new HashMap<String, Object>();
    private Map<String, Object> requestCookies = new HashMap<String, Object>();
    private boolean compressionEnabled = true;
    private boolean shutdownAfter = true;
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private boolean addDefaultHeaders = true;
    private String requestContentType = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private String userAgent = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)";
    //default header values
    private String headerConnectionValue = HttpHeaders.Values.CLOSE;
    private String headerAcceptEncoding = HttpHeaders.Values.GZIP + ',' + HttpHeaders.Values.DEFLATE;
    private String headerAcceptCharset = "ISO-8859-1,utf-8;q=0.7,*;q=0.7";
    private String headerAcceptLang = "en";
    private boolean reconnectEnabled;
    //attributes used in generating the request or processing the response but aren't
    //exactly configurable as such
    private boolean useSSL;

    public HttpRequestBuilder() {
        try {
            requestURL = new URL("http://localhost/");
        } catch (MalformedURLException cause) {
            throw new RuntimeException("", cause); //will never happen
        }
        //default behaviour when dealing with Files
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }

    protected OAuth1RequestBuilder oauth1=new OAuth1RequestBuilder(this);

    public OAuth1RequestBuilder oauth1() {
        return oauth1;
    }

    public boolean isMultiPart() {
        return multiPart;
    }

    public void setMultiPart(final boolean multiPart) {
        this.multiPart = multiPart;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(final boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public boolean isShutdownAfter() {
        return shutdownAfter;
    }

    public void setShutdownAfter(final boolean shutdownAfter) {
        this.shutdownAfter = shutdownAfter;
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(final HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    public boolean isAddDefaultHeaders() {
        return addDefaultHeaders;
    }

    public void setAddDefaultHeaders(final boolean addDefaultHeaders) {
        this.addDefaultHeaders = addDefaultHeaders;
    }

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(final String requestContentType) {
        this.requestContentType = requestContentType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(final boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getHeaderConnectionValue() {
        return headerConnectionValue;
    }

    public void setHeaderConnectionValue(final String headerConnectionValue) {
        this.headerConnectionValue = headerConnectionValue;
    }

    public String getHeaderAcceptEncoding() {
        return headerAcceptEncoding;
    }

    public void setHeaderAcceptEncoding(final String headerAcceptEncoding) {
        this.headerAcceptEncoding = headerAcceptEncoding;
    }

    public String getHeaderAcceptCharset() {
        return headerAcceptCharset;
    }

    public void setHeaderAcceptCharset(final String headerAcceptCharset) {
        this.headerAcceptCharset = headerAcceptCharset;
    }

    public String getHeaderAcceptLang() {
        return headerAcceptLang;
    }

    public void setHeaderAcceptLang(final String headerAcceptLang) {
        this.headerAcceptLang = headerAcceptLang;
    }

    public Map<String, List<PartialHttpFile>> getFormMultiFiles() {
        return formMultiFiles;
    }

    public ArrayList<HttpFile> getFormFiles() {
        return formFiles;
    }

    public Map<String, Object> getFormParameters() {
        return formParameters;
    }

    public Map<String, Object> getUrlParameters() {
        return urlParameters;
    }

    public Map<String, Object> getRequestHeaders() {
        return requestHeaders;
    }

    public HttpMethod getRequestMethod() {
        return requestMethod;
    }

    public Map<String, Object> getRequestCookies() {
        return requestCookies;
    }

    /**
     * To support continual method chaining return this;
     * } allowing you to construct and send request after request
     * without having to manually create a new instance.
     * NOTE: It does not copy any of the current settings from the currently referenced instance
     * it instead returns a completely new instance as in {@code new HttpRequestBuilder()}
     *
     * @return
     */
    public HttpRequestBuilder clear() {
        return new HttpRequestBuilder();
    }

    public String path() {
        String path;
        if (url().getPath().isEmpty()) {
            path = "/";
        } else {
            path = url().getPath();
        }
        if (url().getQuery() != null && !url().getQuery().isEmpty()) {
            path += "?" + url().getQuery();
        }
        return path;
    }

    /**
     * Set/Change the URL this request is being made to
     *
     * @param url
     * @return
     */
    public HttpRequestBuilder url(URL url) {
        this.requestURL = url;
        return this;
    }

    public HttpRequestBuilder url(String url) {
        try {
            this.requestURL = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
        return this;
    }

    public URL url() {
        return requestURL;
    }

    /**
     * If true then add support for compressing/de-compressing HTTP content
     *
     * @param b
     * @return
     */
    public HttpRequestBuilder compress(boolean b) {
        this.compressionEnabled = b;
        return this;
    }

    /**
     * If true (default) then once a response is received close the connection
     *
     * @param b
     * @return
     */
    public HttpRequestBuilder shutdown(boolean b) {
        this.shutdownAfter = b;
        return this;
    }

    /**
     * HTTP Version, defaults to 1.1
     *
     * @param v
     * @return
     */
    public HttpRequestBuilder version(HttpVersion v) {
        this.httpVersion = v;
        return this;
    }

    /**
     * If true (default) then HOST,CONNECTION and userAgent headers are added automatically.
     *
     * @param b
     * @return
     */
    public HttpRequestBuilder defaultHeaders(boolean b) {
        this.addDefaultHeaders = b;
        return this;
    }

    /**
     * HTTP content type, default "text/plain"
     *
     * @param t
     * @return
     */
    public HttpRequestBuilder contentType(String t) {
        this.requestContentType = t;
        return this;
    }

    /**
     * Sets the client user agent. Default:
     * "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)"
     *
     * @param a
     * @return
     */
    public HttpRequestBuilder userAgent(String a) {
        this.userAgent = a;
        return this;
    }

    /**
     * The method to use for this request, i.e. GET,POST,PUT,DELETE,HEAD etc
     *
     * @param m
     * @return
     */
    public HttpRequestBuilder method(HttpMethod m) {
        this.requestMethod = m;
        return this;
    }

    /**
     * Add a single header
     *
     * @param name  header name
     * @param value header value
     * @return
     */
    public HttpRequestBuilder header(String name, String value) {
        this.requestHeaders.put(name, value);
        return this;
    }

    /**
     * Add all the name value pairs in the given map as headers
     *
     * @param h
     * @return
     */
    public HttpRequestBuilder headers(Map<String, Object> h) {
        this.requestHeaders.putAll(h);
        return this;
    }

    /**
     * Add a single cookie
     *
     * @param name  cookie name
     * @param value cookie value
     * @return
     */
    public HttpRequestBuilder cookie(String name, Object value) {
        this.requestCookies.put(name, value);
        return this;
    }

    /**
     * Add all the name value pairs in the given map as cookies
     *
     * @param h
     * @return
     */
    public HttpRequestBuilder cookies(Map<String, Object> h) {
        this.requestCookies.putAll(h);
        return this;
    }

    /**
     * Add a single query string parameter
     *
     * @param name  query string name/key
     * @param value value accept Any, toString() is called to allow numeric values
     * @return
     */
    public HttpRequestBuilder query(String name, Object value) {
        this.urlParameters.put(name, value);
        return this;
    }

    /**
     * Add all the name value pairs in the given map query string parameters
     * Values will have toString() called
     *
     * @param h
     * @return
     */
    public HttpRequestBuilder query(Map<String, Object> h) {
        this.urlParameters.putAll(h);
        return this;
    }

    /**
     * Explicitly set multi-part.
     * The default is true, if b is set to false and files are added then
     * the files WILL NOT BE UPLOADED, only the file names will be sent in the
     * POST or PUT request. i.e. b must be true to upload the files themselves.
     * This is the default behaviour so only use if you intend to set to false
     *
     * @param b
     * @return
     */
    public HttpRequestBuilder fileMultiPart(boolean b) {
        this.multiPart = b;
        return this;
    }

    /**
     * Add a single file parameter
     * {@link java.io.File} values will be handled properly
     *
     * @param value the file to upload
     * @return
     */
    public HttpRequestBuilder file(HttpFile value) {
        this.formFiles.add(value);
        return this;
    }

    /**
     * Add a single file parameter which has multiple files associated with it
     * {@link java.io.File} values will be handled properly.
     * Name is the key that all the {@link PartialHttpFile}s will be uploaded under
     *
     * @param name  string name/key
     * @param value the file to upload
     * @return
     */
    public HttpRequestBuilder file(String name, List<PartialHttpFile> value) {
        this.formMultiFiles.put(name, value);
        return this;
    }

    /**
     * Add a set of files to be uploaded
     * {@link java.io.File} values will be handled properly
     *
     * @param h the files to be uploaded
     * @return
     */
    public HttpRequestBuilder file(List<HttpFile> h) {
        this.formFiles.addAll(h);
        return this;
    }

    public HttpRequestBuilder fileDeleteTempFilesOnExit(boolean b) {
        DiskFileUpload.deleteOnExitTemporaryFile = b;
        DiskAttribute.deleteOnExitTemporaryFile = b;
        return this;
    }

    public HttpRequestBuilder fileBaseDirectory(String baseDir) {
        DiskFileUpload.baseDirectory = baseDir;
        DiskAttribute.baseDirectory = baseDir;
        return this;
    }

    /**
     * Add a single form parameter
     * {@link java.io.File} values will NOT be handled properly they'll have toString() called
     * use {@link #file(HttpFile)} and its related builders instead
     *
     * @param name  string name/key
     * @param value value accept Any
     * @return
     */
    public HttpRequestBuilder form(String name, Object value) {
        this.formParameters.put(name, value);
        return this;
    }

    /**
     * Add a set of form parameters
     * {@link java.io.File} values will NOT be handled properly they'll have toString() called
     * use {@link #file(HttpFile)} and its related builders instead
     *
     * @param h key value pair
     * @return
     */
    public HttpRequestBuilder form(Map<String, Object> h) {
        this.formParameters.putAll(h);
        return this;
    }

    /**
     * Turn this into an HTTP GET request
     */
    public HttpRequestBuilder GET() {
        requestMethod = HttpMethod.GET;
        return this;
    }

    /**
     * Turn this into an HTTP POST request
     */
    public HttpRequestBuilder POST() {
        requestMethod = HttpMethod.POST;
        return this;
    }

    /**
     * Turn this into an HTTP PUT request
     */
    public HttpRequestBuilder PUT() {
        requestMethod = HttpMethod.PUT;
        return this;
    }

    /**
     * Turn this into an HTTP DELETE request
     */
    public HttpRequestBuilder DELETE() {
        requestMethod = HttpMethod.DELETE;
        return this;
    }

    /**
     * Perform an HTTP request using the parameters configured in this builder.
     *
     * @param callback A function that will be invoked accepting the HTTP response
     * @tparam U
     */
    public HttpRequestBuilder build(Function1<HTTPResponse> callback) {
        if (requestMethod == HttpMethod.GET) {
            getRequester().getOrDelete(this, callback);
        } else if (requestMethod == HttpMethod.DELETE) {
            getRequester().getOrDelete(this, callback);
        } else if (requestMethod == HttpMethod.POST) {
            getRequester().postOrPut(this, callback);
//        } else if (requestMethod == HttpMethod.PUT) {
//            getRequester().postOrPut(this, callback);
        } else {
            throw new UnsupportedOperationException(String.format("HTTP method \"%s\" not supported",
                    requestMethod));
        }
        return this;
    }

    public HttpRequestBuilder basicAuth(String user, String password) {
        String auth = DatatypeConverter.printBase64Binary((user + ":" + password).getBytes(Charset.forName("UTF-8")));
        header("Authorization", "Basic " + auth);
        return this;
    }

    public boolean isReconnectEnabled() {
        return reconnectEnabled;
    }
}
