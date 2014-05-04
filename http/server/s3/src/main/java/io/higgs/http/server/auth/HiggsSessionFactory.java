package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.SimpleSessionFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSessionFactory extends SimpleSessionFactory implements SessionFactory {
    public Session createSession(SessionContext initData) {
        if (initData.getSessionId() != null) {
            return new HiggsSession(initData.getSessionId());
        }
        return super.createSession(initData);
    }
}
