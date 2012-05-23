package info.crlog.higgs.ssl;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Bogus {@link TrustManagerFactorySpi} which accepts any certificate even if it
 * is invalid.
 */
public class TrustManagerFactory extends TrustManagerFactorySpi {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(TrustManagerFactory.class);
    private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(
                X509Certificate[] chain, String authType) throws CertificateException {
            // Always trust - it is an example.
            // You should do something in the real world.
            // You will reach here only if you enabled client certificate auth,
            // as described in securechat.SecureChatSslContextFactory.
            logger.error(
                    "UNKNOWN CLIENT CERTIFICATE: " + chain[0].getSubjectDN());
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] chain, String authType) throws CertificateException {
            // Always trust - it is an example.
            // You should do something in the real world.
            logger.error(
                    "UNKNOWN SERVER CERTIFICATE: " + chain[0].getSubjectDN());
        }
    };

    public static TrustManager[] getTrustManagers() {
        return new TrustManager[]{DUMMY_TRUST_MANAGER};
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return getTrustManagers();
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
        // Unused
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
            throws InvalidAlgorithmParameterException {
        // Unused
    }
}
