package io.higgs.examples.httpServer.restService;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.Transcription;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public final class Demo {
    private Demo() {
    }

    public static void main(String... args) {

        HttpProtocolConfiguration http = new HttpProtocolConfiguration();
        //re-write all requests to /app/* to index.html
        http.getTranscriber().addTranscription(new Transcription("/app((?:\\/[\\w([^\\..]{1,4}\b)\\-]+)+)",
                "/index.html"));
        HiggsServer server = new HiggsServer().setConfig("config.yml", HttpConfig.class);
        server.registerProtocol(http);
        //
        server.registerPackage(Api.class.getPackage());
//        server.registerClass(Api.class);
//        server.registerObject(new Api());
        server.start();
    }
}
