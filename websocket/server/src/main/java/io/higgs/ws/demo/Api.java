package io.higgs.ws.demo;

import io.higgs.core.method;
import io.higgs.ws.JsonRequest;
import io.higgs.ws.protocol.WebSocketConfiguration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@method("/ws")
public class Api {

    @method("test/{string:[a-z0-9]+}/{num:[0-9]+}")
    public Object test(
            JsonRequest request,
            ChannelHandlerContext ctx,
            Channel channel,
            WebSocketConfiguration configuration,
            Pojo pojo
    ) {
        return request;
    }
}
