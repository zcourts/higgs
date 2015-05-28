package io.higgs.examples.httpClient;

import io.higgs.core.func.Function1;
import io.higgs.core.func.Function2;
import io.higgs.http.client.FutureResponse;
import io.higgs.http.client.HTTPStreamingRequest;
import io.higgs.http.client.HttpFile;
import io.higgs.http.client.HttpRequestBuilder;
import io.higgs.http.client.Request;
import io.higgs.http.client.Response;
import io.higgs.http.client.RetryPolicy;
import io.higgs.http.client.readers.FileReader;
import io.higgs.http.client.readers.LineReader;
import io.higgs.http.client.readers.PageReader;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Files;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public final class Demo {
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

    public static void main(final String[] args) throws Exception {
        URI url = new URI("https://127.0.0.1:9999/a04e8ed9b1214978a45e568f68a22a2b");

        final HTTPStreamingRequest str = HttpRequestBuilder.instance().streamJSON(url,
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, Response response) {
                        System.out.println(s);
                    }
                }));
        str.header("Auth", args.length >= 3 ? args[2] : "drew:5d080caac4892626a9dc659cf3a25c15")
                //SSLv3, TLSv1, TLSv1.1, TLSv1.2 are typical
                .withSSLProtocols(new String[]{"TLSv1"})
                .policy(new RetryPolicy() {
                    int connectBackOff, connectMax = 10000, backOff = 1000, backOffMax = 10000,
                            retries, maxRetries = 10;

                    @Override
                    public void activate(FutureResponse future, Throwable cause,
                                         boolean connectFailure, Response response) {
                        if (retries >= maxRetries) {
                            response.markFailed(cause);
                            future.setFailure(cause);
                            return;
                        }
                        retries++;
                        if (connectFailure) { //linear back off for connection failure
                            try {
                                response.request().retry();
                                Thread.sleep(connectBackOff);
                            } catch (InterruptedException ignored) {
                                System.out.println("Retry wait interrupted");
                            } finally {
                                if (connectBackOff <= connectMax) {
                                    connectBackOff += 1000;
                                } else {
                                    connectBackOff = 0;
                                }
                            }
                        } else {
                            response.request().retry();
                            try {
                                //exponential back off in all other cases
                                Thread.sleep(backOff);
                            } catch (InterruptedException ignored) {
                                System.out.println("Retry wait interrupted");
                            } finally {
                                if (backOff <= backOffMax) {
                                    backOff *= 2;
                                } else {
                                    backOff = 0;
                                }
                            }
                        }
                    }
                });
        //start the connection
        FutureResponse res = str.execute();
        str.getChannel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("Connection closed");
            }
        });
        str.onReady(new Function1<HTTPStreamingRequest.StreamSender>() {
            @Override
            public void apply(HTTPStreamingRequest.StreamSender sender) {
                while (str.getChannel().isWritable()) {
                    try {
                        sender.send("{\"id_str\":\"164768123994640385\",\"place\":null,\"text\":\"@penpen_mne \\u304b\\u308c\\u3053\\u308c10\\u56de\\u76ee\\u3067\\u3059\",\"created_at\":\"Wed Feb 01 17:52:36 +0000 2012\",\"in_reply_to_user_id_str\":\"252032529\",\"coordinates\":null,\"contributors\":null,\"geo\":null,\"source\":\"\\u003Ca href=\\\"http:\\/\\/www.echofon.com\\/\\\" rel=\\\"nofollow\\\"\\u003EEchofon\\u003C\\/a\\u003E\",\"retweet_count\":0,\"favorited\":false,\"in_reply_to_screen_name\":\"penpen_mne\",\"in_reply_to_user_id\":252032529,\"in_reply_to_status_id\":164755622917713920,\"user\":{\"id_str\":\"446266509\",\"default_profile\":true,\"created_at\":\"Sun Dec 25 14:08:50 +0000 2011\",\"contributors_enabled\":false,\"profile_use_background_image\":true,\"following\":null,\"profile_text_color\":\"333333\",\"profile_background_image_url\":\"http:\\/\\/a0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"followers_count\":110,\"url\":null,\"profile_image_url\":\"http:\\/\\/a2.twimg.com\\/profile_images\\/1758888780\\/PILE_RiS_normal.jpg\",\"description\":\"My name is...P!!!\\nfrom SENDAI\\nRING in SOUND\\nCAUTION TAPE\\nCLUB SHAFT\",\"default_profile_image\":false,\"show_all_inline_media\":false,\"profile_link_color\":\"0084B4\",\"favourites_count\":0,\"screen_name\":\"PILE_RiS\",\"time_zone\":null,\"profile_background_color\":\"C0DEED\",\"location\":\"\",\"geo_enabled\":false,\"profile_background_tile\":false,\"protected\":false,\"statuses_count\":65,\"verified\":false,\"profile_background_image_url_https\":\"https:\\/\\/si0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"profile_sidebar_fill_color\":\"DDEEF6\",\"name\":\"PILE of the DEAD\",\"listed_count\":0,\"notifications\":null,\"profile_image_url_https\":\"https:\\/\\/si0.twimg.com\\/profile_images\\/1758888780\\/PILE_RiS_normal.jpg\",\"id\":446266509,\"is_translator\":false,\"follow_request_sent\":null,\"lang\":\"ja\",\"utc_offset\":null,\"friends_count\":131,\"profile_sidebar_border_color\":\"C0DEED\"},\"retweeted\":false,\"truncated\":false,\"id\":164768123994640385,\"in_reply_to_status_id_str\":\"164755622917713920\",\"entities\":{\"hashtags\":[],\"user_mentions\":[{\"id_str\":\"252032529\",\"indices\":[0,11],\"screen_name\":\"penpen_mne\",\"name\":\"\\u307a\\u3093\\u307a\\u3093\",\"id\":252032529}],\"urls\":[]}}\n");
//                        System.out.println("Complete : " + future.isSuccess());
//                        System.out.println("Progressed : " + future.isSuccess() + " : current = " + progress + " : total = " + total);

//                        sender.send("\n").addListener(new GenericFutureListener<ChannelFuture>() {
//                            @Override
//                            public void operationComplete(ChannelFuture future) throws Exception {
////                                System.out.println("Progressed : " + future.isSuccess() + " : current = " + progress + " : total = " + total);
//                                System.out.println("Complete : " + future.isSuccess());
//                            }
//                        });
                        System.out.println("Sent a new interaction");
                    } catch (Exception e) {
                        System.out.println("Boom");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep((args.length >= 1 ? Integer.parseInt(args[1]) : 1000));
                    } catch (InterruptedException ignored) {
                        ignored.printStackTrace();
                    }
                }
                System.out.println("Connection closed... channel no longer writable");
                HttpRequestBuilder.shutdown();
            }
        });
        res.addListener(new GenericFutureListener<Future<? super Response>>() {
            @Override
            public void operationComplete(Future<? super Response> future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("Future success");
                } else {
                    System.out.println("Future failed");
                    future.cause().printStackTrace();
                }
            }
        });
        boolean opt = true;
        if (opt) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });

        Request req3 = HttpRequestBuilder.instance().GET(new URI("https://api.datasift.com/v1.1/dpu"),
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, final Response response) {
                        System.out.println(s);
                        HttpRequestBuilder.shutdown();
                    }
                })
        )
                //SSLv3, TLSv1, TLSv1.1, TLSv1.2 are typical
                .withSSLProtocols(new String[]{"SSLv3", "TLSv1"});
        //can always check what options are supported with
        HttpRequestBuilder.getSupportedSSLProtocols();
        //check if a specific version is supported
        HttpRequestBuilder.isSupportedSSLProtocol("SSLv3");

        req3.execute();
        //automatically follow redirects
        //disable redirect
        HttpRequestBuilder clone = defaults.copy();
        // could remove all redirect statuses with copy.redirectOn().clear();
        Request req = clone.GET(new URI("http://httpbin.org/relative-redirect/1"),
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, final Response response) {
                        System.out.println(s);
                    }
                })
        );
        req.execute();
        Request r = clone.GET(new URI("http://httpbin.org/redirect/1"),
                new PageReader(new Function2<String, Response>() {
                    public void apply(String s, Response response) {
                        System.out.println(s);
                        System.out.println(response);
                    }
                })
        );
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
