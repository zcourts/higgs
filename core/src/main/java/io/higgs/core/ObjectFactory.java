package io.higgs.core;

import java.util.Set;

/**
 * An object factories is responsible for creating instances of various objects.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class ObjectFactory {
    private final Set<ProtocolConfiguration> configurations;
    private final HiggsServer server;

    public ObjectFactory(HiggsServer server, Set<ProtocolConfiguration> configurations) {
        this.server = server;
        this.configurations = configurations;
    }

    public abstract Object newInstance(Class<?> klass);

    public abstract boolean canCreateInstanceOf(Class<?> klass);
}
