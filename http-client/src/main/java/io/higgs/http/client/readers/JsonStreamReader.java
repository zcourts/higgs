package io.higgs.http.client.readers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonStreamReader extends Reader<String> {
    protected JsonFactory factory;
    protected JsonParser parser;

    public JsonStreamReader() {
        factory = new JsonFactory();
        try {
            parser = factory.createParser(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        parser.read
    }

    @Override
    public void done() {

    }
}
