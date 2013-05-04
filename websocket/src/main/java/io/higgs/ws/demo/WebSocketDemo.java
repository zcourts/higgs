package io.higgs.ws.demo;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.Transcription;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.ws.protocol.WebSocketConfiguration;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketDemo {
    private WebSocketDemo() {
    }

    public static void main(String... args) {
        WebSocketConfiguration ws = new WebSocketConfiguration();
        //re-write all requests to /app/* to index.html
        ws.getTranscriber().addTranscription(new Transcription("/app((?:\\/[\\w([^\\..]{1,4}\b)\\-]+)+)",
                "/index.html"));

        HiggsServer<HttpConfig> server = new HiggsServer<>("config.yml", HttpConfig.class);
        server.registerProtocol(ws);
        //
        server.registerClass(Api.class);
        server.start();
    }
}
