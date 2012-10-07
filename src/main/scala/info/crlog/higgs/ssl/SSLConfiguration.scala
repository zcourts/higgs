package info.crlog.higgs.ssl

class SSLConfiguration {
  private var keyStorePath: String = null
  private var keyPassword: String = null
  private var keyStorePassword: String = null
  private var keyStoreType: String = "JKS"
  private var keyManagerFactoryType: String = "SunX509"
  private var trustStorePath: String = null
  private var trustStorePassword: String = null
  private var trustStoreType: String = "JKS"
  private var trustManagerFactoryType: String = "SunX509"
  private var securityProviderClass: String = "com.sun.net.ssl.internal.www.protocol"
  private var securityProtocol: String = "TLS"

  def getKeyStorePath: String = {
    return keyStorePath
  }

  def setKeyStorePath(keyStorePath: String) {
    this.keyStorePath = keyStorePath
  }

  def getKeyPassword: String = {
    return keyPassword
  }

  def setKeyPassword(keyPassword: String) {
    this.keyPassword = keyPassword
  }

  def getKeyStorePassword: String = {
    return keyStorePassword
  }

  def setKeyStorePassword(keyStorePassword: String) {
    this.keyStorePassword = keyStorePassword
  }

  def getKeyStoreType: String = {
    return keyStoreType
  }

  def setKeyStoreType(keyStoreType: String) {
    this.keyStoreType = keyStoreType
  }

  def getKeyManagerFactoryType: String = {
    return keyManagerFactoryType
  }

  def setKeyManagerFactoryType(keyManagerFactoryType: String) {
    this.keyManagerFactoryType = keyManagerFactoryType
  }

  def getTrustManagerFactoryType: String = {
    return trustManagerFactoryType
  }

  def setTrustManagerFactoryType(trustManagerFactoryType: String) {
    this.trustManagerFactoryType = trustManagerFactoryType
  }

  def getTrustStorePath: String = {
    return trustStorePath
  }

  def setTrustStorePath(trustStorePath: String) {
    this.trustStorePath = trustStorePath
  }

  def getTrustStorePassword: String = {
    return trustStorePassword
  }

  def setTrustStorePassword(trustStorePassword: String) {
    this.trustStorePassword = trustStorePassword
  }

  def getTrustStoreType: String = {
    return trustStoreType
  }

  def setTrustStoreType(trustStoreType: String) {
    this.trustStoreType = trustStoreType
  }

  def getSecurityProviderClass: String = {
    return securityProviderClass
  }

  def setSecurityProviderClass(securityProviderClass: String) {
    this.securityProviderClass = securityProviderClass
  }

  def getSecurityProtocol: String = {
    return securityProtocol
  }

  def setSecurityProtocol(securityProtocol: String) {
    this.securityProtocol = securityProtocol
  }
}

