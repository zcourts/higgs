package com.fillta.higgs.http.server.transformers;

import com.fillta.higgs.http.server.HttpRequest;
import com.fillta.higgs.http.server.HttpResponse;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.ResponseTransformer;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class BaseTransformer implements ResponseTransformer {
    public HttpResponse tryNextTransformer(final HttpServer server, Object returns, HttpRequest request,
                                           final Queue<ResponseTransformer> transformers) {
        //copy the transformer queue so removing an item doesn't remove it from the server
        LinkedBlockingDeque<ResponseTransformer> copied = new LinkedBlockingDeque<>(transformers);
        copied.remove(this);
        Iterator<ResponseTransformer> it = copied.descendingIterator();
        while (it.hasNext()) {
            ResponseTransformer transformer = it.next();
            if (transformer != this && transformer.canTransform(returns, request)) {
                transformer.transform(server, returns, request, copied);
            }
        }
        return null;
    }

}
