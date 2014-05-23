package io.higgs.http.server.providers.mustache;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.higgs.core.ConfigUtil;
import io.higgs.core.reflect.ReflectionUtil;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.config.MustacheConfig;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.providers.entity.BaseEntityProvider;
import io.higgs.http.server.providers.ResponseTransformer;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;

import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.higgs.http.server.resource.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static io.higgs.http.server.resource.MediaType.APPLICATION_XHTML_XML_TYPE;
import static io.higgs.http.server.resource.MediaType.TEXT_HTML_TYPE;
import static io.higgs.http.server.resource.MediaType.WILDCARD_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.EXPECTATION_FAILED;
import static io.netty.handler.codec.http.HttpResponseStatus.FAILED_DEPENDENCY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public class MustacheEntityProvider extends BaseEntityProvider {
    protected MustacheConfig config;
    protected MustacheFactory mf;

    public MustacheEntityProvider() {
        config = ConfigUtil.loadYaml("mustache_config.yml", MustacheConfig.class);
        setPriority(config.priority);
        addSupportedTypes(WILDCARD_TYPE, TEXT_HTML_TYPE, APPLICATION_FORM_URLENCODED_TYPE, APPLICATION_XHTML_XML_TYPE);
        mf = new HiggsMustacheFactory(config);
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method, ChannelHandlerContext ctx) {
        if (isError(response)) {
            determineErrorStatus(res, (Throwable) response);
            return;
        }
        if (method == null) {
            //should only ever happen if isError is true which means we should never get here
            throw new WebApplicationException(EXPECTATION_FAILED, request);
        }
        if (!method.hasTemplate()) {
            WebApplicationException e = new WebApplicationException(FAILED_DEPENDENCY, request, method);
            e.setMessage("MustacheTransformer only supports a template value, to use fragments use mustacheTransformer's inheritance");
            throw e;
        }
        ByteBuf buf = ctx.alloc().heapBuffer();
        OutputStream in = new ByteBufOutputStream(buf);
        Writer writer = new OutputStreamWriter(in);
        Mustache mustache = mf.compile(resoleTemplateName(method.getTemplate()));
        mustache.execute(writer, scopes(response, request, method));
        try {
            //flush data to byte buf
            writer.flush();
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            setResponseContent(res, data);
        } catch (IOException e) {
            log.warn("Failed to write the results of a mustacheTransformer execution", e);
            res.setStatus(INTERNAL_SERVER_ERROR);
            setResponseContent(res, new byte[0]);
        }
    }

    private String resoleTemplateName(String template) {
        String ext = config.template;
        if (template.endsWith(ext) || template.contains(".")) {
            return template;
        }
        return template + ext;
    }

    private Object[] scopes(Object response, HttpRequest request, HttpMethod method) {
        Set<Map<String, Object>> scopes = new HashSet<>();
        Map<String, Object> global = new HashMap<>();
        scopes.add(global);

        //${_query} ,${_form},${_files},${_session},${_cookies},${_request},${_response},${_server}
        global.put("_query", request.getQueryParams());
        global.put("_form", request.getFormParam());
        global.put("_files", request.getFormFiles());
        global.put("_subject", request.getSubject());
        global.put("_session", request.getSubject().getSession());
        global.put("_cookies", request.getCookies());
        global.put("_request", request);
        global.put("_response", response);
        if (method != null) {
            global.put("_validation", method.getValidationResult());
        }
        //scopes.put("_server", server);
        //response already available under ${_response} so only include if is POJO or Map, then we can
        //do a field to value setup
        if (response instanceof Map || //only Map is allowed from the set of Collections
                (response != null && !ReflectionUtil.isNumeric(response.getClass()) && !(response instanceof
                        Collection))) {

            if (response instanceof Map && config.extract_values_from_maps) {
                scopes.add((Map) response);
            } else {
                //it must be a POJO otherwise (since its not a primitive or a Map,List or Set...)
                if (config.extract_pojo_fields) {
                    //get fields going a max of 10 parent classes up in the chain
                    Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), response.getClass(), 10);
                    Map<String, Object> variables = new HashMap<>();
                    scopes.add(variables);
                    for (Field field : fields) {
                        try {
                            field.setAccessible(true);
                            variables.put(field.getName(), field.get(response));
                        } catch (IllegalAccessException e) {
                            log.warn(String.format("Unable to set template variable %s", field.getName()), e);
                        }
                    }
                }
            }
        }
        return scopes.toArray();
    }

    @Override
    public ResponseTransformer instance() {
        return new MustacheEntityProvider();
    }
}
