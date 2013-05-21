package io.higgs.http.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ClientHandler extends ChannelInboundMessageHandlerAdapter<Object> {

    private final Response response;
    private final FutureResponse future;
    private boolean redirecting;

    public ClientHandler(Response response, FutureResponse future) {
        this.future = future;
        this.response = response;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (redirecting && msg instanceof LastHttpContent) {
            //execute a new request using the same request instance and response
            //this will use a new channel initializer
            response.request()
                    .execute()
                    .addListener(new GenericFutureListener<Future<Response>>() {
                        public void operationComplete(Future<Response> f) throws Exception {
                            if (!f.isSuccess()) {
                                future.setFailure(f.cause());
                            }
                        }
                    });
            return;
        }
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            String location = res.headers().get(HttpHeaders.Names.LOCATION);
            if (response.request().redirectOn().contains(res.getStatus().code())
                    && location != null) {
                response.request()
                        .url(location);
                redirecting = true;
                return;
            }
            response.setStatus(res.getStatus());
            response.setProtocolVersion(res.getProtocolVersion());
            response.setHeaders(res.headers());

            if (res.getStatus().code() == 200 && HttpHeaders.isTransferEncodingChunked(res)) {
                response.setChunked(true);
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            response.write(chunk.content());
            if (chunk instanceof LastHttpContent) {
                response.setCompleted(true);
                future.setSuccess(response);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        future.setFailure(cause);
        ctx.channel().close();
    }
}
