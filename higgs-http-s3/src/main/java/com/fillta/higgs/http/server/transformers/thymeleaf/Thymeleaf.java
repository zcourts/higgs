package com.fillta.higgs.http.server.transformers.thymeleaf;

import com.fillta.higgs.http.server.config.TemplateConfig;
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

	public Thymeleaf(TemplateConfig config) {
		this.config = config;
		templateEngine.setTemplateResolvers(getTemplateResolvers());
		if (config.auto_initialize_thymeleaf)
			templateEngine.initialize();
	}

	public Set<? extends ITemplateResolver> getTemplateResolvers() {
		HashSet<ITemplateResolver> resolvers = new HashSet<>();
		ClassLoaderTemplateResolver clResolver = new ClassLoaderTemplateResolver();
		FileTemplateResolver fileResolver = new FileTemplateResolver();
		UrlTemplateResolver urlResolver = new UrlTemplateResolver();
		//
		clResolver.setCacheable(config.cacheable);
		clResolver.setCacheTTLMs(config.cache_age_ms);
		clResolver.setCharacterEncoding(config.character_encoding);
		clResolver.setSuffix(config.suffix);
		clResolver.setPrefix(config.prefix);
		clResolver.setOrder(config.classLoader_resolver_order);
		//
		fileResolver.setCacheable(config.cacheable);
		fileResolver.setCacheTTLMs(config.cache_age_ms);
		fileResolver.setCharacterEncoding(config.character_encoding);
		fileResolver.setSuffix(config.suffix);
		fileResolver.setPrefix(config.prefix);
		fileResolver.setOrder(config.fileResolver_order);
		//
		urlResolver.setCacheable(config.cacheable);
		urlResolver.setCacheTTLMs(config.cache_age_ms);
		urlResolver.setCharacterEncoding(config.character_encoding);
		urlResolver.setSuffix(config.suffix);
		urlResolver.setPrefix(config.prefix);
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
}
