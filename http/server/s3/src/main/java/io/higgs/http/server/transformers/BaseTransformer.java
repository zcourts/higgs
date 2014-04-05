package io.higgs.http.server.transformers;

import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.WebApplicationException;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class BaseTransformer implements ResponseTransformer {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected void setResponseContent(HttpResponse res, byte[] data) {
        if (data != null) {
            res.content().writeBytes(data);
            HttpHeaders.setContentLength(res, data.length);
        }
    }

    protected boolean isError(Object response) {
        return response instanceof Throwable;
    }

    protected void determineErrorStatus(HttpResponse res, Throwable response) {
        HttpResponseStatus status = null;
        if (response == null) {
            status = HttpResponseStatus.NO_CONTENT;
        } else if (response instanceof WebApplicationException) {
            WebApplicationException wae = ((WebApplicationException) response);
            status = wae.getStatus() == null ? HttpResponseStatus.INTERNAL_SERVER_ERROR : wae.getStatus();
        }
        res.setStatus(status);
    }

    @Override
    public int compareTo(ResponseTransformer that) {
        return that.priority() - this.priority();
    }
}
