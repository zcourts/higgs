package info.crlog.higgs;

import info.crlog.higgs.messaging.MessageFactory;

/**
 * All subclasses of {@link HiggsClient} and {@link HiggsServer} MUST initialize
 * the decoder and encoder properties supplying a valid {@link MessageFactory}
 * instance
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class EncoderDecoderInitializationException extends RuntimeException {

    public EncoderDecoderInitializationException() {
        super("All subclasses of {@link HiggsClient} and {@link HiggsServer} "
                + "MUST initialize the decoder and encoder properties supplying "
                + "a valid {@link MessageFactory} instance");
    }
}
