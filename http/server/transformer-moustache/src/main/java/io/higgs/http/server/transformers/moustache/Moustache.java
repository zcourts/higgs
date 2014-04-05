package io.higgs.http.server.transformers.moustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.transformers.BaseTransformer;
import io.higgs.http.server.transformers.ResponseTransformer;
import io.higgs.spi.ProviderFor;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@ProviderFor(ResponseTransformer.class)
public class Moustache extends BaseTransformer {
    public Moustache() {
    }

    static class Feature {
        Feature(String description) {
            this.description = description;
        }

        String description;
    }

    public static void main(String[] args) throws IOException {
        HashMap<String, Object> scopes = new HashMap<String, Object>();
        scopes.put("name", "Mustache");
        scopes.put("feature", new Feature("Perfect!"));

        Writer writer = new OutputStreamWriter(System.out);
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new StringReader("{{name}}, {{feature.description}}!"), "example");
        mustache = mf.compile("templates/index.moustache");
        mustache = mf.compile("templates/index.moustache");
        mustache.execute(writer, scopes);
        writer.flush();
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {
        //first and foremost an endpoint must have a template annotation to even be considered
        if (!method.hasTemplate()) {
            return false;
        }
        if (request.getAcceptedMediaTypes().isEmpty()) {
            return true; //assume */*
        }
        for (MediaType type : request.getAcceptedMediaTypes()) {
            if (type.isCompatible(MediaType.WILDCARD_TYPE) ||
                    type.isCompatible(MediaType.TEXT_HTML_TYPE) ||
                    type.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE) ||
                    type.isCompatible(MediaType.APPLICATION_XHTML_XML_TYPE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse httpResponse, MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {

    }

    @Override
    public ResponseTransformer instance() {
        return this; //not stateful so we can return this safely
    }

    @Override
    public int priority() {
        return 1;
    }
}
