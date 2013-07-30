package org.apodhrad.eclipse.p2.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RuleParser {

	private Element mappings;

	public RuleParser(Node mappings) {
		this(toElement(mappings));
	}

	public RuleParser(Element mappings) {
		this.mappings = mappings;
	}

	private static Element toElement(Node node) {
		if (node instanceof Element) {
			return (Element) node;
		}
		throw new IllegalArgumentException("Not an element");
	}

	public List<Rule> parseRules() {
		List<Rule> rulesList = new ArrayList<Rule>();
		NodeList ruleNodes = mappings.getChildNodes();
		Node ruleNode = null;
		int i = 0;
		while ((ruleNode = ruleNodes.item(i++)) != null) {
			if (ruleNode instanceof Element) {
				Element ruleElement = (Element) ruleNode;
				rulesList.add(parseRule(ruleElement));
			}
		}
		return rulesList;
	}

	private static Rule parseRule(Element ruleElement) {
		String filter = ruleElement.getAttribute("filter");
		String output = ruleElement.getAttribute("output");
		Properties properties = parseFilter(filter);
		return new Rule(properties, output);
	}

	public static Properties parseFilter(String filter) {
		Properties properties = new Properties();

		String regex = "\\((?!&.*)(.*?)\\)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(filter);
		while (matcher.find()) {
			String property = matcher.group();
			property = property.substring(1, property.length() - 1);
			String[] props = property.split("=");
			properties.put(props[0], props[1]);
		}

		return properties;
	}
}
