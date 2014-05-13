package io.higgs.http.server.auth;

import io.higgs.boson.serialization.v1.BosonReader;
import io.higgs.boson.serialization.v1.BosonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultHiggsSessionDAO extends MemorySessionDAO implements SessionDAO {
    protected final Path sessionDir;
    private Logger log = LoggerFactory.getLogger(getClass());
    protected Map<Serializable, Session> sessions = new HashMap<>();
    protected static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public DefaultHiggsSessionDAO(String sessionDirName) {
        sessionDir = Paths.get(sessionDirName == null ? "/tmp/hs3-sessions" : sessionDirName);
        ensurePathExists();
        //find old sessions and delete any that cannot be ready anymore, typically because of binary incompatibility
        //after recompiling classes that have been previously serialized
        getActiveSessions();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Serializable, Session> e : sessions.entrySet()) {
                    writeSession(e.getValue());
                }
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    protected void ensurePathExists() {
        if (!sessionDir.toFile().exists()) {
            try {
                //create session directory including any non-existent parents
                Files.createDirectories(sessionDir);
            } catch (IOException e) {
                throw new InvalidSessionDirectory("The configured session directory could not be created", e);
            }
        }
    }

    @Override
    public Serializable create(Session session) {
        createOrUpdateSession(session);
        //when we create we should flush straight away
        writeSession(session);
        return session.getId();
    }

    private boolean createOrUpdateSession(Session session) {
        if (!(session instanceof Serializable)) {
            return true;
        }
        sessions.put(session.getId(), session);
        return false;
    }

    private void writeSession(Session session) {
        if (session == null || session.getId() == null || session.getId().toString().isEmpty()) {
            return;
        }
        ensurePathExists();
        Path sessionPath = sessionDir.resolve(session.getId().toString());
        File sessionFile = sessionPath.toFile();
        if (sessionFile.exists()) {
            sessionFile.delete();
        }
        boolean created;
        try {
            created = sessionFile.createNewFile();
        } catch (IOException x) {
            created = false;
            log.debug("Failed to create new session file", x);
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(sessionFile);
        } catch (FileNotFoundException e) {
            //should never happen because we raise error above if the file isn't created
            throw new IllegalStateException("Unable to find session data", e);
        }
        try {
            if (created) {
                BosonWriter writer = new BosonWriter();
                ByteBuf msg = writer.serialize(session).slice();
                out.write(msg.array());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save session data", e);
        } finally {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                log.warn("Failed to flush session data to disk", e);
            }
        }
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        Path sessionPath = sessionDir.resolve(sessionId.toString());
        if (!sessionPath.toFile().exists()) {
            throw new UnknownSessionException();
//            session = new HiggsSession(sessionId);
//            writeSession();
        }
        Session session = readSession(sessionPath);
        if (session == null || session.getId() == null || session.getId().toString().isEmpty()) {
            throw new UnknownSessionException();
//            session = new HiggsSession(sessionId);
//            writeSession();
        }
        return session;
    }

    private HiggsSession readSession(Path sessionPath) {
        FileInputStream in = null;
        try {
            File f = sessionPath.toFile();
            in = new FileInputStream(f);
            BosonReader reader = new BosonReader();
            byte[] data = new byte[(int) f.length()];
            in.read(data);
            return reader.deSerialize(Unpooled.wrappedBuffer(data));
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Error closing session file", e);
                }
            }
        }
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        createOrUpdateSession(session);
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
