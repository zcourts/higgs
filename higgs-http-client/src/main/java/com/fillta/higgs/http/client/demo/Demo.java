package com.fillta.higgs.http.client.demo;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.http.client.HTTPResponse;
import com.fillta.higgs.http.client.HttpRequestBuilder;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.util.Scanner;

public class Demo {
	public static void main(String... args) throws IOException, InterruptedException {
		OAuthService service = new ServiceBuilder()
				.provider(TwitterApi.class)
				.apiKey("6icbcAXyZx67r8uTAUM5Qw")
				.apiSecret("SCCAdUUc6LXxiazxH3N0QfpNUvlUy84mZ2XZKiv39s")
				.build();
		Scanner in = new Scanner(System.in);

		System.out.println("=== Twitter's OAuth Workflow ===");
		System.out.println();

		// Obtain the Request Token
		System.out.println("Fetching the Request Token...");
		Token requestToken = service.getRequestToken();
		System.out.println("Got the Request Token!");
		System.out.println();

		System.out.println("Now go and authorize Scribe here:");
		System.out.println(service.getAuthorizationUrl(requestToken));
		System.out.println("And paste the verifier here");
		System.out.print(">>");
		Verifier verifier = new Verifier(in.nextLine());
		System.out.println();

		HttpRequestBuilder builder = new HttpRequestBuilder();
		builder.getRequester().onException(new ChannelEventListener() {
			public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
				ex.get().printStackTrace();
			}
		});
		builder
				.form("a", "")
				.oAuth1(TwitterApi.class, "me0HrzWNedYOSX4pTK6adg",
						"wOc9XXS5FIPQsHiDrqGMl8Fnz1EFhLyocF4pfz6hzE")
				.oAuth1RequestToken(new Function1<HTTPResponse>() {
					public void apply(final HTTPResponse a) {
						System.out.println(a);
					}
				});
	}

	public static void main1(String... args) throws IOException, InterruptedException {
		HttpRequestBuilder builder = new HttpRequestBuilder();
		builder.oAuth1(TwitterApi.class, "me0HrzWNedYOSX4pTK6adg",
				"wOc9XXS5FIPQsHiDrqGMl8Fnz1EFhLyocF4pfz6hzE");
		builder
				.url("http://httpbin.org/post")
				.url("https://stream.twitter.com/1/statuses/sample.json")
				.url("https://developers.facebook.com/docs/reference/api/")
//				.basicAuth("zcourts", "!1dinebk6N")
				.GET()
//				.cookie("username", "courtney")
//				.cookie("id", 3)
//				.cookie("postcode", "cr8 4hb")
//				.form("title", "some post field")
//				.form("desc", "a post field desc")
		//.file(new HttpFile("images", file))
		;
		final int[] count = {0};
//		for (int i = 0; i < 100000; i++)
		builder.build(new Function1<HTTPResponse>() {
			public void apply(HTTPResponse a) {
//				System.out.println(++count[0]);
//					System.out.println(a);
				a.readLine(new Function1<String>() {
					public void apply(final String line) {
						System.out.println("LINE:" + line);
					}
				});
//						a.readAll(new Function1<String>() {
//							public void apply(final String data) {
//								System.out.println(data);
//							}
//						});
			}
		});
	}
}
