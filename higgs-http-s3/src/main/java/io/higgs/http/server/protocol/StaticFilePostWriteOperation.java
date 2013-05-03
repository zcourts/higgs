package io.higgs.http.server.protocol;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface StaticFilePostWriteOperation {
    void apply();

    boolean isDone();
}
