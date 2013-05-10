package io.higgs.core;

/**
 * An object factories is responsible for creating instances of various objects.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class ObjectFactory {
    private final HiggsServer server;

    public ObjectFactory(HiggsServer server) {
        this.server = server;
    }

    public abstract Object newInstance(Class<?> klass);

    public abstract boolean canCreateInstanceOf(Class<?> klass);

    public HiggsServer getServer() {
        return server;
    }
}
