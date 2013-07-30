package org.apodhrad.eclipse.p2.repository;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArtifactParser {

	private Element artifacts;

	public ArtifactParser(Node artifacts) {
		this(toElement(artifacts));
	}

	public ArtifactParser(Element artifacts) {
		this.artifacts = artifacts;
	}

	private static Element toElement(Node node) {
		if (node instanceof Element) {
			return (Element) node;
		}
		System.out.println(node);
		throw new IllegalArgumentException("Not an element");
	}

	public List<Artifact> parseArtifacts() {
		List<Artifact> artifactList = new ArrayList<Artifact>();
		NodeList artifactNodes = artifacts.getElementsByTagName("artifact");
		Node artifactNode = null;
		int i = 0;
		while ((artifactNode = artifactNodes.item(i++)) != null) {
			artifactList.add(parseArtifact(toElement(artifactNode)));
		}
		return artifactList;
	}

	private static Artifact parseArtifact(Element artifactElement) {
		String id = artifactElement.getAttribute("id");
		String classifier = artifactElement.getAttribute("classifier");
		String version = artifactElement.getAttribute("version");
		Artifact artifact = new Artifact(id, classifier, version);
		NodeList propertyNodes = artifactElement.getElementsByTagName("property");
		Node propertyNode = null;
		int i = 0;
		while ((propertyNode = propertyNodes.item(i++)) != null) {
			Element propertyElement = toElement(propertyNode);
			String key = propertyElement.getAttribute("name");
			String value = propertyElement.getAttribute("value");
			artifact.addProperty(key, value);
		}

		return artifact;
	}
}
