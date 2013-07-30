package org.apodhrad.eclipse.p2.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Repository {

	public static final BaseMatcher ALL = new BaseMatcher() {
		public boolean matches(Object obj) {
			return true;
		}
	};

	public static final BaseMatcher BUNDLES = new BaseMatcher() {
		public boolean matches(Object obj) {
			if (obj instanceof Artifact) {
				Artifact artifact = (Artifact) obj;
				return artifact.getClassifier().equals("osgi.bundle")
						&& artifact.getProperty("format") == null;
			}
			return false;
		}
	};

	private String repoUrl;
	private List<Unit> units;
	private List<Rule> rules;
	private List<Artifact> artifacts;

	public Repository(String repoUrl) throws IOException {
		this.repoUrl = repoUrl;
		try {
			parseArtifacts(getInputStream(repoUrl, "artifacts"));
			parseContent(getInputStream(repoUrl, "content"));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private static InputStream getInputStream(String repoUrl, String fileName)
			throws IOException {
		download(repoUrl + "/" + fileName + ".jar", "./");
		JarFile jarFile = new JarFile("./" + fileName + ".jar");
		JarEntry jarEntry = jarFile.getJarEntry(fileName + ".xml");
		InputStream inputStream = jarFile.getInputStream(jarEntry);
		return inputStream;
	}

	private void parseContent(InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputStream);

		Node mappingsNode = doc.getElementsByTagName("units").item(0);
		units = new UnitParser(mappingsNode).parseUnits();

		inputStream.close();
	}

	private void parseArtifacts(InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputStream);

		Node mappingsNode = doc.getElementsByTagName("mappings").item(0);
		rules = new RuleParser(mappingsNode).parseRules();

		Node artifactsNode = doc.getElementsByTagName("artifacts").item(0);
		artifacts = new ArtifactParser(artifactsNode).parseArtifacts();

		inputStream.close();
	}

	public void listArtifacts() {
		listArtifacts(ALL);
	}

	public void listArtifacts(BaseMatcher matcher) {
		for (Artifact artifact : artifacts) {
			if (matcher.matches(artifact)) {
				System.out.println(artifact);
			}
		}
	}

	public void listUnits() throws IOException {
		listUnits(ALL);
	}

	public void listUnits(BaseMatcher matcher) throws IOException {
		for (Unit unit : units) {
			if (matcher.matches(unit)) {
				System.out.println(unit);
			}
		}
	}

	public void download(String folder) throws IOException {
		download(ALL, folder);
	}

	public void downloadBundles(String folder) throws IOException {
		download(BUNDLES, folder);
	}

	public void downloadBundles(String folder, String... id) throws IOException {
		if (id.length == 0) {
			download(BUNDLES, folder);
		} else {
			download(new ArtifactWithId(id), folder);
		}
	}

	public void download(BaseMatcher matcher, String folder) throws IOException {
		for (Artifact artifact : artifacts) {
			if (matcher.matches(artifact)) {
				download(applyRule(artifact), folder);
			}
		}
	}

	public List<Artifact> getArtifacts() {
		return artifacts;
	}

	public List<Artifact> getArtifacts(BaseMatcher matcher) {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		for (Artifact artifact : this.artifacts) {
			if (matcher.matches(artifact)) {
				artifacts.add(artifact);
			}
		}
		return artifacts;
	}

	public List<String> applyRule(List<Artifact> artifacts) {
		List<String> outputs = new ArrayList<String>();
		for (Artifact artifact : artifacts) {
			outputs.add(applyRule(artifact, rules));
		}
		return outputs;
	}

	public String applyRule(Artifact artifact) {
		return applyRule(artifact, rules);
	}

	public String applyRule(Artifact artifact, List<Rule> rules) {
		for (Rule rule : rules) {
			String output = rule.getOutput();
			if (rule.matches(artifact.getProperties())) {
				List<String> variables = getVariables(output);
				for (String variable : variables) {
					String value = getValue(artifact, variable);
					if (value == null) {
						value = variable;
					}
					output = output.replace(variable, value);
				}
				return output;
			}
		}
		return null;
	}

	private String getValue(Artifact artifact, String variable) {
		if ("${repoUrl}".equals(variable)) {
			return repoUrl;
		}

		String regex = "\\$\\{(.*?)\\}";
		if (!variable.matches(regex)) {
			throw new IllegalArgumentException("'" + variable
					+ "' is not a variable");
		}
		String key = variable.substring(2, variable.length() - 1);
		return artifact.getProperty(key);
	}

	public static List<String> getVariables(String output) {
		List<String> variables = new ArrayList<String>();
		String regex = "\\$\\{(.*?)\\}";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(output);
		while (matcher.find()) {
			variables.add(matcher.group());
		}
		return variables;
	}

	private static void download(String source, String target)
			throws IOException {
		File f = new File(target);
		if (!f.exists()) {
			f.mkdirs();
		}

		String fileName = source.substring(source.lastIndexOf("/") + 1);

		URL url = new URL(source);
		InputStream is = url.openStream();
		FileOutputStream fos = new FileOutputStream(target + "/" + fileName);

		byte[] buffer = new byte[4096];
		int bytesRead = 0;

		System.out.println("Downloading " + source);
		while ((bytesRead = is.read(buffer)) != -1) {
			fos.write(buffer, 0, bytesRead);
		}

		fos.close();
		is.close();
	}

	private class ArtifactWithId implements BaseMatcher {

		private List<String> idList;

		public ArtifactWithId(String... id) {
			idList = new ArrayList<String>();
			for (int i = 0; i < id.length; i++) {
				idList.add(id[i]);
			}
		}

		public boolean matches(Object obj) {
			if (BUNDLES.matches(obj)) {
				Artifact artifact = (Artifact) obj;
				return idList.contains(artifact.getId());
			}
			return false;
		}

	}

}
