package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSessionManager extends DefaultSessionManager {
    public HiggsSession start(SessionContext context) {
        HiggsSession session = new HiggsSession(context.getSessionId());
        applyGlobalSessionTimeout(session);
        onStart(session, context);
        notifyStart(session);
        return session;
    }

    public HiggsSession getSession(SessionKey key) throws SessionException {
        Session session = super.getSession(key);
        if (session instanceof HiggsSession) {
            return (HiggsSession) session;
        }
        HiggsSession s = new HiggsSession(key.getSessionId());
        s.fromSession(session);
        return s;
    }
}
