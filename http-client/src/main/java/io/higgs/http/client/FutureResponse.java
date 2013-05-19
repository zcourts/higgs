package io.higgs.http.client;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FutureResponse extends DefaultPromise<Response> {
    public FutureResponse(EventLoopGroup group) {
        super(group.next());
    }
}
