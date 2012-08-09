package info.crlog.higgs.ssl
package com.fillta.https

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.io.FileInputStream
import java.security.KeyStore
import java.security.SecureRandom

object SSLContextFactory {
  def getSSLSocket(sslConfiguration: SSLConfiguration): SSLContext = {
    var useTrustStore: Boolean = false
    var tmf: TrustManagerFactory = null
    var trustStore: KeyStore = null
    try {
      trustStore = KeyStore.getInstance(sslConfiguration.getTrustStoreType)
      if (sslConfiguration.getTrustStorePath != null) {
        trustStore.load(new FileInputStream(sslConfiguration.getTrustStorePath), if (sslConfiguration.getTrustStorePassword == null) "".toCharArray else sslConfiguration.getTrustStorePassword.toCharArray)
        tmf = TrustManagerFactory.getInstance(sslConfiguration.getTrustManagerFactoryType)
        tmf.init(trustStore)
        useTrustStore = true
      }
      else {
        useTrustStore = false
      }
    }
    catch {
      case e: Exception => {
        System.out.println("Unable to create TrustStore Manager. Reason: " + e.getMessage)
      }
    }
    var useClientKeyStore: Boolean = false
    var kmf: KeyManagerFactory = null
    try {
      val clientKeyStore: KeyStore = KeyStore.getInstance(sslConfiguration.getKeyStoreType)
      if (sslConfiguration.getKeyStorePath != null) {
        clientKeyStore.load(new FileInputStream(sslConfiguration.getKeyStorePath), if (sslConfiguration.getKeyStorePassword == null) "".toCharArray else sslConfiguration.getKeyStorePassword.toCharArray)
        kmf = KeyManagerFactory.getInstance(sslConfiguration.getKeyManagerFactoryType)
        kmf.init(clientKeyStore, if (sslConfiguration.getKeyPassword == null) "".toCharArray else sslConfiguration.getKeyPassword.toCharArray)
        useClientKeyStore = true
      }
      else {
        useClientKeyStore = false
      }
    }
    catch {
      case e: Exception => {
        System.out.println("Unable to create KeyStore Manager. Reason: " + e.getMessage)
      }
    }
    try {
      val ctx: SSLContext = SSLContext.getInstance(sslConfiguration.getSecurityProtocol)
      ctx.init(if (useClientKeyStore) kmf.getKeyManagers else null, if (useTrustStore) tmf.getTrustManagers else null, new SecureRandom)
      return ctx
    }
    catch {
      case e: Exception => {
        System.out.println("Unable to create SSL Context. Reason : " + e.getMessage)
      }
    }
    return null
  }
}


