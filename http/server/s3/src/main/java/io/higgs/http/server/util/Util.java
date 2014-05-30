package io.higgs.http.server.util;

import io.higgs.http.server.providers.ProviderContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Util {
    private static Logger log = LoggerFactory.getLogger(Util.class);

    public static <T> Set<T> getServices(Class<T> klass, ContextResolver resolver) {
        return getServices(klass, new HashSet<>(Arrays.asList(new ProviderContainer<>(resolver))));
    }

    public static <T> Set<T> getServices(Class<T> klass, Set<ProviderContainer<ContextResolver>> resolvers) {
        Iterator<T> providers = HiggsServiceLoader.load(klass, resolvers).iterator();
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
