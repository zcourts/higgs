package com.fillta.higgs;

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * An initializer which provides default NOOP implementations for {@link com.fillta.higgs.HiggsInitializer#decoder()}
 * and {@link com.fillta.higgs.HiggsInitializer#encoder()}. It also configures parent options
 * suitable for codec implementations such as HTTP
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class HiggsCodecInitializer<IM, OM> extends HiggsInitializer<IM, OM> {
    public HiggsCodecInitializer(boolean inflate, boolean deflate, boolean ssl) {
        super(inflate, deflate, true, ssl);
    }

    @Override
    public ByteToMessageDecoder<IM> decoder() {
        return null;
    }

    @Override
    public MessageToByteEncoder<OM> encoder() {
        return null;
    }
}
