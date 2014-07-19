package io.higgs.http.server.auth;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.config.HttpConfig;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authc.pam.AuthenticationStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSecurityManager {
    protected static Logger log = LoggerFactory.getLogger(HiggsSecurityManager.class);
    protected static DefaultSecurityManager securityManager;
    protected static HiggsSessionManager sessionManager = new HiggsSessionManager();
    protected static HttpConfig config;

    protected HiggsSecurityManager() {
    }

    public static void configure(HiggsServer server, DefaultSecurityManager securityManager) {
        HiggsSecurityManager.securityManager = securityManager;
        config = server.getConfig();
        setupSessions();
        setupRealms();
        seupAuthenticationStrategy();
        setupAuthorization();
    }

    protected static void setupSessions() {
        sessionManager.setSessionFactory(new HiggsSessionFactory());
        securityManager.setSessionManager(sessionManager);
        Set<SessionDAO> sessionDAO = getServices(SessionDAO.class);
        if (sessionDAO.size() > 0) {
            Iterator<SessionDAO> it = sessionDAO.iterator();
            SessionDAO dao = it.next();
            sessionManager.setSessionDAO(dao);
            if (it.hasNext()) {
                log.warn(String.format("Multiple SessionDAO configured, ONLY using %s", dao.getClass().getName()));
            }
        } else {
            sessionManager.setSessionDAO(new DefaultHiggsSessionDAO(config.session_dir));
        }
    }

    protected static void setupRealms() {
        Set<Realm> realms = getServices(Realm.class);
        if (realms.size() > 0) {
            // if realms are found they probably came from the config, keep them in addition to the ones discovered
            //note that the ones discovered via SPI takes precedence
            if (securityManager.getRealms() != null) {
                realms.addAll(securityManager.getRealms());
            }
            securityManager.setRealms(realms);
        } else if (securityManager.getRealms() == null || securityManager.getRealms().size() == 0) {
            log.info("No Realm services setup on the class path, this means if authorization or authentication is " +
                    "configured they may not work as expected");
        }
    }

    protected static void seupAuthenticationStrategy() {
        Set<AuthenticationStrategy> authenticators = getServices(AuthenticationStrategy.class);
        if (authenticators.size() > 0) {
            ModularRealmAuthenticator mod = new ModularRealmAuthenticator();
            Authenticator auth = securityManager.getAuthenticator();
            if (auth instanceof ModularRealmAuthenticator) {
                mod = (ModularRealmAuthenticator) auth;
            } else {
                securityManager.setAuthenticator(mod);
            }
            Iterator<AuthenticationStrategy> it = authenticators.iterator();
            AuthenticationStrategy strategy = it.next();
            mod.setAuthenticationStrategy(strategy);
            if (it.hasNext()) {
                log.warn(String.format("Multiple authentication strategies found, only using the first one which is" +
                        " %s ", strategy.getClass().getName()));
            }
        } else {
            log.info("No authentication service setup on the class path");
        }
    }

    protected static void setupAuthorization() {
        Set<Authorizer> authorizers = getServices(Authorizer.class);
        if (authorizers.size() > 0) {
            Iterator<Authorizer> it = authorizers.iterator();
            Authorizer auth = it.next();
            securityManager.setAuthorizer(auth);
            if (it.hasNext()) {
                log.warn(String.format("Multiple authorizers configured, ONLY %s is being used",
                        auth.getClass().getName()));
            }
        } else if (securityManager.getAuthorizer() == null) {
            log.info("No authorization service setup on the class path");
        }
    }

    protected static <T> Set<T> getServices(Class<T> klass) {
        Iterator<T> providers = ServiceLoader.load(klass).iterator();
        HashSet<T> services = new HashSet<>();
        while (providers.hasNext()) {
            try {
                services.add(providers.next());
            } catch (ServiceConfigurationError sce) {
                log.warn("Unable to register Realm", sce);
            }
        }
        return services;
    }

}
