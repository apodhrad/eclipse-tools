package org.apodhrad.eclipse.p2.repository;

import java.util.Properties;
import java.util.Set;

public class Rule {

	private Properties properties;
	private String output;

	public Rule(Properties properties, String output) {
		Set<String> keys = properties.stringPropertyNames();
		for (String key : keys) {
			if (properties.getProperty(key) == null) {
				throw new IllegalArgumentException("Given properties contain 'null' value");
			}
		}

		this.properties = properties;
		this.output = output;
	}

	public Properties getFilter() {
		return properties;
	}

	public String getOutput() {
		return output;
	}

	public boolean matches(Properties properties) {
		Set<String> keys = getFilter().stringPropertyNames();
		for (String key : keys) {
			if (!getFilter().getProperty(key).equals(properties.getProperty(key))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return "filter: " + properties + ", output = " + output;
	}

}
