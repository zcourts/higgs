package io.higgs.events;

import io.higgs.core.HiggsServer;
import io.higgs.core.ObjectFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class RandomFactory extends ObjectFactory {
    private final Object instance;
    private final Class<?> klass;

    public RandomFactory(HiggsServer server, Object instance) {
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
}
