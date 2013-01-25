package com.fillta.higgs.sniffing;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.ssl.SSLConfigFactory;
import com.fillta.higgs.ssl.SSLContextFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class ProtocolSniffer extends ChannelInboundByteHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final EventProcessor events;
    private final boolean detectSsl;
    private final boolean detectGzip;
    private final Set<ProtocolDetector> detectors;

    public ProtocolSniffer(Set<ProtocolDetector> detectors, EventProcessor events,
                           boolean detectSsl, boolean detectGZip) {
        this.events = events;
        this.detectSsl = detectSsl;
        this.detectGzip = detectGZip;
        this.detectors = detectors;
    }

    public void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // Will use the first five bytes to detect SSL.
        if (detectSsl && in.readableBytes() < 5) {
            return;
        }
        if (detectGzip && in.readableBytes() < 2) {
            return;
        }
        if (isSsl(in)) {
            enableSsl(ctx);
        } else {
            final int magic1 = in.getUnsignedByte(in.readerIndex());
            final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
            if (isGzip(magic1, magic2)) {
                enableGzip(ctx);
            } else {
                boolean foundProtocol = false;
                if (log.isDebugEnabled()) {
                    log.debug(new String(in.toString(Charset.forName("utf-8"))));
                }
                for (ProtocolDetector fn : detectors) {
                    if (in.readableBytes() < fn.bytesRequired()) {
                        //go to the next detector this one can't detect the protocol from what's available
                        continue;
                    }
                    foundProtocol = fn.apply(in);
                    if (foundProtocol) {
                        boolean removeSelf = fn.setupPipeline(ctx);
                        if (removeSelf) {
                            ctx.pipeline().remove(this);
                        }
                        break;
                    }
                }
                if (!foundProtocol) {
                    log.error("Unable to detect the protocol of an incoming connection!");
                    // Unknown protocol; discard everything and close the connection.
                    in.clear();
                    ctx.close();
                    return;
                }
            }
        }
        // Forward the current read buffer as is to the new handlers.
        ctx.nextInboundByteBuffer().writeBytes(in);
        ctx.fireInboundBufferUpdated();
    }

    private boolean isSsl(ByteBuf buf) {
        if (detectSsl) {
            return SslHandler.isEncrypted(buf);
        }
        return false;
    }

    private boolean isGzip(int magic1, int magic2) {
        if (detectGzip) {
            //see http://www.gzip.org/zlib/rfc-gzip.html#header-trailer for the magic number definitions
            return magic1 == 31 && magic2 == 139;
        }
        return false;
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
        engine.setUseClientMode(false);

        p.addLast("ssl", new SslHandler(engine));
        p.addLast("unificationA", new ProtocolSniffer(detectors, events, false, detectGzip));
        p.remove(this);
    }

    private void enableGzip(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        p.addLast("unificationB", new ProtocolSniffer(detectors, events, detectSsl, false));
        p.remove(this);
    }
}
