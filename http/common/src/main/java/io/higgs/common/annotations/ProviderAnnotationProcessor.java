package io.higgs.common.annotations;

import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ProviderAnnotationProcessor extends HiggsAnnotationProcessor {
    @Override
    protected Set<Class<?>> types() {
        return new HashSet<Class<?>>() {
            {
                add(Provider.class);
            }
        };
    }
}
