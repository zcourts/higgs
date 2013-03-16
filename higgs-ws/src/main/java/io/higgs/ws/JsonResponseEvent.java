package io.higgs.ws;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface JsonResponseEvent {
    /**
     * Get the name of the client callback
     *
     * @return
     */
    String getCallback();

    /**
     * The callback function that will receive responses...if any
     *
     * @param callback
     */
    void setCallback(String callback);

    Object getMessage();

    void setMessage(Object o);

}
