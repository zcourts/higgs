package com.fillta.higgs.http.client.demo;

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
