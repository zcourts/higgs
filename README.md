# Higgs Boson

The name shamelessly stolen from the almost mythical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of a custom binary protocol

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking framework.
TODO: add answer to question about describing as message oriented as opposed to rpc e.g. explanation http://www-scf.usc.edu/~shailesn/csci-555/mp_vs_rpc.html

# Supported protocols

1. HTTP/HTTPS (Client and Server)
2. WebSocket  (Server) -
3. Boson - A language independent, object serialization protocol for sending/receiving arbitrary data over the network and
            performing remote method invocation (RMI/RPC).

# Custom Protocols

+ One of the biggest wins with Higgs is it provides a simple framework for you to do your own protocol.
            The [advanced](#advanced) section below shows how trivial it is to do so.
            However, as it stands, Higgs will continue to add support for "standard" protocols. On the to do list are
            ftp,ssh, sctp, telnet etc (They'll probably be external projects with a dependency on higgs rather than core modules).
+ __Boson__ is the actively used/developed custom protocol and is a detailed example of doing your own.
+ The Boson protocol is actively used between Node JS and Java/Scala at [Fillta](http://fillta.com)
+ The library uses the latest version 4 Netty API. Since the netty project had a major refactor between v3 and v4
it is not compatible with previous versions and Netty needs to be built and installed in your local maven REPO using the [master branch](https://github.com/netty/netty)
+ TODO: See if replacing reflection based invocation with code generation via [ReflexASM](http://code.google.com/p/reflectasm/) makes a diff.
# Features

* Simplicity and Abstraction from the underlying NIO operations & socket handling.
* Extensible - Allowing user supplied protocols, encoders,decoders,client server handlers
* Performant - While it includes an abstraction layer on top of Netty it does not impede or limit the performance you usually get with Netty
* Easily extensible to add custom protocols, binary or otherwise.
* Built on top of [Netty](http://netty.io)

#Modules

+ __core__ The Higgs framework. If you want to do a custom protocol without the other modules, add this as a dependency.
+ __boson__ An implementation of the Boson  [Protocol Specification](https://github.com/zcourts/higgs/tree/master/boson)
+ __http-client__  A feature rich asynchronous HTTP Client
+ __http-s3__ (Higgs Http Single Site Server [s3] or HS3) Is an HTTP server built for deploying a single site. It is highly configurable
                    and feature rich. Can be used to serve either REST/JSON services,static files (including HTML,images etc) AND dynamic HTML.
                    Dynamic HTML support is made possible by [Thymeleaf](http://www.thymeleaf.org/). Loosely coupled so any feature can be removed or disabled
                    via configurations. Extensible, add custom output or input manager. Annotation based configurations.
+ __websocket__ WebSocket server. The server depends on HS3 and inherits all its features. In addition, it allows serving
                either a plain WebSocket api or mixing HTTP and WebSockets on the same port and using the same or different paths.
+ __cluster__ A peer to peer system which enables boson applications to be clustered. It includes features to dynamically load balance with each Node in the cluster having roles. The implementation is still in very early development and experimental use so hasn't be published yet.
+ __events__ Events offers a simple mechanism to make thread safe, multi-threaded applications that communicate asynchronously

# Getting started

Each protocol comes with a simple client/server demo.

## Examples

### HTTP

```java

public class Demo {
    private static HttpRequestBuilder defaults = new HttpRequestBuilder();
    private static Logger log = LoggerFactory.getLogger(Demo.class);

    private Demo() {
        //configure default builder
        defaults.acceptedLanguages("en,fr")
                .acceptedMimeTypes("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .charSet("ISO-8859-1,utf-8;q=0.7,*;q=0.7")
                .userAgent("Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)")
                .connection(HttpHeaders.Values.CLOSE)
                        //automatically follow redirects when these status codes are returned
                .redirectOn(301, 302, 303, 307, 308);
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });

        //automatically follow redirects
        //disable redirect
        HttpRequestBuilder clone = defaults.copy();
        // could remove all redirect statuses with copy.redirectOn().clear();
        Request req = clone.GET(new URI("http://httpbin.org/relative-redirect/1"),
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, final Response response) {
                        System.out.println(s);
                    }
                }));
        req.execute();
        Request r = clone.GET(new URI("http://httpbin.org/redirect/1"),
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, Response response) {
                        System.out.println(s);
                        System.out.println(response);
                    }
                }));
        r.execute();

        //keeping all previous settings on r we can make a request to a url on the same host
        //by providing a path or to a different host by providing a complete URL
        //this will make a request to http://httpbin.org/get
        r.url("/get").execute();

        //to read an entire page
        PageReader page = new PageReader();
        page.listen(new Function2<String, Response>() {
            public void apply(String data, Response response) {
                System.out.println("----------------------------------- SIMPLE GET ----------------------------------");
                System.out.println(data);
            }
        });
        //by using copy we create a new instance which keeps the global settings configured on defaults
        //and now any operation on the copy is completely independent so default settings can be changed
        // without affecting each other
        Request request = defaults.copy().GET(new URI("http://httpbin.org/get"), page);
        //get the request here
        Response response = request.response();

        request
                //can add headers
                .header("some-header", "it's value")
                        //can add cookies separately
                .cookie("cookie-name", "cookie value");

        request.execute().
                addListener(new GenericFutureListener<Future<Response>>() {
                    public void operationComplete(Future<Response> future) throws Exception {
                        //or get the response here
                    }
                });

        //to read a url line by line such as a Twitter or other API stream
        //use alternative constructor
        LineReader lineReader = new LineReader(new Function2<String, Response>() {
            public void apply(String line, Response response) {
                System.out.println("LINE: " + line);
            }
        });
        defaults.GET(new
                URI("http://httpbin.org/get"), lineReader).
                execute();

        //to download a file
        FileReader fileReader = new FileReader(new Function2<File, Response>() {
            public void apply(File file, Response response) {
                System.out.println("--------------------------------- DOWNLOAD FILE ---------------------------------");
                System.out.print("NAME:");
                System.out.println(file.getName());
                System.out.print("PATH:");
                System.out.println(file.getPath());
                System.out.print("SIZE:");
                System.out.println(file.getTotalSpace());
            }
        });
        defaults.GET(new URI("https://codeload.github.com/zcourts/higgs/zip/master"), fileReader).execute();

        //url encoded POST request
        PageReader post = new PageReader(new Function2<String, Response>() {
            public void apply(String data, Response response) {
                System.out.println("------------------------------- URL-ENCODED POST --------------------------------");
                System.out.println(data);
            }
        });

        defaults.POST(new URI("http://httpbin.org/post"), post)
                .form("abc", 123)
                .form("def", 456)
                .header("haha", "yup")
                .execute();

        //multi part http post request
        PageReader postReader = new PageReader(new Function2<String, Response>() {
            public void apply(String data, Response response) {
                System.out.println("----------------------------------- MULTIPART -----------------------------------");
                System.out.println(data);
            }
        });
        File tmpFile = Files.createTempFile("upload", ".txt").toFile();

        if (tmpFile.exists()) {
            tmpFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile));
        writer.write("This is a temporary text file");

        //could use HttpFile(String,HttpFile) constructor but that assumes a binary file
        HttpFile file = new HttpFile("the-file-name-param");
        file.addFile(tmpFile, true); //true = isTextFile

        //could also use http://posttestserver.com/post.php to test post
        Request p = defaults.POST(new URI("http://httpbin.org/post"), postReader)
                //multipart is inferred as soon as a file is added, otherwise it'll just
                //be a normal url-encoded post
                .file(file)
                .form("abc", 123)
                .header("haha", "yup");
        p.execute().addListener(new GenericFutureListener<Future<Response>>() {
            public void operationComplete(Future<Response> future) throws Exception {
                System.out.println(future.getNow());
                //handle errors
                if (!future.isSuccess()) {
                    log.warn("ummo", future.cause());
                }
            }
        });

        //See also HttpRequestBuilder.GET,HEAD,OPTIONS,PATCH,DELETE,TRACE
        //shutdown thread pool when finished, only do this once you're sure no more requests will be made
        //do it in here becuase downloading that file will take the longest to complete
        //HttpRequestBuilder.shutdown();
    }
}

```
Output:

```javascript

{
  "url": "http://httpbin.org/get",
  "headers": {
    "Cookie": "",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/relative-redirect/1",
    "Connection": "close"
  },
  "args": {},
  "origin": "90.201.46.89"
}
{
  "url": "http://httpbin.org/get",
  "headers": {
    "Cookie": "",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/redirect/1",
    "Connection": "close"
  },
  "args": {},
  "origin": "90.201.46.89"
}
Response{chunked=false, protocolVersion=HTTP/1.1, status=200 OK, headers=io.netty.handler.codec.http.DefaultHttpHeaders@12b7530, completed=true, reader=io.higgs.http.client.future.PageReader@4c1aa2e9,
request=io.higgs.http.client.Request@68bd9607}
----------------------------------- SIMPLE GET ----------------------------------
{
  "url": "http://httpbin.org/get",
  "headers": {
    "Accept-Language": "en",
    "Accept-Encoding": "gzip,deflate",
    "Some-Header": "it's value",
    "Connection": "close",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
    "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/get",
    "Cookie": "cookie-name=\"cookie value\""
  },
  "args": {},
  "origin": "90.201.46.89"
}
                                                                                                                                                                                                                          {
  "url": "http://httpbin.org/get",
  "headers": {
    "Cookie": "",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/get",
    "Connection": "close"
  },
  "args": {},
  "origin": "90.201.46.89"
}
Response{chunked=false, protocolVersion=HTTP/1.1, status=200 OK, headers=io.netty.handler.codec.http.DefaultHttpHeaders@687f6b72, completed=true, reader=io.higgs.http.client.future.PageReader@4c1aa2e9,
request=io.higgs.http.client.Request@68bd9607}
LINE: {
LINE:   "url": "http://httpbin.org/get",
LINE:   "headers": {
LINE:     "Accept-Language": "en",
LINE:     "Accept-Encoding": "gzip,deflate",
LINE:     "Connection": "close",
LINE:     "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
LINE:     "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
LINE:     "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
LINE:     "Host": "httpbin.org",
LINE:     "Referer": "http://httpbin.org/get",
LINE:     "Cookie": ""
LINE:   },
LINE:   "args": {},
LINE:   "origin": "90.201.46.89"
LINE: }
LINE:
------------------------------- URL-ENCODED POST --------------------------------
{
  "origin": "90.201.46.89",
  "files": {},
  "form": {
    "abc": "123",
    "def": "456"
  },
  "url": "http://httpbin.org/post",
  "args": {},
  "headers": {
    "Content-Length": "15",
    "Haha": "yup",
    "Host": "httpbin.org",
    "Connection": "close",
    "Referer": "http://httpbin.org/post",
    "Cookie": "",
    "Content-Type": "application/x-www-form-urlencoded"
  },
  "json": null,
  "data": ""
}
----------------------------------- MULTIPART -----------------------------------
{
  "origin": "90.201.46.89",
  "files": {},
  "form": {},
  "url": "http://httpbin.org/post",
  "args": {},
  "headers": {
    "Transfer-Encoding": "chunked",
    "Haha": "yup",
    "Connection": "close",
    "Host": "httpbin.org",
    "Referer": "http://httpbin.org/post",
    "Cookie": "",
    "Content-Type": "multipart/form-data; boundary=70ae510349bdeea6"
  },
  "json": null,
  "data": ""
}
Response{chunked=false, protocolVersion=HTTP/1.1, status=200 OK, headers=io.netty.handler.codec.http.DefaultHttpHeaders@22438e8c, completed=true, reader=io.higgs.http.client.future.PageReader@1439ae7,
request=io.higgs.http.client.POST@7d6eb456}

--------------------------------- DOWNLOAD FILE ---------------------------------
NAME:higgs-http-client-temp-02883210041828734993.tmp
PATH:/tmp/higgs-http-client-temp-02883210041828734993.tmp
SIZE:251030364160


```
### Boson  [Protocol Specification](https://github.com/zcourts/higgs/tree/master/boson)

See the demo package [boson](https://github.com/zcourts/higgs/tree/master/boson/src/main/java/io/higgs/boson)

### Higgs S3

S3 is a simple way for you to deploy self contained web services and applications.
The below example creates the Api resource and serves its endpoints based on the annotations.
The default config also serves static files from /public
Access test method at: http://localhost:3434/api/test/a/12345?a=v&c=x,d,s
Query string etc are random but gives some data to populate the HTML page generated

```java

@method("/api")
@Produces({ MediaType.TEXT_HTML })
public class Api {
    String a = "a";
    int b = 023343;
    long c = 999999999;
    double d = Math.random();
    static int count;

    @GET
    @method
    public String index(HttpSession session) {
        System.out.println("index");
        session.put("index-" + Math.random(), Math.random());
        return "yes index";
    }

    //value uses the JAX-RS format http://jersey.java.net/nonav/documentation/latest/user-guide.html#d4e104
    //template is the name of the HTML template to use, if no template is provided then another resource
    //transformer is used on the response, if no transformer can convert the response a Not Acceptable status
    // is returned
    @method("test/{string:[a-z0-9]+}/{num:[0-9]+}")
    @template("api")
    @GET
    @POST
    public Object test(
            //inject these named parameters
            //for cookies, values can be a cookie String or a HttpCookie
            @CookieParam(HttpRequest.SID) String sessionid, //gets HttpCookie.getValue()
            @CookieParam(HttpRequest.SID) HttpCookie sessionidAsCookie, //gets HttpCookie
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
            HttpRequest request, HttpResponse response, FormFiles files,
            FormParams form, HttpCookies cookies,
            QueryParams query, HttpSession session,
            ResourcePath path, MessagePusher pusher
    ) throws JsonProcessingException {
        assert request != null;
        assert files != null;
        assert form != null;
        assert cookies != null;
        assert query != null;
        assert session != null;
        assert path != null;
        session.put("api-" + count, Math.random());
        count += 1;
        System.out.println("test:" + count);
        //set something in the session
        session.put("count", count);
        response.setCookie("api-cookie", String.valueOf(true));
        return new ObjectMapper().writeValueAsString(this);
    }

    @method(value = "json")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Map<Integer, Double> json(HttpRequest request) {
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(i, Math.random());
        }
        return map;
    }

    @method("boom")
    @GET
    public Object boom2() {
        //you can, and SHOULD return WebApplicationException
        //if a wae is returned wae.setRequest() is automatically called
        return new WebApplicationException(HttpStatus.NOT_IMPLEMENTED, "error/default");
    }

    @method("manual")
    @GET
    public Object manual() {
        //if a Function is returned then we must write the response manual
        return new Function() {
            public Object apply(Object o) {
                // message.channel.write(new HttpResponse(HttpStatus.FOUND));
                //close as soon as its written
                // message.channel.closeFuture().addListener(ChannelFutureListener.CLOSE);
                return null;
            }
        };
    }
}

```

![Thymeleaf Resource Screenshot](https://raw.github.com/zcourts/higgs/master/higgs-http-s3/example.png)

# WebSocket Server

```java

public class WebSocketDemo {
    private WebSocketDemo() {
    }

    public static void main(String... args) {
        //handles HTTP GET requests
        WebSocketConfiguration ws = new WebSocketConfiguration();
        //handles all other HTTP requests
        HttpProtocolConfiguration http = new HttpProtocolConfiguration();
        //re-write all requests to /app/* to index.html
        ws.getTranscriber().addTranscription(new Transcription("/app((?:\\/[\\w([^\\..]{1,4}\b)\\-]+)+)",
                "/index.html"));

        HiggsServer server = new HiggsServer().setConfig("config.yml", HttpConfig.class);
        server.registerProtocol(ws);
        //HTTP must be registered after WebSockets
        server.registerProtocol(http);
        //
        server.registerClass(Api.class);
        server.start();
    }
}
@method("/ws")
public class Api {

    @method("test/{string:[a-z0-9]+}/{num:[0-9]+}")
    public Object test(
            JsonRequest request,
            ChannelHandlerContext ctx,
            Channel channel,
            WebSocketConfiguration configuration,
            Pojo pojo
    ) {
        return request;
    }
}

```

# Advanced

Higgs is a fairly flexible library.

//TODO