package com.fillta.higgs.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLContextFactory {


    public static SSLContext getSSLSocket(SSLConfiguration sslConfiguration) {

        boolean useTrustStore = false;
        TrustManagerFactory tmf = null;
        KeyStore trustStore;

        try {
            trustStore = KeyStore.getInstance(sslConfiguration.getTrustStoreType());
            if (sslConfiguration.getTrustStorePath() != null) {
                trustStore.load(new FileInputStream(sslConfiguration.getTrustStorePath()),
                        sslConfiguration.getTrustStorePassword() == null ? "".toCharArray() : sslConfiguration.getTrustStorePassword().toCharArray());
                tmf = TrustManagerFactory.getInstance(sslConfiguration.getTrustManagerFactoryType());
                tmf.init(trustStore);
                useTrustStore = true;
            } else {
                useTrustStore = false;
            }
        } catch (Exception e) {
            System.out.println("Unable to create TrustStore Manager. Reason: " + e.getMessage());
        }

        // Load the client's key store
        boolean useClientKeyStore = false;
        KeyManagerFactory kmf = null;
        try {

            KeyStore clientKeyStore = KeyStore.getInstance(sslConfiguration.getKeyStoreType());

            if (sslConfiguration.getKeyStorePath() != null) {
                clientKeyStore.load(new FileInputStream(sslConfiguration.getKeyStorePath()),
                        sslConfiguration.getKeyStorePassword() == null ? "".toCharArray() : sslConfiguration.getKeyStorePassword().toCharArray());

                kmf = KeyManagerFactory.getInstance(sslConfiguration.getKeyManagerFactoryType());
                kmf.init(clientKeyStore, sslConfiguration.getKeyPassword() == null ? "".toCharArray() : sslConfiguration.getKeyPassword().toCharArray());
                useClientKeyStore = true;
            } else {
                useClientKeyStore = false;
            }


        } catch (Exception e) {
            System.out.println("Unable to create KeyStore Manager. Reason: " + e.getMessage());

        }
        // Create SSL Socket Factory
        try {
            SSLContext ctx = SSLContext.getInstance(sslConfiguration.getSecurityProtocol());
            ctx.init(useClientKeyStore ? kmf.getKeyManagers() : null, useTrustStore ? tmf.getTrustManagers() : null,
                    new SecureRandom());
            return ctx;

        } catch (Exception e) {
            System.out.println("Unable to create SSL Context. Reason : " + e.getMessage());
        }

        return null;
    }
}
