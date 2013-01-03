package com.fillta.higgs.http.server.demo;

import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.config.ServerConfig;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpServerDemo {
	public static void main(String... args) throws IOException, InterruptedException {
		HttpServer server = new HttpServer(ServerConfig.class,"./config.yml");
		server.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
			public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
				if (ex.isPresent()) {
					LoggerFactory.getLogger(getClass()).warn("Error", ex.get());
				}
			}
		});
//		server.setQueueingStrategyAsCircularBuffer();
		server.register(Api.class);
		server.bind();
	}
}
