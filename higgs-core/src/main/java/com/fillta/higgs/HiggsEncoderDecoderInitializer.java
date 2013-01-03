package com.fillta.higgs;

import io.netty.channel.CombinedChannelHandler;

/**
 * An newInitializer which provides default NOOP implementations for {@link com.fillta.higgs.HiggsInitializer#codec()}
 * And initializes super arguments as required to super encoder/decoder based initializers
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class HiggsEncoderDecoderInitializer<IM, OM> extends HiggsInitializer<IM, OM> {
	public HiggsEncoderDecoderInitializer(boolean inflate, boolean deflate, boolean ssl) {
		super(inflate, deflate, false, ssl);
	}

	protected HiggsEncoderDecoderInitializer(final boolean inflate, final boolean deflate, final boolean ssl, final boolean usecodec, final boolean sslClientMode) {
		super(inflate, deflate, ssl, usecodec, sslClientMode);
	}

	@Override
	public CombinedChannelHandler codec() {
		return null;
	}
}
