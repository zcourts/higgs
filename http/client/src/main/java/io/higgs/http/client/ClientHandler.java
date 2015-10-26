package io.higgs.http.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ClientHandler extends SimpleChannelInboundHandler<Object> {

    private final Response response;
    private final FutureResponse future;
    private boolean redirecting;
    protected RetryPolicy policy;

    public ClientHandler(Response response, FutureResponse future, RetryPolicy policy) {
        this.future = future;
        this.response = response;
        this.policy = policy;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            String location = res.headers().get(HttpHeaders.Names.LOCATION);
            if (response.request().redirectOn().contains(res.getStatus().code()) && location != null) {
                //execute a new request using the same request instance and response
                //this will use a new channel initializer
                //BufUtil.retain(response.request().nettyRequest());
                response.request()
                        .url(location)
                        .execute()
                        .addListener(new GenericFutureListener<Future<Response>>() {
                            public void operationComplete(Future<Response> f) throws Exception {
                                if (!f.isSuccess()) {
                                    future.setFailure(f.cause());
                                }
                            }
                        });
                redirecting = true;
                return;
            }
            response.setStatus(res.getStatus());
            response.setProtocolVersion(res.getProtocolVersion());
            response.setHeaders(res.headers());

            if (HttpHeaders.isTransferEncodingChunked(res)) {
                response.setChunked(true);
            }
            //retry logic after setting headers etc
            if (policy != null && response.request().retryOn().contains(res.getStatus().code())) {
                policy.activate(future, null, false, response);
                return;
            }
        }
        if (!redirecting && msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            response.write(chunk.content());
            if (chunk instanceof LastHttpContent) {
                ctx.channel().close(); //received everything so close the connection
                response.setCompleted(true);
                //make sure it's not already marked as finished
                if (!future.isDone()) {
                    future.setSuccess(response);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (policy != null) {
            policy.activate(future, cause, false, response);
        } else {
            future.setFailure(cause);
            ctx.channel().close();
        }
    }
}
