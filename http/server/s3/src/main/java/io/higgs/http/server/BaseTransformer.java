package io.higgs.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class BaseTransformer implements ResponseTransformer {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public int compareTo(ResponseTransformer that) {
        return that.priority() - this.priority();
    }
}
