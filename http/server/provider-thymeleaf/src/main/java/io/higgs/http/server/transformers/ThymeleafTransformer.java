package io.higgs.http.server.transformers;

import io.higgs.core.ConfigUtil;
import io.higgs.core.reflect.ReflectionUtil;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.config.TemplateConfig;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.providers.BaseTransformer;
import io.higgs.http.server.providers.ResponseTransformer;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.transformers.thymeleaf.Thymeleaf;
import io.higgs.http.server.transformers.thymeleaf.WebContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.higgs.http.server.resource.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static io.higgs.http.server.resource.MediaType.APPLICATION_XHTML_XML_TYPE;
import static io.higgs.http.server.resource.MediaType.TEXT_HTML_TYPE;
import static io.higgs.http.server.resource.MediaType.WILDCARD_TYPE;

/**
 * See {@link io.higgs.http.server.resource.template#value()} for a list of types that will be injected by default
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
//@ProviderFor(ResponseTransformer.class)
@MetaInfServices(ResponseTransformer.class)
public class ThymeleafTransformer extends BaseTransformer {
    protected TemplateConfig config;
    protected Thymeleaf tl;
    private Logger log = LoggerFactory.getLogger(getClass());

    public ThymeleafTransformer() {
        this.config = ConfigUtil.loadYaml("thymeleaf_config.yml", TemplateConfig.class);
        tl = new Thymeleaf(this.config);
        setPriority(config.priority);
        addSupportedTypes(WILDCARD_TYPE, TEXT_HTML_TYPE, APPLICATION_FORM_URLENCODED_TYPE, APPLICATION_XHTML_XML_TYPE);
    }

    @Override
    public ThymeleafTransformer instance() {
        return new ThymeleafTransformer();
    }

    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method, ChannelHandlerContext ctx) {
        WebContext webContext = new WebContext();
        String[] fragements = method.getFragments();
        String template = method.getTemplate();
        if (fragements.length > 0) {
            template = tl.getFullTemplate(template, fragements);
        }

        byte[] data = null;
        try {
            if (request != null) {
                if (config.determine_language_from_accept_header) {
                    try {
                        webContext.setLocale(Locale.forLanguageTag(
                                request.headers().get(HttpHeaders.Names.ACCEPT_LANGUAGE)));
                    } catch (Throwable t) {
                        log.warn("Unable to set locale from accept header");
                    }
                }
                populateContext(webContext, response, request, method);
            }
            if (isError(response)) {
                template = determineErrorTemplate(res, response);
            }
            populateContext(webContext, response, request, method);
            String content = tl.getTemplateEngine().process(template, webContext);
            data = content.getBytes(Charset.forName(config.character_encoding));
        } catch (Throwable e) {
            log.warn("Unable to transform response to HTML using Thymeleaf transformer", e);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        setResponseContent(res, data);
    }

    protected String determineErrorTemplate(HttpResponse res, Object response) {
        Throwable err = response instanceof Throwable ? (Throwable) response : null;
        determineErrorStatus(res, err);
        String tpl = "error/default";
        if (response instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) response;
            tpl = e.getTemplate() == null || e.getTemplate().isEmpty() ? tpl : e.getTemplate();
        }
        return tpl;
    }

    private void populateContext(final WebContext ctx, Object response, HttpRequest request, HttpMethod method) {
        //set defaults first so that users can override
        //${_query} ,${_form},${_files},${_session},${_cookies},${_request},${_response},${_server}
        ctx.setVariable("_query", request.getQueryParams());
        ctx.setVariable("_form", request.getFormParam());
        ctx.setVariable("_files", request.getFormFiles());
        ctx.setVariable("_subject", request.getSubject());
        ctx.setVariable("_session", request.getSubject().getSession());
        ctx.setVariable("_cookies", request.getCookies());
        ctx.setVariable("_request", request);
        ctx.setVariable("_response", response);
        if (method != null) {
            ctx.setVariable("_validation", method.getValidationResult());
        }
        //ctx.setVariable("_server", server);
        //response already available under ${_response} so only include if is POJO or Map, then we can
        //do a field to value setup
        if (response instanceof Map || //only Map is allowed from the set of Collections
                (response != null && !ReflectionUtil.isNumeric(response.getClass()) && !(response instanceof
                        Collection))) {

            if (response instanceof Map && config.convert_map_responses_to_key_value_pairs) {
                ctx.setVariables((Map<String, ?>) response);
            } else {
                //it must be a POJO otherwise (since its not a primitive or a Map,List or Set...)
                if (config.convert_pojo_responses_to_key_value_pairs) {
                    //get fields going a max of 10 parent classes up in the chain
                    Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), response.getClass(), 10);
                    for (Field field : fields) {
                        try {
                            field.setAccessible(true);
                            ctx.setVariable(field.getName(), field.get(response));
                        } catch (IllegalAccessException e) {
                            log.warn(String.format("Unable to set template variable %s", field.getName()), e);
                        }
                    }
                }
            }
        }
    }

    public TemplateConfig getConfig() {
        return config;
    }

    /**
     * Get the Thymeleaf template engine which can be used configured further.
     *
     * @return
     */
    public TemplateEngine getTemplateEngine() {
        return tl.getTemplateEngine();
    }
}
