package io.higgs.http.server.providers.handlebars;

import com.github.jknack.handlebars.Helper;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface HandlebarHelper<T> extends Helper<T> {
    String getName();
}
