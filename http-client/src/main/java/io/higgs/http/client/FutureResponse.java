package io.higgs.http.client;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FutureResponse extends DefaultPromise<Response> {
    private final Response response;

    public FutureResponse(EventLoopGroup group, Response response) {
        super(group.next());
        this.response = response;
    }

    @Override
    public Promise<Response> setFailure(Throwable cause) {
        Promise<Response> res = super.setFailure(cause);
        response.markFailed(cause);
        return res;
    }
}
