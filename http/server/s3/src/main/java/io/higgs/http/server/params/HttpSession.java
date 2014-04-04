package io.higgs.http.server.params;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpSession extends HashMap<String, Object> implements Serializable {
    private final HashMap<String, Object> flash = new HashMap<>();
    private File sessionFile;

    private HttpSession() {
    }

    private HttpSession(File sessionFile) {
        this.sessionFile = sessionFile;
    }

    /**
     * Adds a key value pair to the session which is good for one use.
     * Once the object is retrieved it is automatically removed
     */
    public Object flash(String key, Object value) {
        Object o = flash.put(key, value);
        save();
        return o;
    }

    private void save() {
        if (sessionFile != null) {
            if (sessionFile.exists()) {
                sessionFile.delete();
            }
            try {
                FileOutputStream sessionStream = new FileOutputStream(sessionFile);
                ObjectOutputStream out = new ObjectOutputStream(sessionStream);
                out.writeObject(this);
            } catch (IOException ignored) {
                System.err.println("Unable to persist session to disk - " + ignored.getMessage());
            }
        }
    }

    @Override
    public Object put(String key, Object value) {
        Object o = super.put(key, value);
        save();
        return o;
    }

    @Override
    public Object get(Object key) {
        Object val = super.get(key);
        if (val == null) {
            val = flash.get(key);
            if (val != null) {
                flash.remove(key.toString());
            }
        }
        return val;
    }

    /**
     * @return Total combination of normal session objects and flash objects
     */
    public int getSize() {
        return size() + flash.size();
    }

    public static HttpSession newSession(String sessionId, String sessionDirName) {
        Path sessionDir = Paths.get(sessionDirName);
        File sessionFile = sessionDir.resolve(sessionId).toFile();
        if (sessionFile.exists()) {
            //load session from disk if exists
            try {
                FileInputStream in = new FileInputStream(sessionFile);
                ObjectInputStream oin = new ObjectInputStream(in);
                HttpSession session = (HttpSession) oin.readObject();
                if (session != null) {
                    return session;
                } else {
                    return new HttpSession(sessionFile);
                }
            } catch (Exception e) {
                return new HttpSession(sessionFile);
            }
        } else {
            if (!sessionDir.toFile().exists()) {
                try {
                    //create tmp directory including any non-existent parents
                    Files.createDirectories(sessionDir);
                } catch (IOException e) {
                    return new HttpSession();
                }
            }
            return new HttpSession(sessionFile);
        }
    }
}
