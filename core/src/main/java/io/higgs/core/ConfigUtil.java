package io.higgs.core;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ConfigUtil {
    protected ConfigUtil() {
    }

    public static <T> T loadYaml(String configFile, Class<T> klass) {
        T config;
        Yaml yaml = new Yaml();
        Path configPath = Paths.get(configFile).toAbsolutePath();
        try {
            config = yaml.loadAs(new FileInputStream(configPath.toFile()), klass);
        } catch (Throwable e) {
            throw new IllegalStateException(String.format("Unable to load config (%s)", configPath), e);
        }
        return config;
    }
}
