package io.higgs.core;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ConfigUtil {
    protected ConfigUtil() {
    }

    public static <T> T loadYaml(String configFile, Class<T> klass) {
        return loadYaml(Paths.get(configFile), klass);
    }

    public static <T> T loadYaml(Path configFile, Class<T> klass) {
        return loadYaml(configFile, klass, null);
    }

    /**
     * Given a path, load the file at its location as a YAML file and convert it to an instance of the given class
     *
     * @param configFile the path to the config file
     * @param klass      the class of which to return an instance
     * @return an instance of the class with it's fields initialized to whatever is in the config
     */
    public static <T> T loadYaml(Path configFile, Class<T> klass, Constructor constructor) {
        T config;
        Yaml yaml;
        if (constructor != null) {
            yaml = new Yaml(constructor);
        } else {
            yaml = new Yaml();
        }
        ResolvedFile in = FileUtil.resolve(configFile);
        if (!in.hasStream()) {
            throw new ResourceNotFoundException("Unable to find config " + configFile);
        }
        try {
            config = yaml.loadAs(in.getStream(), klass);
        } catch (Throwable e) {
            throw new IllegalStateException(String.format("Can't load config (%s)", configFile.toAbsolutePath()), e);
        }
        return config;
    }
}
