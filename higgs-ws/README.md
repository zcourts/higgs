# Higgs WebSocket

Provides an implementation of a WebSocket server.
The Server can be used to server both HTTP and WebSocket requests on the same port or even at the same path.
It automatically detects the request type and handles it with an appropriate response.

# Server

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

1 : TextEvent{message='{}', topic='test'}
2 : TextEvent{message='{}', topic='test'}
3 : TextEvent{message='{}', topic='test'}

```

# Client
 Not implemented yet. On the TODO list.