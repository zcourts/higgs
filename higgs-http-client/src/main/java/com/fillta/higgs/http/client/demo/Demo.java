package com.fillta.higgs.http.client.demo;

import com.fillta.higgs.http.client.HTTPResponse;
import com.fillta.higgs.http.client.HttpFile;
import com.fillta.higgs.http.client.HttpRequestBuilder;
import com.fillta.higgs.http.client.PartialHttpFile;
import com.fillta.higgs.util.Function1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Demo {
    public static void main(String... args) throws IOException {
//        File file = Files.createTempFile("higgs.test", ".tmp").toFile();
        File file = new File("B:\\dev\\projects\\Higgs\\higgs-core\\src\\main\\resources\\ordering and visibility.png");
        ArrayList<PartialHttpFile> files = new ArrayList();
        files.add(new PartialHttpFile(file));
        files.add(new PartialHttpFile(file));
        Map params = new HashMap<String, Object>();
        params.put("p", 1);
        params.put("p", 2);
        params.put("p2", "abc");
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder
//                .url("http://httpbin.org/get")
//                .GET()
//                .query("a", 1)
//                .query(params)
//                .cookie("cookie", 1)
//                .cookies(params)
//                .build(new Function1<HTTPResponse>() {
//                    public void call(HTTPResponse a) {
//                        System.out.println(a);
//                    }
//                })
//                .url("http://httpbin.org/delete")
//                .DELETE()
//                .build(new Function1<HTTPResponse>() {
//                    public void call(HTTPResponse a) {
//                        System.out.println(a);
//                    }
//                })
                .url("http://httpbin.org/post")
//                .url("http://stuweb.cms.gre.ac.uk/~rc909/wad/item/create")
                .POST()
                .cookie("username", "courtney")
                .cookie("id", 3)
                .cookie("postcode", "cr8 4hb")
                .form("title", "hacked upload")
                .form("desc", "hacked upload desc")
                .file(new HttpFile("images", file))
                .build(new Function1<HTTPResponse>() {
                    public void call(HTTPResponse a) {
                        System.out.println(a);
                    }
                });
    }
}
