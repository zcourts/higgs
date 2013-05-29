package io.higgs.http.server.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import io.higgs.core.ResourcePath;
import io.higgs.core.method;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.MessagePusher;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.params.CookieParam;
import io.higgs.http.server.params.FormFiles;
import io.higgs.http.server.params.FormParam;
import io.higgs.http.server.params.FormParams;
import io.higgs.http.server.params.HeaderParam;
import io.higgs.http.server.params.HttpCookie;
import io.higgs.http.server.params.HttpCookies;
import io.higgs.http.server.params.HttpSession;
import io.higgs.http.server.params.PathParam;
import io.higgs.http.server.params.QueryParam;
import io.higgs.http.server.params.QueryParams;
import io.higgs.http.server.resource.GET;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.resource.POST;
import io.higgs.http.server.resource.Produces;
import io.higgs.http.server.resource.template;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
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

    public String getA() {
        return a;
    }

    public int getCount() {
        return count;
    }

    public int getB() {
        return b;
    }

    public long getC() {
        return c;
    }

    public double getD() {
        return d;
    }
}
