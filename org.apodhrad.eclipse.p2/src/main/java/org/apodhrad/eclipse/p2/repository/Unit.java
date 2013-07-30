package org.apodhrad.eclipse.p2.repository;

import java.util.Properties;

public class Unit {

	private Properties properties;

	public Unit(String id, String version) {
		properties = new Properties();
		properties.put("id", id);
		properties.put("version", version);
	}

	public String getId() {
		return getProperty("id");
	}

	public String getVersion() {
		return getProperty("version");
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public void addProperty(String key, String value) {
		properties.put(key, value);
	}

	@Override
	public String toString() {
		return getId() + "-" + getVersion();
	}

}
