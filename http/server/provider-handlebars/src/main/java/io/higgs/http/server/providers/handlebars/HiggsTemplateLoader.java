package io.higgs.http.server.providers.handlebars;

import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import io.higgs.core.FileUtil;
import io.higgs.core.ResolvedFile;
import io.higgs.http.server.config.HandlebarsConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsTemplateLoader implements TemplateLoader {
    protected HandlebarsConfig config;
    protected Path base;

    public HiggsTemplateLoader(HandlebarsConfig config) {
        this.config = config;
        base = Paths.get(config.directory);
    }

    @Override
    public TemplateSource sourceAt(String template) throws IOException {
        String ext = config.template;
        if (!template.endsWith(ext) || !template.contains(".")) {
            template = template + ext;
        }
        final ResolvedFile file = FileUtil.resolve(base, Paths.get(template));
        if (!file.exists()) {
            throw new IOException(template + " not found");
        }
        return new HiggsTemplateSource(file);
    }

    @Override
    public String resolve(String s) {
        return s;
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    public String getSuffix() {
        return config.template;
    }
}
