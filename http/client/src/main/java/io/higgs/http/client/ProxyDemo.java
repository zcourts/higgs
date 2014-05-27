package io.higgs.http.client;

import io.higgs.core.func.Function2;
import io.higgs.http.client.readers.PageReader;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ProxyDemo {
    private static HttpRequestBuilder defaults = new HttpRequestBuilder();
    private static Logger log = LoggerFactory.getLogger(ProxyDemo.class);

    private ProxyDemo() {
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
        HttpRequestBuilder proxied = defaults.copy();
        proxied.proxy("localhost", 3128, "a", "b");
        Request req = proxied.GET(new URI("http://api.datasift.com/v1/usage?username=zcourts&api_key=abc123"),
                new PageReader(new Function2<String, Response, Void>() {
                    public Void apply(String s, final Response response) {
                        System.out.println(s);
                        return null;
                    }
                })
        );

        //this request will be tunneled because it uses HTTPS
        Request req2 = proxied.GET(new URI("https://api.datasift.com/v1/usage"),
                new PageReader(new Function2<String, Response, Void>() {
                    public Void apply(String s, final Response response) {
                        System.out.println(s);
                        HttpRequestBuilder.shutdown();
                        return null;
                    }
                })
        );
        req.execute();
        req2.execute();
    }
}
