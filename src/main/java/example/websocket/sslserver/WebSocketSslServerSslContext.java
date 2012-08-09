package example.websocket.sslserver;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Creates a {@link SSLContext} for just server certificates.
 */
public final class WebSocketSslServerSslContext {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketSslServerSslContext.class);
	private static final String PROTOCOL = "TLS";
	private SSLContext _serverContext;

	/**
	 * Returns the singleton instance for this class
	 */
	public static WebSocketSslServerSslContext getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to
	 * SingletonHolder.INSTANCE, not before.
	 *
	 * See http://en.wikipedia.org/wiki/Singleton_pattern
	 */
	private static class SingletonHolder {

		public static final WebSocketSslServerSslContext INSTANCE = new WebSocketSslServerSslContext();
	}

	/**
	 * Constructor for singleton
	 */
	private WebSocketSslServerSslContext() {
		try {
			// Key store (Server side certificate)
			String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
			if (algorithm == null) {
				algorithm = "SunX509";
			}

			SSLContext serverContext;
			try {
				String keyStoreFilePath = System.getProperty("keystore.file.path");
				String keyStoreFilePassword = System.getProperty("keystore.file.password");

				KeyStore ks = KeyStore.getInstance("JKS");
				FileInputStream fin = new FileInputStream(keyStoreFilePath);
				ks.load(fin, keyStoreFilePassword.toCharArray());

				// Set up key manager factory to use our key store
				// Assume key password is the same as the key store file
				// password
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
				kmf.init(ks, keyStoreFilePassword.toCharArray());

				// Initialise the SSLContext to work with our key managers.
				serverContext = SSLContext.getInstance(PROTOCOL);
				serverContext.init(kmf.getKeyManagers(), null, null);
			} catch (Exception e) {
				throw new Error("Failed to initialize the server-side SSLContext", e);
			}
			_serverContext = serverContext;
		} catch (Exception ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Error initializing SslContextManager. " + ex.getMessage(), ex);
			}
			System.exit(1);

		}
	}

	/**
	 * Returns the server context with server side key store
	 */
	public SSLContext getServerContext() {
		return _serverContext;
	}
}
