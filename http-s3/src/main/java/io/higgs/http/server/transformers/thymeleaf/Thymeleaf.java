package io.higgs.http.server.transformers.thymeleaf;

import io.higgs.http.server.config.TemplateConfig;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Thymeleaf {
    private TemplateEngine templateEngine = new TemplateEngine();
    private final TemplateConfig config;
    //
    private HashSet<ITemplateResolver> resolvers = new HashSet<>();
    private ClassLoaderTemplateResolver clResolver = new ClassLoaderTemplateResolver();
    private FileTemplateResolver fileResolver = new FileTemplateResolver();
    private UrlTemplateResolver urlResolver = new UrlTemplateResolver();
    private boolean ignoreConfigPrefixAndSuffix;

    public Thymeleaf(TemplateConfig config) {
        this(config, false);
    }

    public Thymeleaf(TemplateConfig config, boolean ignoreConfigPrefixAndSuffix) {
        this.config = config;
        this.ignoreConfigPrefixAndSuffix = ignoreConfigPrefixAndSuffix;
        templateEngine.setTemplateResolvers(getTemplateResolvers());
        if (config.auto_initialize_thymeleaf) {
            templateEngine.initialize();
        }
    }

    public Set<? extends ITemplateResolver> getTemplateResolvers() {
        if (!ignoreConfigPrefixAndSuffix) {
            fileResolver.setSuffix(config.suffix);
            fileResolver.setPrefix(config.prefix);
            //
            clResolver.setSuffix(config.suffix);
            clResolver.setPrefix(config.prefix);
            //
            urlResolver.setSuffix(config.suffix);
            urlResolver.setPrefix(config.prefix);
        }

        fileResolver.setTemplateMode(config.template_mode);
        urlResolver.setTemplateMode(config.template_mode);
        clResolver.setTemplateMode(config.template_mode);

        //
        clResolver.setCacheable(config.cacheable);
        clResolver.setCacheTTLMs(config.cache_age_ms);
        clResolver.setCharacterEncoding(config.character_encoding);
        clResolver.setOrder(config.classLoader_resolver_order);
        //
        fileResolver.setCacheable(config.cacheable);
        fileResolver.setCacheTTLMs(config.cache_age_ms);
        fileResolver.setCharacterEncoding(config.character_encoding);
        fileResolver.setOrder(config.fileResolver_order);
        //
        urlResolver.setCacheable(config.cacheable);
        urlResolver.setCacheTTLMs(config.cache_age_ms);
        urlResolver.setCharacterEncoding(config.character_encoding);
        urlResolver.setOrder(config.url_resolver_order);
        //
        resolvers.add(clResolver);
        resolvers.add(fileResolver);
        resolvers.add(urlResolver);
        return resolvers;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public ClassLoaderTemplateResolver getClResolver() {
        return clResolver;
    }

    public FileTemplateResolver getFileResolver() {
        return fileResolver;
    }

    public UrlTemplateResolver getUrlResolver() {
        return urlResolver;
    }
}
