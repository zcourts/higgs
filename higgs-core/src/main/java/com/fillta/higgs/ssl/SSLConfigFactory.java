package com.fillta.higgs.ssl;

import java.lang.management.ManagementFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SSLConfigFactory {
	public static final SSLConfiguration sslConfiguration = new SSLConfiguration();

	static {
		for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			String[] kv = arg.split("=");
			if (kv.length >= 2) {
				String name = kv[0];
				String value = kv[1];
				switch (name) {
					case "javax.net.ssl.keyStore":
						sslConfiguration.setKeyStorePath(value);
					case "javax.net.ssl.keyStorePassword":
						sslConfiguration.setKeyStorePassword(value);
					case "javax.net.ssl.trustStrore":
						sslConfiguration.setTrustStorePath(value);
					case "javax.net.ssl.trustStorePassword":
						sslConfiguration.setTrustStorePassword(value);
					case "javax.net.ssl.keyPassword":
						sslConfiguration.setKeyPassword(value);
				}
			}
		}
	}
}
