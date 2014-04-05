package io.higgs.http.server.protocol;

import io.higgs.core.HiggsServer;
import io.higgs.core.MethodProcessor;
import io.higgs.core.ProtocolConfiguration;
import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.DefaultParamInjector;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.transformers.ResponseTransformer;
import io.higgs.http.server.Transcriber;
import io.higgs.http.server.params.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HttpProtocolConfiguration implements ProtocolConfiguration {
    private final Map<String, HttpSession> sessions = new HashMap<>();
    private final Queue<ResponseTransformer> transformers = new ConcurrentLinkedDeque<>();
    private final Queue<MediaTypeDecoder> mediaTypeDecoders = new ConcurrentLinkedDeque<>();
    private HiggsServer server;
    private ParamInjector injector = new DefaultParamInjector();
    private Transcriber transcriber = new Transcriber();
    private Logger log = LoggerFactory.getLogger(getClass());

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
        Iterator<ResponseTransformer> providers = ServiceLoader.load(ResponseTransformer.class).iterator();
        while (providers.hasNext()) {
            try {
                ResponseTransformer transformer = providers.next();
                transformers.add(transformer);
            } catch (ServiceConfigurationError sce) {
                log.warn("Unable to register a transformer. Please ensure it implements the interface correctly" +
                        " and has a public, no-arg constructor", sce);
            }
        }
    }

    public Queue<MediaTypeDecoder> getMediaTypeDecoders() {
        return mediaTypeDecoders;
    }

    public Queue<ResponseTransformer> getTransformers() {
        return transformers;
    }
}
