package com.fillta.higgs.http.server.files;

import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticClasspathFileResolver {
    private static final StaticClassPathCache cache = new StaticClassPathCache(".*(?<!\\.class)$");

    protected StaticClasspathFileResolver() {
    }

    public static byte[] load(String path) {
        if (cache.contains(path)) {
            return cache.load(path);
        }
        return null;
    }

    public static void main(String... args) {
        StaticClasspathFileResolver r = new StaticClasspathFileResolver();
        Map<String, CachedPath> c = r.cache.get();
        for (String key : c.keySet()) {
            System.out.println(c.get(key).getFilename());
        }
    }
}
