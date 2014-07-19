package io.higgs.examples.httpClient;

import io.higgs.core.func.Function2;
import io.higgs.http.client.HttpFile;
import io.higgs.http.client.HttpRequestBuilder;
import io.higgs.http.client.Request;
import io.higgs.http.client.Response;
import io.higgs.http.client.readers.FileReader;
import io.higgs.http.client.readers.LineReader;
import io.higgs.http.client.readers.PageReader;
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

    public static void main(String[] args) throws Exception {
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
                .withSSLProtocols(new String[]{ "SSLv3", "TLSv1" });
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
