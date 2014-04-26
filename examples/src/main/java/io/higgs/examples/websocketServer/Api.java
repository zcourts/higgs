package io.higgs.examples.websocketServer;

import io.higgs.ws.JsonRequest;
import io.higgs.ws.protocol.WebSocketConfiguration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import javax.ws.rs.Path;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Path("/ws")
public class Api {

    @Path("test/{string:[a-z0-9]+}/{num:[0-9]+}")
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
