package io.higgs.http.client;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface RetryPolicy {
    /**
     * Invoked when a request has failed, either because of a connection failure, the response was an error where
     * retries are allowed or another exception occurred.
     * <p/>
     * When the time comes, call the @{link #response.request().retry()} method...
     *
     * @param future         This is the future returned to the user which is to be notified when an error occurs or a
     *                       response is successfully received.
     * @param cause          if available, this will be the exception which caused the retry policy to be activated.
     * @param connectFailure if true, the connection failed to be established in the first place.
     *                       This is available to help distinguish between other types of errors so that a different
     *                       back off algorithm may be applied
     * @param response       this is the response object that was present when the failure occurred. This will never
     *                       be null, but it's contents may be. i.e. there may not be a status, say, if connection
     *                       failed; or there may not be a body, if connection was successful but some error caused
     *                       prevent the body from being read
     */
    void activate(FutureResponse future, Throwable cause, boolean connectFailure, Response response);
}
