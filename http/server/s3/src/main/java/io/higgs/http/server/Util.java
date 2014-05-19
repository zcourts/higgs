package io.higgs.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Util {
    private static Logger log = LoggerFactory.getLogger(Util.class);

    public static <T> Set<T> getServices(Class<T> klass) {
        Iterator<T> providers = ServiceLoader.load(klass).iterator();
        HashSet<T> services = new HashSet<>();
        while (providers.hasNext()) {
            try {
                services.add(providers.next());
            } catch (ServiceConfigurationError sce) {
                log.warn("Unable to register Service", sce);
            }
        }
        return services;
    }

}
