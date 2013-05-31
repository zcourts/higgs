package io.higgs.events;

import io.higgs.core.HiggsServer;
import io.higgs.core.ObjectFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SingletonFactory extends ObjectFactory {
    private final Object instance;
    private final Class<?> klass;

    public SingletonFactory(HiggsServer server, Object instance) {
        super(server);
        this.instance = instance;
        klass = instance.getClass();
        server.registerClass(klass);
    }

    @Override
    public Object newInstance(Class<?> klass) {
        return instance;
    }

    @Override
    public boolean canCreateInstanceOf(Class<?> klass) {
        return this.klass.isAssignableFrom(klass);
    }

    /**
     * @return the instance represented by this singleton factory
     */
    public Object instance() {
        return instance;
    }
}
