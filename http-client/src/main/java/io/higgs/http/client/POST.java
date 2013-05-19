package io.higgs.http.client;

import io.higgs.http.client.future.Reader;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class POST extends Request {
    protected long minSize = DefaultHttpDataFactory.MINSIZE;
    private final List<HttpFile> files = new ArrayList<>();
    private HttpPostRequestEncoder encoder;
    private Map<String, Object> form = new HashMap<>();
    private HttpDataFactory factory;

    public POST(EventLoopGroup group, URI uri, HttpVersion version, Reader f) {
        super(group, uri, HttpMethod.POST, version, f);
    }

    @Override
    protected void configure() throws Exception {
        super.configure();
        boolean multipart = files.size() > 0;
        newEncoder(multipart);
        addFormParams();
        if (multipart) {
            addFileParams();
        }
        // finalize request
        request = encoder.finalizeRequest();
    }

    private void addFileParams() throws HttpPostRequestEncoder.ErrorDataEncoderException {
        for (HttpFile file : files) {
            if (file.isSingle()) {
                encoder.addBodyFileUpload(file.name(), file.file(), file.contentType(), file.isText());
            } else {
                encoder.addBodyFileUploads(file.name(), file.fileSet(), file.contentTypes(), file.isTextSet());
            }
        }
    }

    private void addFormParams() throws HttpPostRequestEncoder.ErrorDataEncoderException {
        // add Form attribute
        for (Map.Entry<String, Object> e : form.entrySet()) {
            encoder.addBodyAttribute(e.getKey(), e.getValue() == null ? "" : e.getKey());
        }
    }

    protected ChannelFuture makeTheRequest() {
        return !encoder.isChunked() ? super.makeTheRequest() : doRequest();
    }

    private ChannelFuture doRequest() {
        channel.write(request);
        return channel.write(encoder).awaitUninterruptibly()
                .addListener(new GenericFutureListener<Future<Void>>() {
                    @Override
                    public void operationComplete(Future<Void> future) throws Exception {
                        encoder.cleanFiles();
                    }
                });
    }

    /**
     * Create a new factory and post request encoder
     *
     * @param multipart if true then a multipart encoder is created
     * @throws HttpPostRequestEncoder.ErrorDataEncoderException
     *
     */
    protected void newEncoder(boolean multipart) throws HttpPostRequestEncoder.ErrorDataEncoderException {
        factory = new DefaultHttpDataFactory(minSize);
        // Use the PostBody encoder
        encoder = new HttpPostRequestEncoder(factory, request, multipart);
    }

    public long getMinSize() {
        return minSize;
    }

    public POST setMinSize(long minSize) {
        this.minSize = minSize;
        return this;
    }

    public List<HttpFile> getFiles() {
        return files;
    }

    public HttpPostRequestEncoder getEncoder() {
        return encoder;
    }

    public Map<String, Object> getForm() {
        return form;
    }

    public HttpDataFactory getFactory() {
        return factory;
    }

    public POST form(String name, Object value) {
        form.put(name, value);
        return this;
    }

    public POST file(HttpFile file) {
        if (file != null) {
            files.add(file);
        }
        return this;
    }

}
