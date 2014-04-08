package io.higgs.http.server.transformers.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import io.higgs.core.FileUtil;
import io.higgs.core.ResolvedFile;
import io.higgs.http.server.config.MustacheConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsMustacheFactory extends DefaultMustacheFactory implements MustacheFactory {
    protected MustacheConfig config;
    protected Path base;

    public HiggsMustacheFactory(MustacheConfig config) {
        this.config = config;
        base = Paths.get(config.directory);
    }

    @Override
    public Reader getReader(String resourceName) {
        ResolvedFile file = FileUtil.resolve(base, Paths.get(resourceName));
        if (!file.exists()) {
            throw new MustacheException(resourceName + " not found");
        }
        return new BufferedReader(new InputStreamReader(file.getStream(), Charsets.UTF_8));
    }
}
