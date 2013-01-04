package com.fillta.higgs.ws.demo;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.ws.JsonEvent;
import com.fillta.higgs.ws.WebSocketServer;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketServerDemo {
	static int count = 0;

	public static void main(String... args) {
		WebSocketServer server = new WebSocketServer(3535);
		server.HTTP.register(Api.class);
		server.listen("test", new Function1<ChannelMessage<JsonEvent>>() {
			public void apply(final ChannelMessage<JsonEvent> a) {
				System.out.println(++count + " : " + a.message);
			}
		});
		server.bind();
	}
}
