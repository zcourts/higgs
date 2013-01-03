package com.fillta.higgs;

import com.fillta.higgs.events.ChannelMessage;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
public class SingleFileDemo {
	public static class MyServer extends RPCServer<String, String, ByteBuf> {
		private final MyServer me;

		public MyServer(int port) {
			super(port);
			me = this;
		}

		//given an incoming request, extract the data necessary to construct a set of parameters for a method
		public Object[] getArguments(final Class<?>[] argTypes, final ChannelMessage<String> request) {
			return new Object[]{request.message};
		}

		protected String newResponse(final String methodName, final ChannelMessage<String> request, final Optional<Object> returns, final Optional<Throwable> error) {
			return request.message;//just return the same message that was received for simplicity
		}

		public ChannelInitializer<SocketChannel> initializer() {
			return new HiggsEncoderDecoderInitializer<String, String>(false, false, false) {
				public ChannelInboundMessageHandlerAdapter handler() {
					return new HiggsEventHandlerProxy(me);
				}

				public ByteToMessageDecoder<String> decoder() {
					return new ByteToMessageDecoder<String>() {
						protected String decode(final ChannelHandlerContext context, final ByteBuf buf) throws Exception {
							byte[] data = new byte[buf.writerIndex()];
							buf.getBytes(buf.writerIndex(), data);
							return new String(data);
						}
					};
				}

				public MessageToByteEncoder<String> encoder() {
					return new MessageToByteEncoder<String>() {
						protected void encode(final ChannelHandlerContext context, final String s, final ByteBuf buf) throws Exception {
							buf.writeBytes(s.getBytes());
						}
					};
				}
			};
		}

		public MessageConverter<String, String, ByteBuf> serializer() {
			return new MessageConverter<String, String, ByteBuf>() {
				public ByteBuf serialize(final Channel ctx, final String msg) {
					return Unpooled.wrappedBuffer(msg.getBytes());
				}

				public String deserialize(final ChannelHandlerContext ctx, final ByteBuf msg) {
					byte[] data = new byte[msg.writerIndex()];
					msg.getBytes(msg.writerIndex(), data);
					return new String(data);
				}
			};
		}

		public MessageTopicFactory<String, String> topicFactory() {
			return new MessageTopicFactory<String, String>() {
				public String extract(final String msg) {
					return "test";//determine which method to invoke
				}
			};
		}
	}
}
