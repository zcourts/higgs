package io.higgs.http.server.transformers.moustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.higgs.core.ConfigUtil;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.config.MoustacheConfig;
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

import static io.higgs.http.server.resource.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static io.higgs.http.server.resource.MediaType.APPLICATION_XHTML_XML_TYPE;
import static io.higgs.http.server.resource.MediaType.TEXT_HTML_TYPE;
import static io.higgs.http.server.resource.MediaType.WILDCARD_TYPE;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@ProviderFor(ResponseTransformer.class)
public class Moustache extends BaseTransformer {
    protected MoustacheConfig config;

    public Moustache() {
        config = ConfigUtil.loadYaml("moustache_config.yml", MoustacheConfig.class);
        setPriority(config.priority);
        addSupportedTypes(WILDCARD_TYPE, TEXT_HTML_TYPE, APPLICATION_FORM_URLENCODED_TYPE, APPLICATION_XHTML_XML_TYPE);
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
    public void transform(Object response, HttpRequest request, HttpResponse httpResponse, MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {

    }

    @Override
    public ResponseTransformer instance() {
        return this; //not stateful so we can return this safely
    }
}
