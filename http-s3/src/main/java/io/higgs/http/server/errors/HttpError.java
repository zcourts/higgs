package io.higgs.http.server.errors;

import io.higgs.core.method;

public class HttpError {
    public static class HttpErrorCode {
    }

    @method("404")
    public String notFound() {
        return "<h1>Not Found</h1>";
    }

}
