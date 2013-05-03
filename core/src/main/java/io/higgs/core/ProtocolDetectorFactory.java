package io.higgs.core;

/**
 * To support shared state encoders, decoders and {@link ProtocolDetector}s a {@link ProtocolDetectorFactory}
 * is used as a factory to create new instances of each for every channel.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ProtocolDetectorFactory extends Sortable<ProtocolDetectorFactory>{

    /**
     * Provide a new instance of the {@link ProtocolDetector} this codec represents
     * Very important that a new instance is provided each time. If the same one is returned it will be used by multiple
     * threads.
     *
     * @return A new ProtocolDetector
     */
    ProtocolDetector newProtocolDetector();

}
