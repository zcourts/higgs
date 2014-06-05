package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.MemorySessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultHiggsSessionDAO extends MemorySessionDAO implements SessionDAO {
    protected Map<Serializable, Session> sessions = new HashMap<>();

    public DefaultHiggsSessionDAO(String sessionDirName) {
    }

    @Override
    public Serializable create(Session session) {
        createOrUpdateSession(session);
        return session.getId();
    }

    private boolean createOrUpdateSession(Session session) {
        sessions.put(session.getId(), session);
        return false;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        Session session = sessions.get(sessionId);
        if (session == null || session.getId() == null || session.getId().toString().isEmpty()) {
            throw new UnknownSessionException();
        }
        return session;
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        createOrUpdateSession(session);
    }

    @Override
    public void delete(Session session) {
        sessions.remove(session.getId());
    }

    @Override
    public Collection<Session> getActiveSessions() {
        return sessions.values();
    }
}
