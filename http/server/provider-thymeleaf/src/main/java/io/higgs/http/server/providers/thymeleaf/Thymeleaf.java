package io.higgs.http.server.providers.thymeleaf;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.config.TemplateConfig;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.UrlTemplateResolver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    public String getFullTemplate(String suggestedName, String[] fragements) {
        String name = config.fragments_dir;
        if (suggestedName == null || suggestedName.isEmpty()) {
            for (String a : fragements) {
                name += "_" + a;
            }
        } else {
            name += suggestedName;
        }
        String fullPath = config.prefix + name + config.suffix;
        File file = HiggsServer.BASE_PATH.resolve(fullPath).toFile();

        if (!file.exists() || config.merge_fragments_on_each_request) {
            try {
                if (file.exists() && !file.delete()) {
                    throw new IllegalStateException(String.format("Unable to delete template file '%s'",
                            file.getAbsolutePath()));
                }
                File parent = file.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException(String.format("Unable to create directory structure for fragments" +
                                    " '%s'",
                            file.getAbsolutePath()
                    ));
                }
                if (!file.createNewFile()) {
                    throw new IllegalStateException(String.format("Unable to create template file from fragments '%s'",
                            file.getAbsolutePath()));
                }
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
                for (String frag : fragements) {
                    String fragmentPath = config.prefix + frag + config.suffix;
                    BufferedReader stream = new BufferedReader(new FileReader(HiggsServer.BASE_PATH.resolve
                            (fragmentPath).toFile()));
                    String line;
                    while ((line = stream.readLine()) != null) {
                        out.append(line);
                    }
                    out.flush();
                    stream.close();
                }
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(String.format("Couldn't build template file from fragments - %s",
                        file.getAbsolutePath()), e);
            }
        }
        return name;
    }
}
