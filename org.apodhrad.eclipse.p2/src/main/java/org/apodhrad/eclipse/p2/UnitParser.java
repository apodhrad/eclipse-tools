package org.apodhrad.eclipse.p2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UnitParser {

	private Element units;

	public UnitParser(Node units) {
		this(toElement(units));
	}

	public UnitParser(Element units) {
		this.units = units;
	}

	private static Element toElement(Node node) {
		if (node instanceof Element) {
			return (Element) node;
		}
		System.out.println(node);
		throw new IllegalArgumentException("Not an element");
	}

	public List<Unit> parseUnits() {
		List<Unit> unitList = new ArrayList<Unit>();
		NodeList unitNodes = units.getElementsByTagName("unit");
		Node unitNode = null;
		int i = 0;
		while ((unitNode = unitNodes.item(i++)) != null) {
			unitList.add(parseUnit(toElement(unitNode)));
		}
		return unitList;
	}

	private static Unit parseUnit(Element unitElement) {
		String id = unitElement.getAttribute("id");
		String version = unitElement.getAttribute("version");
		Unit unit = new Unit(id, version);
		Node filterNode = unitElement.getElementsByTagName("filter").item(0);
		if (filterNode != null) {
			Element filterElement = toElement(filterNode);
			String filter = filterElement.getTextContent();
			unit.getProperties().putAll(parseFilter(filter));
		}

		return unit;
	}

	private static Properties parseFilter(String filter) {
		Properties properties = new Properties();

		String regex = "\\((.*=.*)+\\)";
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
