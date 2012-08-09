/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package example.securechat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 */
public class SecureChatClient {

	private final String host;
	private final int port;

	public SecureChatClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void run() throws Exception {
		Bootstrap b = new Bootstrap();
		try {
			b.eventLoop(new NioEventLoop()).channel(new NioSocketChannel()).remoteAddress(host, port).handler(new SecureChatClientInitializer());

			// Start the connection attempt.
			Channel ch = b.connect().sync().channel();

			// Read commands from the stdin.
			ChannelFuture lastWriteFuture = null;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			for (;;) {
				String line = in.readLine();
				if (line == null) {
					break;
				}

				// Sends the received line to the server.
				lastWriteFuture = ch.write(line + "\r\n");

				// If user typed the 'bye' command, wait until the server closes
				// the connection.
				if (line.toLowerCase().equals("bye")) {
					ch.closeFuture().sync();
					break;
				}
			}

			// Wait until all messages are flushed before closing the channel.
			if (lastWriteFuture != null) {
				lastWriteFuture.sync();
			}
		} finally {
			// The connection is closed automatically on shutdown.
			b.shutdown();
		}
	}

	public static void main(String[] args) throws Exception {
		// Print usage if no argument is specified.
//        if (args.length != 2) {
//            System.err.println(
//                    "Usage: " + SecureChatClient.class.getSimpleName() +
//                    " <host> <port>");
//            return;
//        }

		// Parse options.
//        String host = args[0];
//        int port = Integer.parseInt(args[1]);

		new SecureChatClient("graph.facebook.com", 443).run();
	}
}
