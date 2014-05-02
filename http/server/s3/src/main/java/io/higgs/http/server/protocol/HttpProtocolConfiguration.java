package io.higgs.http.server.protocol;

import io.higgs.core.HiggsServer;
import io.higgs.core.MethodProcessor;
import io.higgs.core.ProtocolConfiguration;
import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.DefaultParamInjector;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.Transcriber;
import io.higgs.http.server.auth.HiggsSecurityManager;
import io.higgs.http.server.transformers.ResponseTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Queue;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HttpProtocolConfiguration implements ProtocolConfiguration {
    protected final Queue<ResponseTransformer> transformers = new ConcurrentLinkedDeque<>();
    protected final Queue<MediaTypeDecoder> mediaTypeDecoders = new ConcurrentLinkedDeque<>();
    protected HiggsSecurityManager securityManager = new HiggsSecurityManager();
    protected HiggsServer server;
    protected ParamInjector injector = new DefaultParamInjector();
    protected Transcriber transcriber = new Transcriber();
    protected Logger log = LoggerFactory.getLogger(getClass());

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
        securityManager.init(server);
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
        if (transformers.size() == 0) {
            log.warn("No response transformers registered, this means requests will not receive response entities");
        }
    }

    public Queue<MediaTypeDecoder> getMediaTypeDecoders() {
        return mediaTypeDecoders;
    }

    public Queue<ResponseTransformer> getTransformers() {
        return transformers;
    }

    public HiggsSecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(HiggsSecurityManager securityManager) {
        if (securityManager == null) {
            throw new IllegalArgumentException("Security manager cannot be null");
        }
        this.securityManager = securityManager;
    }
}
