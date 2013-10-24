package io.higgs.http.server.resource;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class JsonData {
    protected final JsonNode node;
    protected final String json;

    public JsonData(String json, JsonNode node) {
        this.json = json;
        this.node = node;
    }

    public JsonNode getNode() {
        return node;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return json;
    }
}
