package io.higgs.core;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface EventListenerMethod {
    /**
     * This method is invoked after an instance of a resource has been created AND any injectable resources have been
     * injected.
     * i.e. after constructor initialization, since there is no way to inject field values before calling
     * the constructor.
     */
    void init();
}
