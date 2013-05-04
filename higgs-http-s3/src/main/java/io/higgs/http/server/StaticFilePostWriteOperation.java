package io.higgs.http.server;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface StaticFilePostWriteOperation {
    void apply();

    boolean isDone();
}
