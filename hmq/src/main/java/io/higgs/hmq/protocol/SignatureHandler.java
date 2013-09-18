package io.higgs.hmq.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class SignatureHandler extends SimpleChannelInboundHandler<Signature> {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Signature msg) throws Exception {
        if (msg.isValid()) {
            ctx.writeAndFlush(Signature.fixed());
            //once we've done the signature get out of the pipeline and make wat for the revision/socket handler
            ctx.pipeline().remove(this);
        } else {
            //if the signature is invalid then we can't allow any further interaction
            log.error(String.format("An invalid signature was received during the ZMQ handshake, data \n %s",
                    msg.rawBuffer().toString(Charset.forName("UTF-8"))));
            ctx.channel().close();
        }
    }
}
