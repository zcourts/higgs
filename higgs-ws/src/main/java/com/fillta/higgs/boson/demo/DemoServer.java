package com.fillta.higgs.boson.demo;

import com.fillta.higgs.boson.BosonServer;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DemoServer {
	public static void main(String... args) {
		BosonServer server = new BosonServer(8080);
//		server.setQueueingStrategyAsBlockingQueue();
		server.setQueueingStrategyAsCircularBuffer();
		server.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
			public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
				ex.get().printStackTrace();
			}
		});
		server.registerPackage(Listener.class.getPackage().getName());
		server.bind();
	}


//	public static void main(String[] args) throws Exception {
//		// create a script engine manager
//		ScriptEngineManager factory = new ScriptEngineManager();
//		// create a JavaScript engine
//		ScriptEngine engine = factory.getEngineByName("JavaScript");
//		// evaluate JavaScript code from String
//		System.out.println(engine.eval("print new Date('2012-12-14T13:43:00+0000').getTime()"));
//	}

}
