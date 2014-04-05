package io.higgs.http.server.protocol;

import io.higgs.core.HiggsServer;
import io.higgs.core.MethodProcessor;
import io.higgs.core.ProtocolConfiguration;
import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.DefaultParamInjector;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.ResponseTransformer;
import io.higgs.http.server.Transcriber;
import io.higgs.http.server.params.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HttpProtocolConfiguration implements ProtocolConfiguration {
    private final Map<String, HttpSession> sessions = new HashMap<>();
    private final Queue<ResponseTransformer> transformers = new ConcurrentLinkedDeque<>();
    private final Queue<ResponseTransformer> errorTransformers = new ConcurrentLinkedDeque<>();
    private final Queue<MediaTypeDecoder> mediaTypeDecoders = new ConcurrentLinkedDeque<>();
    private HiggsServer server;
    private ParamInjector injector = new DefaultParamInjector();
    private Transcriber transcriber = new Transcriber();

    public Map<String, HttpSession> getSessions() {
        return sessions;
    }

    public Transcriber getTranscriber() {
        return transcriber;
    }

    public void setTranscriber(Transcriber transcriber) {
        if (transcriber == null) {
            throw new IllegalArgumentException("Null transcriber");
        }
        this.transcriber = transcriber;
    }

    public HiggsServer getServer() {
        return server;
    }

    public void setServer(HiggsServer server) {
        this.server = server;
    }

    public ParamInjector getInjector() {
        return injector;
    }

    public void setInjector(ParamInjector injector) {
        this.injector = injector;
    }

    @Override
    public ProtocolDetectorFactory getProtocol() {
        return new HttpDetectorFactory(this);
    }

    @Override
    public MethodProcessor getMethodProcessor() {
        return new HttpMethodProcessor(this);
    }

    @Override
    public void initialise(HiggsServer server) {
        this.server = server;
        ServiceLoader<ResponseTransformer> transformerServiceLoader = ServiceLoader.load(ResponseTransformer.class);

    }

    public Queue<MediaTypeDecoder> getMediaTypeDecoders() {
        return mediaTypeDecoders;
    }

    public Queue<ResponseTransformer> getTransformers() {
        return transformers;
    }

    public Queue<ResponseTransformer> getErrorTransformers() {
        return errorTransformers;
    }
}
