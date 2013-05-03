package io.higgs.core;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ProtocolConfiguration {
    ProtocolDetectorFactory getProtocol();

    MethodProcessor getMethodProcessor();

    /**
     * Invoked before any other method is called on the protocol implementation.
     *
     * @param server the server instance this protocol is being registered to
     */
    void initialise(HiggsServer server);
}
