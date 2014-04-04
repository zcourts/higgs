# Higgs Single Site Server (S3)

Using the demo resource below going to the URL http://localhost:3434/api/test/a/12345?a=v&c=x,d,s
produces:

![Thymeleaf Resource Screenshot](https://raw.github.com/zcourts/higgs/master/higgs-http-s3/example.png)

S3 is a simple way for you to deploy self contained web services and applications.
The below example creates the Api resource and serves its endpoints based on the annotations.
The default config also serves static files from /public
Access test classMethod at: http://localhost:3434/api/test/a/12345?a=v&c=x,d,s
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
