package io.higgs.ws.demo;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.ws.protocol.WebSocketConfiguration;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketDemo {
    private WebSocketDemo() {
    }

    public static void main(String... args) {
        //handles HTTP GET requests
        WebSocketConfiguration ws = new WebSocketConfiguration();
        //handles all other HTTP requests
        HttpProtocolConfiguration http = new HttpProtocolConfiguration();
        //re-write all requests to /app/* to index.html
        ws.getTranscriber().addTranscription(new Transcription("/app((?:\\/[\\w([^\\..]{1,4}\b)\\-]+)+)",
                "/index.html"));

        HiggsServer server = new HiggsServer().setConfig("config.yml", HttpConfig.class);
        server.registerProtocol(ws);
        //HTTP must be registered after WebSockets
        server.registerProtocol(http);
        //
        server.registerClass(Api.class);
        server.start();
    }
}
