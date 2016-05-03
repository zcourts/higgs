package io.higgs.core.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLContextFactory {

    public SSLContext getSSLSocket(SSLConfiguration sslConfiguration) {

        boolean useTrustStore = false;
        TrustManagerFactory tmf = null;
        KeyStore trustStore;

        try {
            trustStore = KeyStore.getInstance(sslConfiguration.getTrustStoreType());
            if (sslConfiguration.getTrustStorePath() != null) {
                trustStore.load(new FileInputStream(sslConfiguration.getTrustStorePath()),
                        sslConfiguration.getTrustStorePassword() == null ? "".toCharArray() :
                                sslConfiguration.getTrustStorePassword().toCharArray());
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
                        sslConfiguration.getKeyStorePassword() == null ? "".toCharArray() :
                                sslConfiguration.getKeyStorePassword().toCharArray());
                kmf = KeyManagerFactory.getInstance(sslConfiguration.getKeyManagerFactoryType());
                kmf.init(clientKeyStore, sslConfiguration.getKeyPassword() == null ? "".toCharArray() :
                        sslConfiguration.getKeyPassword().toCharArray());
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

    /**
     * Adds an SSL engine to the given pipeline.
     *
     * @param pipeline     the pipeline to add SSL support to
     * @param forceToFront if true then the SSL handler is added to the front of the pipeline otherwise it is added
     *                     at the end
     */
    public void addSSL(ChannelPipeline pipeline, boolean forceToFront, String[] sslProtocols) {
        SSLEngine engine = getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
        engine.setUseClientMode(true);
        if (sslProtocols != null && sslProtocols.length > 0) {
            engine.setEnabledProtocols(sslProtocols);
        }
        if (forceToFront) {
            pipeline.addFirst("ssl", new SslHandler(engine));
        } else {
            pipeline.addLast("ssl", new SslHandler(engine));
        }
    }
}
