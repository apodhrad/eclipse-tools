package org.apodhrad.eclipse.surefire;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * 
 * @author apodhrad
 * 
 */
public class Dropins extends MojoExecutor {

	private MavenProject mavenProject;
	private MavenSession mavenSession;
	private BuildPluginManager pluginManager;
	private List<Dropin> dropins;
	private String dropinFolder;

	public Dropins(MavenProject mavenProject, MavenSession mavenSession,
			BuildPluginManager pluginManager, String dropinFolder) {
		this.mavenProject = mavenProject;
		this.mavenSession = mavenSession;
		this.pluginManager = pluginManager;
		this.dropinFolder = dropinFolder;
		dropins = new ArrayList<Dropins.Dropin>();
	}

	public void addDropin(String group, String artifact) {
		dropins.add(new Dropin(group, artifact, "0.0.0"));
	}

	public void addDropin(String group, String artifact, String version) {
		dropins.add(new Dropin(group, artifact, version));
	}

	public void addDropin(String group, String artifact, String version, String goal) {
		dropins.add(new Dropin(group, artifact, version, goal));
	}

	public void addDropin(Dropin dropin) {
		dropins.add(dropin);
	}

	public void createDropinsFolder() throws MojoExecutionException {
		executeMojo(
				plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"),
						version("2.8")), goal("copy"),
				configuration(getArtifactItemsWithGoal("copy")),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
		executeMojo(
				plugin(groupId("org.apache.maven.plugins"), artifactId("maven-dependency-plugin"),
						version("2.8")), goal("unpack"),
				configuration(getArtifactItemsWithGoal("unpack")),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
	}

	private Element getArtifactItemsWithGoal(String goal) {
		List<Element> artifactItems = new ArrayList<MojoExecutor.Element>();
		for (Dropin dropin : dropins) {
			if (dropin.getGoal().equals(goal)) {
				artifactItems.add(getArtifactItem(dropin));
			}
		}
		return element("artifactItems", artifactItems.toArray(new Element[artifactItems.size()]));
	}

	private Element getArtifactItem(Dropin dropin) {
		String output = dropinFolder;
		if (dropin.getGoal().equals("unpack")) {
			output += "/" + dropin.getArtifact() + "-" + dropin.getVersion();
		}
		return element("artifactItem", element("groupId", dropin.getGroup()),
				element("artifactId", dropin.getArtifact()),
				element("version", dropin.getVersion()), element("type", "jar"),
				element("outputDirectory", output));
	}

	public class Dropin {

		private String group;
		private String artifact;
		private String version;
		private String goal;

		public Dropin(String group, String artifact, String version) {
			this(group, artifact, version, "copy");
		}

		public Dropin(String group, String artifact, String version, String goal) {
			this.group = group;
			this.artifact = artifact;
			this.version = version;
			this.goal = goal;
		}

		public String getGroup() {
			return group;
		}

		public String getArtifact() {
			return artifact;
		}

		public String getVersion() {
			return version;
		}

		public String getGoal() {
			return goal;
		}

	}
}
