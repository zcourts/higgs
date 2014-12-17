package io.higgs.http.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.higgs.core.func.Function2;
import io.higgs.http.client.readers.PageReader;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class JSONRequestTest {
    URI uri;
    PageReader reader = new PageReader(new Function2<String, Response>() {
        @Override
        public void apply(String s, Response response) {
        }
    });
    Map map = new HashMap();

    public JSONRequestTest() throws URISyntaxException {
        uri = new URI("http://httpbin.org");
    }

    @Test(expected = IllegalStateException.class)
    public void throwIllegalStateIfAddFieldCalledAfterSetDataStr() throws JsonProcessingException {
        HttpRequestBuilder.instance().postJSON(uri, reader)
                .setData(map)
                        //can't combine setData with addField
                .addField("a", "b");
    }

    @Test(expected = IllegalStateException.class)
    public void throwIllegalStateIfAddFieldCalledAfterSetDataObj() throws JsonProcessingException {
        HttpRequestBuilder.instance().postJSON(uri, reader)
                .setData("{}")
                        //can't combine setData with addField
                .addField("a", "b");
    }

    @Test(expected = IllegalStateException.class)
    public void throwIllegalStateIfAddFieldCalledBeforeSetDataStr() throws JsonProcessingException {
        HttpRequestBuilder.instance().postJSON(uri, reader)
                .addField("a", "b")
                        //can't combine setData with addField
                .setData(map);
    }

    @Test(expected = IllegalStateException.class)
    public void throwIllegalStateIfAddFieldCalledBeforeSetDataObj() throws JsonProcessingException {
        HttpRequestBuilder.instance().postJSON(uri, reader)
                .addField("a", "b")
                        //can't combine setData with addField
                .setData("{}");
    }
}
