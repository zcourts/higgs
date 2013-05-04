# Higgs Boson

A high performance, message oriented network library. It provides a core extensible framework and libraries built on top of the core. Libraries include WebSocket server, HTTP Server and Client and Boson a custom serialization and RMI library. 

The name shamelessly stolen from the almost mythical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of the protocol

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking framework.
TODO: add answer to question about describing as message oriented as opposed to rpc e.g. explanation http://www-scf.usc.edu/~shailesn/csci-555/mp_vs_rpc.html
The project was started to remove the need I had for ZeroMQ (too many issues with native dependency in jzmq)
The other protocols were added as the need arose. And it has since grown to be more robust than originally intended.

# Supported protocols

1. HTTP/HTTPS (Client and Server)
2. WebSocket  (Client and Server) - Compatible with Socket.io or any other WebSocket client (being ported from original Scala implementation)
3. Boson - A language independent, path oriented protocol for sending/receiving arbitrary data over the network and
            performing remote method invocation (RMI/RPC).

# Custom Protocols

+ One of the biggest wins with Higgs is it provides a simple framework for you to do your own protocol.
            The [advanced](#advanced) section below shows how trivial it is to do so.
            However, as it stands, Higgs will continue to add support for "standard" protocols. On the to do list are
            ftp,ssh, sctp, telnet etc (They'll probably be external projects with a dependency on higgs rather than core modules).
+ __Boson__ is the actively used/developed custom protocol and is a detailed example of doing your own.
+ A __Node JS__ implementation of the Boson protocol can be found here [https://github.com/zcourts/higgs-node](https://github.com/zcourts/higgs)
+ The Boson protocol is actively used between Node JS and Java/Scala at [Fillta](http://fillta.com)
+ The library uses the latest version 4 Netty API. Since the netty project had a major refactor between v3 and v4
it is not compatible with previous versions and Netty needs to be built and installed in your local maven REPO using the [master branch](https://github.com/netty/netty)
+ TODO: See if replacing reflection based invocation with code generation via [ReflexASM](http://code.google.com/p/reflectasm/) makes a diff.
# Features

* Simplicity and Abstraction from the underlying NIO operations & socket handling.
* Extensible - Allowing user supplied protocols, encoders,decoders,client server handlers
* Performant
* Easily extensible to add custom protocols, binary or otherwise.
* Built on top of [Netty](http://netty.io)

#Modules

+ __higgs-core__ The Higgs framework. If you want to do a custom protocol without the other modules, add this as a dependency.
+ __higgs-boson__ An implementation of the Boson  [Protocol Specification](https://github.com/zcourts/higgs/tree/master/higgs-boson)
+ __higgs-http-client__  A feature rich asynchronous HTTP Client
+ __higgs-http-s3__ (Higgs Http Single Site Server [s3] or HS3) Is an HTTP server built for deploying a single site. It is highly configurable
                    and feature rich. Can be used to serve either REST/JSON services,static files (including HTML,images etc) AND dynamic HTML.
                    Dynamic HTML support is made possible by [Thymeleaf](http://www.thymeleaf.org/). Loosely coupled so any feature can be removed or disabled
                    via configurations. Extensible, add custom output or input manager. Annotation based configurations.
+ __higgs-ws__ WebSocket server and client. The server depends on HS3 and inherits all its features. In addition, it allows serving
                either a plain WebSocket api or mixing HTTP and WebSockets on the same port and using the same or different paths.
+ __higgs-scala__ On the to do list, intention is to provide a Scala esk API

# Getting started

Each protocol comes with a simple client/server demo.

## Examples

### HTTP

```java

import com.fillta.functional.Function1;
import com.fillta.higgs.http.client.HTTPResponse;
import com.fillta.higgs.http.client.HttpRequestBuilder;
import java.io.IOException;
public class Demo {
	public static void main(String... args) throws IOException, InterruptedException {
		HttpRequestBuilder builder = new HttpRequestBuilder();
		builder
				.url("http://httpbin.org/post")
				.POST()
				.cookie("username", "courtney")
				.cookie("id", 3)
				.cookie("postcode", "cr8 4hb")
				.form("title", "some post field")
				.form("desc", "a post field desc")
						//.file(new HttpFile("images", file))
				.build(new Function1<HTTPResponse>() {
					public void apply(HTTPResponse a) {
						System.out.println(a);
					}
				});
	}
}

```
Output:

```javascript

200 OK
 SINGLE
 HTTP/1.1
 {Date=[Wed, 02 Jan 2013 23:24:57 GMT], Content-Length=[778], Content-Type=[application/json], Connection=[Close], Server=[gunicorn/0.16.1]}
 {
  "origin": "10.117.13.242",
  "files": {},
  "form": {
    "desc": "hacked upload desc",
    "title": "hacked upload"
  },
  "url": "http://httpbin.org/post",
  "args": {},
  "headers": {
    "Content-Length": "43",
    "Accept-Language": "en",
    "Accept-Encoding": "gzip,deflate",
    "Connection": "keep-alive",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
    "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/post",
    "Cookie": "id=3; username=courtney; postcode=\"cr8 4hb\"",
    "Content-Type": "application/x-www-form-urlencoded"
  },
  "json": null,
  "data": ""
}

```
### Boson  [Protocol Specification](https://github.com/zcourts/higgs/tree/master/higgs-boson)

See the demo package [/higgs-boson/src/main/java/com/fillta/higgs/boson/demo](https://github.com/zcourts/higgs/tree/master/higgs-boson/src/main/java/com/fillta/higgs/boson/demo)

### Higgs S3

S3 is a simple way for you to deploy self contained web services and applications.
The below example creates the Api resource and serves its endpoints based on the annotations.
The default config also serves static files from /public
Access test method at: http://localhost:3434/api/test/a/12345?a=v&c=x,d,s
Query string etc are random but gives some data to populate the HTML page generated

```java

public class HttpServerDemo {
	public static void main(String... args) throws IOException, InterruptedException {
		HttpServer server = new HttpServer("./config.yml");
		server.register(Api.class);
		server.bind();
	}
}
@Path("/api")
public class Api {
	String a = "a";
	int b = 023343;
	long c = 999999999;
	double d = Math.random();
	static int count;

	@GET
	public String index() {
		System.out.println("index");
		return "yes index";
	}

	//value uses the JAX-RS format http://jersey.java.net/nonav/documentation/latest/user-guide.html#d4e104
	//template is the name of the HTML template to use, if no template is provided then another resource
	//transformer is used on the response, if no transformer can convert the response a Not Acceptable status is returned
	@Path(value = "test/{string:[a-z0-9]+}/{num:[0-9]+}", template = "api")
	@GET
	@POST
	public Object test(
			//inject these named parameters
			//for cookies, values can be a cookie object or a HiggsHttpCookie
			@CookieParam(HttpServer.SID) String sessionid, //gets HiggsHttpCookie.getValue()
			@CookieParam(HttpServer.SID) HttpCookie sessionidAsCookie, //gets HiggsHttpCookie
			//will be null in get requests
			@FormParam("textline") String text,
			@HeaderParam("Connection") String keepAlive,
			@PathParam("string") String random,
			//if a primitive number such as int,double,float etc is not found it'll be 0
			@PathParam("num") int integer,
			@PathParam("num-doesn't-exist") int integerPrimitive,
			//if boxed Number such as Integer,Double,Float etc is not found then value will be null
			@PathParam("some-random-name") Integer randomInt,
			@QueryParam("a") String a,
			//all these unnamed parameters can be injected and should never be null
			HttpServer server,
			ChannelMessage<HttpRequest> message,
			HttpRequest request, FormFiles files,
			FormParams form, HttpCookies cookies,
			QueryParams query, HttpSession session,
			ResourcePath path
	) throws JsonProcessingException {
		count += 1;
		System.out.println("test:" + count);
		return new ObjectMapper().writeValueAsString(this);
	}

	@Path("boom1")
	@GET
	public void boom1(HttpRequest request) {
	    //can throw web exception to return html error to user
		throw new WebApplicationException(HttpStatus.NOT_IMPLEMENTED, null, request, "error/default");
	}

	@Path("boom2")
	@GET
	public Object boom2() {
		//you can, and SHOULD return WebApplicationException
		//if a wae is returned wae.setRequest() is automatically called
		return new WebApplicationException(HttpStatus.NOT_IMPLEMENTED, "error/default");
	}

	@Path("manual")
	@GET
	public Object manual(final ChannelMessage<HttpRequest> message) {
		//if a Function is returned then we must write the response manually
		return new Function() {
			public void apply() {
				message.channel.write(new HttpResponse(HttpStatus.FOUND));
				//close as soon as its written
				message.channel.closeFuture().addListener(ChannelFutureListener.CLOSE);
			}
		};
	}
}


```

![Thymeleaf Resource Screenshot](https://raw.github.com/zcourts/higgs/master/higgs-http-s3/example.png)

# WebSocket Server

```java

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

```
## Output

```javascript

1 : TextEvent{message='{}', path='test'}
2 : TextEvent{message='{}', path='test'}
3 : TextEvent{message='{}', path='test'}

```

# Advanced

Higgs is a fairly flexible library.

Here's a quick protocol implementation  creates a server.
When a client sends a string to the server, the server invokes a locally registered method called
"test" (hard coded for the example). The example then returns the same string back to the client.
Obviously, if you need a custom protocol you won't be sending a simple string some complex serialization
and de-serialization will be needed. The most complete example of a complex protocol is the __higgs-boson__
module. Its serialization goes as far as supporting "references".  That will be a more complete example for
using higgs-core.

```java

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

```
