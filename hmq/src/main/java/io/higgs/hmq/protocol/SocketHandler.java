package io.higgs.hmq.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class SocketHandler extends SimpleChannelInboundHandler<Message> {
    long count, time, total;
    private Logger log = LoggerFactory.getLogger(getClass());
    private Socket socket;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        ++count;
        if (++total % 10000 == 0) {
//            System.out.println(total + ":" + msg);
            long now = System.nanoTime();
            if (time == 0) {
                time = now;
            } else {
                long timeTaken = TimeUnit.NANOSECONDS.toSeconds(now - time);
                if (timeTaken > 0) {
                    time = now;
                    System.out.println(
                            String.format("%s messages per second with a total of %s received", (count / timeTaken),
                                    total)
                    );
                    count = 0;
                }
            }
        }
    }

    protected void setSocket(Socket socket) {
        this.socket = socket;
        System.out.println("New ZMQ socket created and handshake completed!");
        //try doing a subscription
        Message subscribe = new Message(Message.Command.SUBSCRIBE, "B");
        socket.channel().writeAndFlush(subscribe);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
