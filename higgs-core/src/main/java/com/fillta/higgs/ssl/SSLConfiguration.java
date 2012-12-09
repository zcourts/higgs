package com.fillta.higgs.ssl;

public class SSLConfiguration {

	private String keyStorePath;
	private String keyPassword;
	private String keyStorePassword;
	private String keyStoreType = "JKS";
	private String keyManagerFactoryType = "SunX509";

	private String trustStorePath;
	private String trustStorePassword;
	private String trustStoreType = "JKS";
	private String trustManagerFactoryType = "SunX509";

	private String securityProviderClass = "com.sun.net.ssl.internal.www.protocol";
	private String securityProtocol = "TLS";


	public String getKeyStorePath() {
		return keyStorePath;
	}

	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStoreType() {
		return keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getKeyManagerFactoryType() {
		return keyManagerFactoryType;
	}

	public void setKeyManagerFactoryType(String keyManagerFactoryType) {
		this.keyManagerFactoryType = keyManagerFactoryType;
	}

	public String getTrustManagerFactoryType() {
		return trustManagerFactoryType;
	}

	public void setTrustManagerFactoryType(String trustManagerFactoryType) {
		this.trustManagerFactoryType = trustManagerFactoryType;
	}

	public String getTrustStorePath() {
		return trustStorePath;
	}

	public void setTrustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getTrustStoreType() {
		return trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public String getSecurityProviderClass() {
		return securityProviderClass;
	}

	public void setSecurityProviderClass(String securityProviderClass) {
		this.securityProviderClass = securityProviderClass;
	}

	public String getSecurityProtocol() {
		return securityProtocol;
	}

	public void setSecurityProtocol(String securityProtocol) {
		this.securityProtocol = securityProtocol;
	}
}
