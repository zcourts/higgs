package io.higgs.http.server;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public enum TransformerType {
    /**
     * When a transformer can handle normal responses where no exceptions have been raised
     */
    GENERIC,
    /**
     * When a transformer can handle exceptions and convert them to proper HTML/JSON responses
     */
    ERROR
}
