package org.apodhrad.eclipse.p2.repository;

import java.util.Properties;

public class Artifact {

	private Properties properties;

	public Artifact(String id, String classifier, String version) {
		properties = new Properties();
		properties.put("id", id);
		properties.put("classifier", classifier);
		properties.put("version", version);
	}

	public String getId() {
		return getProperty("id");
	}

	public String getClassifier() {
		return getProperty("classifier");
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
		return getId() + "-" + getVersion() + " [" + getClassifier() + "]";
	}

}
