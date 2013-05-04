package io.higgs.http.server.protocol;

import io.higgs.core.HiggsServer;
import io.higgs.core.MethodProcessor;
import io.higgs.core.ProtocolConfiguration;
import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.DefaultParamInjector;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.Transcriber;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.params.HttpSession;
import io.higgs.http.server.transformers.HttpErrorTransformer;
import io.higgs.http.server.transformers.JsonTransformer;
import io.higgs.http.server.transformers.ResponseTransformer;
import io.higgs.http.server.transformers.StaticFileTransformer;
import io.higgs.http.server.transformers.ThymeleafTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HttpProtocolConfiguration implements ProtocolConfiguration {
    private HiggsServer<HttpConfig> server;
    private ParamInjector injector = new DefaultParamInjector();
    private final Map<String, HttpSession> sessions = new HashMap<>();
    private final Queue<ResponseTransformer> transformers = new ConcurrentLinkedDeque<>();
    private final Queue<ResponseTransformer> errorTransformers = new ConcurrentLinkedDeque<>();
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

    public HiggsServer<HttpConfig> getServer() {
        return server;
    }

    public void setServer(HiggsServer<HttpConfig> server) {
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
        if (server.getConfig().add_static_resource_filter) {
            transformers.add(new StaticFileTransformer(this));
        }
        if (server.getConfig().add_default_error_transformer) {
            errorTransformers.add(new HttpErrorTransformer(this,
                    new JsonTransformer(),
                    new ThymeleafTransformer(this.server.getConfig().template_config)));
        }
        if (server.getConfig().add_json_transformer) {
            transformers.add(new JsonTransformer());
        }
        if (server.getConfig().add_thymeleaf_transformer) {
            transformers.add(new ThymeleafTransformer(this.server.getConfig().template_config));
        }
    }

    public Queue<ResponseTransformer> getTransformers() {
        return transformers;
    }

    public Queue<ResponseTransformer> getErrorTransformers() {
        return errorTransformers;
    }
}
