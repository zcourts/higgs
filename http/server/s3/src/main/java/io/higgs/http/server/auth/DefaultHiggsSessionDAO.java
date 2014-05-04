package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.MemorySessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultHiggsSessionDAO extends MemorySessionDAO implements SessionDAO {
    protected final Path sessionDir;
    private Logger log = LoggerFactory.getLogger(getClass());

    public DefaultHiggsSessionDAO(String sessionDirName) {
        sessionDir = Paths.get(sessionDirName == null ? "/tmp/hs3-sessions" : sessionDirName);
        if (!sessionDir.toFile().exists()) {
            try {
                //create session directory including any non-existent parents
                Files.createDirectories(sessionDir);
            } catch (IOException e) {
                throw new InvalidSessionDirectory("The configured session directory could not be created", e);
            }
        }
        //find old sessions and delete any that cannot be ready anymore, typically because of binary incompatibility
        //after recompiling classes that have been previously serialized
        getActiveSessions();
    }

    @Override
    public Serializable create(Session session) {
        if (createOrUpdateSession(session)) {
            return super.create(session);
        }
        return session.getId();
    }

    private boolean createOrUpdateSession(Session session) {
        if (!(session instanceof Serializable)) {
            return true;
        }

        Path sessionPath = sessionDir.resolve(session.getId().toString());
        File sessionFile = sessionPath.toFile();
        if (!sessionFile.exists()) {
            boolean created;
            IOException e = null;
            try {
                created = sessionFile.createNewFile();
            } catch (IOException x) {
                created = false;
                e = x;
            }
            if (!created) {
                throw new IllegalStateException("Failed to create session file", e);
            }
        }
        writeSession(session, sessionFile);
        return false;
    }

    private void writeSession(Session session, File sessionFile) {
        try {
            FileOutputStream out = new FileOutputStream(sessionFile);
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            outputStream.writeObject(session);
        } catch (FileNotFoundException e) {
            //should never happen because we raise error above if the file isn't created
            throw new IllegalStateException("Unable to find session data", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save session data", e);
        }
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        Path sessionPath = sessionDir.resolve(sessionId.toString());
        if (!sessionPath.toFile().exists()) {
            throw new UnknownSessionException("The given session doesn't exist");
        }
        return readSession(sessionPath);
    }

    private Session readSession(Path sessionPath) {
        try {
            FileInputStream in = new FileInputStream(sessionPath.toFile());
            ObjectInputStream inputStream = new ObjectInputStream(in);
            return (Session) inputStream.readObject();
        } catch (FileNotFoundException e) {
            //should never happen because we raise error above if the file isn't created
            throw new IllegalStateException("Unable to find session data", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save session data", e);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new UnknownSessionException("Failed to load session");
        }
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        if (createOrUpdateSession(session)) {
            super.create(session);
        }
    }

    @Override
    public void delete(Session session) {
        Path sessionPath = sessionDir.resolve(session.getId().toString());
        if (sessionPath.toFile().exists()) {
            sessionPath.toFile().delete();
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        Set<Session> sessions = new HashSet<>();
        try {
            DirectoryStream<Path> files = Files.newDirectoryStream(sessionDir);
            for (Path sessionFile : files) {
                try {
                    sessions.add(readSession(sessionFile));
                } catch (Exception e) {
                    sessionFile.toFile().delete();
                }
            }
        } catch (IOException e) {
            log.info("Failed to get active sessions");
        }
        return sessions;
    }
}
