package io.higgs.http.server.auth;

import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class InvalidSessionDirectory extends RuntimeException {
    public InvalidSessionDirectory(String msg, IOException e) {
        super(msg, e);
    }
}
