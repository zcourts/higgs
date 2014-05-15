package io.higgs.common.annotations.tests;

import io.higgs.common.annotations.HiggsAnnotationProcessor;

import javax.xml.ws.Provider;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SimpleSpiProcessor extends HiggsAnnotationProcessor {
    @Override
    protected Set<Class<?>> types() {
        return new HashSet<Class<?>>() {
            {
                //generate SPI for all usages of these at compile time
                add(Provider.class);
                add(SpiAnnotation.class);
                add(SpiClass.class);
                add(SpiInterface.class);
            }
        };
    }
}
