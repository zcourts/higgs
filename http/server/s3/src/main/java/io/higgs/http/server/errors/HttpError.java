package io.higgs.http.server.errors;

import javax.ws.rs.Path;

public class HttpError {
    @Path("404")
    public String notFound() {
        return "<h1>Not Found</h1>";
    }

    public static class HttpErrorCode {
    }

}
