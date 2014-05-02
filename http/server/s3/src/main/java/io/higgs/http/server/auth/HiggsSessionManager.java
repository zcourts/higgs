package io.higgs.http.server.auth;

import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSessionManager extends DefaultSessionManager {
    public HiggsSession start(SessionContext context) {
        HiggsSession session = new HiggsSession(context);
        applyGlobalSessionTimeout(session);
        onStart(session, context);
        notifyStart(session);
        return session;
    }
}
