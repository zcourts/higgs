package com.fillta.higgs.http.client.demo;

import com.fillta.functional.Function1;
import com.fillta.higgs.http.client.HTTPResponse;
import com.fillta.higgs.http.client.HttpRequestBuilder;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

public class Demo {
    protected Demo() {
    }

    public static void main(String... args) throws IOException, InterruptedException {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        //twitter streams must be kept alive, default header is close
        builder.setHeaderConnectionValue(HttpHeaders.Values.KEEP_ALIVE);
        builder
                .url("http://httpbin.org/post")
                .url("https://stream.twitter.com/1/statuses/sample.json")
//                .url("https://developers.facebook.com/docs/reference/api/")
                .basicAuth("zcourts", "dsgergr333333333g")
                .GET()
//              .cookie("username", "courtney")
//              .cookie("id", 3)
//              .cookie("postcode", "cr8 4hb")
//              .form("title", "some post field")
//              .form("desc", "a post field desc")
        //.file(new HttpFile("images", file))
        ;
        builder.build(new Function1<HTTPResponse>() {
            public void apply(HTTPResponse a) {
                a.readLine(new Function1<String>() {
                    public void apply(final String line) {
                        System.out.println("LINE:" + line);
                    }
                });
//                a.readAll(new Function1<String>() {
//                    public void apply(final String data) {
//                        System.out.println(data);
//                    }
//                });
            }
        });
    }
}
