package com.fillta.higgs.http.client.demo;

import com.fillta.functional.Function1;
import com.fillta.higgs.http.client.HTTPResponse;
import com.fillta.higgs.http.client.HttpRequestBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Demo {
	public static void main(String... args) throws IOException, InterruptedException {
//		File file = Files.createTempFile("higgs.test", ".tmp").toFile();
//		new BufferedOutputStream(new FileOutputStream(file)).write("Some random text".getBytes());
//		ArrayList<PartialHttpFile> files = new ArrayList();
//		files.add(new PartialHttpFile(file));
//		files.add(new PartialHttpFile(file));
		Thread.sleep(30000);
		Map params = new HashMap<String, Object>();
		params.put("p", 1);
		params.put("p", 2);
		params.put("p2", "abc");
		HttpRequestBuilder builder = new HttpRequestBuilder();
		final int[] count = {0};
		for (int i = 0; i < 100000; i++) {
			builder
//                .url("http://httpbin.org/get")
//                .GET()
//                .query("a", 1)
//                .query(params)
//                .cookie("cookie", 1)
//                .cookies(params)
//                .build(new Function1<HTTPResponse>() {
//                    public void apply(HTTPResponse a) {
//                        System.out.println(a);
//                    }
//                })
//                .url("http://httpbin.org/delete")
//                .DELETE()
//                .build(new Function1<HTTPResponse>() {
//                    public void apply(HTTPResponse a) {
//                        System.out.println(a);
//                    }
//                })
					.url("http://httpbin.org/post")
//                .url("http://stuweb.cms.gre.ac.uk/~rc909/wad/item/create")
					.url("http://localhost:8080")
					.url("http://fillta.com/")
					.GET()
					.cookie("username", "courtney")
					.cookie("id", 3)
					.cookie("postcode", "cr8 4hb")
					.form("title", "hacked upload")
					.form("desc", "hacked upload desc")
//					.file(new HttpFile("images", file))
					.build(new Function1<HTTPResponse>() {
						public void apply(HTTPResponse a) {
							System.out.println(++count[0]);
						}
					});
		}
	}
}
