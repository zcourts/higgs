package io.higgs.http.server.transformers;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpServer;
import io.higgs.http.server.ResponseTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class BaseTransformer implements ResponseTransformer {
    public HttpResponse tryNextTransformer(final HttpServer server, Object returns, HttpRequest request,
                                           final PriorityBlockingQueue<ResponseTransformer> transformers) {
        //copy the transformer queue so removing an item doesn't remove it from the server
        PriorityBlockingQueue<ResponseTransformer> copied = new PriorityBlockingQueue<>(transformers);
        copied.remove(this);
        ArrayList<ResponseTransformer> arr = new ArrayList<>(copied);
        Collections.sort(arr, copied.comparator());
        for (ResponseTransformer transformer : arr) {
            if (transformer != this && transformer.canTransform(returns, request)) {
                transformer.transform(server, returns, request, copied);
            }
        }
        return null;
    }

    @Override
    public int compareTo(ResponseTransformer that) {
        return this.priority() - that.priority();
    }
}
