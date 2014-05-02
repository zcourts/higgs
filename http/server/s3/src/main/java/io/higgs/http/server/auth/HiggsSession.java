package io.higgs.http.server.auth;

import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SimpleSession;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSession extends SimpleSession {
    protected final SessionContext ctx;

    public HiggsSession(SessionContext ctx) {
        this.ctx = ctx;
        setId(ctx.getSessionId());
    }
}
