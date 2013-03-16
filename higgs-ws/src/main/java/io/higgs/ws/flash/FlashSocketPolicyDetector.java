package io.higgs.ws.flash;

import io.higgs.EventProcessor;
import io.higgs.sniffing.ProtocolDetector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.nio.charset.Charset;

/**
 * We have to override {@link FlashSocketProtocolDetector} because the protocol detector requires only 3 bytes
 * where as to detect the flash policy file request 23 bytes are required
 * + the check is different...
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashSocketPolicyDetector implements ProtocolDetector {
    protected EventProcessor events;
    protected final FlashPolicyFile policy;

    public FlashSocketPolicyDetector(EventProcessor events, final FlashPolicyFile policy) {
        this.events = events;
        this.policy = policy;
    }

    public Boolean apply(final ByteBuf in) {
        //detect flash policy file request => Higgs Flash Socket (Header)
        String request = in.toString(Charset.forName("utf-8"));
        //adobe documents it as "<policy-file-request />" with a space but in reality flash 10 has no space
        //http://help.adobe.com/en_US/AS2LCR/Flash_10.0/help.html?content=00000471.html
        if (request.contains("<policy-file-request")) {
            return true;
        }
        return false;
    }

    public boolean setupPipeline(final ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        if (pipeline.get("flash-policy-encoder") != null) {
            pipeline.remove("flash-policy-encoder");
        }
        if (pipeline.get("flash-policy-decoder") != null) {
            pipeline.remove("flash-policy-decoder");
        }
        //add an encoder to the pipeline to send the policy file.
        pipeline.addLast("flash-policy-encoder", new FlashPolicyEncoder());
        //add the flash policy decoder...
        pipeline.addLast("flash-policy-decoder", new FlashPolicyDecoder(policy));
        //discard the request, the decoder writes the policy which gets encoded by the encoder
        pipeline.addLast("handler", new DiscardHandler());
        return false; //don't remove protocol sniffer
    }

    public int bytesRequired() {
        return 23;
    }
}
