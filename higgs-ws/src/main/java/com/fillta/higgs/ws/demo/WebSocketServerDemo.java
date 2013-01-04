package com.fillta.higgs.ws.demo;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.ws.JsonEvent;
import com.fillta.higgs.ws.WebSocketServer;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketServerDemo {
	static int count = 0;

	public static void main(String... args) {
		WebSocketServer server = new WebSocketServer(3535);
		server.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
			public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
				ex.get().printStackTrace();
			}
		});
		server.HTTP.getConfig().template_config.cacheable=false;
		server.HTTP.register(Api.class);
		server.listen("test", new Function1<ChannelMessage<JsonEvent>>() {
			public void apply(final ChannelMessage<JsonEvent> a) {
				System.out.println(++count + " : " + a.message);
			}
		});
		server.bind();
	}
}
