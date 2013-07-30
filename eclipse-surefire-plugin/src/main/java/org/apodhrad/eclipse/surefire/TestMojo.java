package org.apodhrad.eclipse.surefire;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.suite.RunResult;
import org.apodhrad.eclipse.p2.director.Eclipse;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * 
 * @author apodhrad
 * 
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class TestMojo extends AbstractMojo {

	public static final String DROPINS_FOLDER = "dropins";
	public static final String SWTBOT = "http://download.eclipse.org/technology/swtbot/releases/latest";
	public static final String ORBIT = "http://download.eclipse.org/tools/orbit/downloads/drops/R20130517111416/repository";
	public static final String REDDEER = "http://download.jboss.org/jbosstools/builds/staging/RedDeer_master/all/repo";

	public static final Name ECLIPSE_BUNDLESHAPE = new Name("Eclipse-BundleShape");

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	@Parameter(defaultValue = "${session}")
	private MavenSession session;

	@Component
	private BuildPluginManager manager;

	@Parameter(defaultValue = "${project.build.directory}")
	private String target;

	@Parameter(defaultValue = "${project.build.directory}/workspace")
	private String workspace;

	@Parameter(defaultValue = "0.18.0")
	private String tychoVersion;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}")
	private List<MavenArtifactRepository> remoteRepositories;

	@Component
	private RepositorySystem repoSystem;

	@Parameter(defaultValue = "${repositorySystemSession}")
	private RepositorySystemSession repoSession;

	@Parameter(required = true)
	private String eclipseHome;

	@Parameter(defaultValue = "org.eclipse.platform.ide")
	private String product;

	@Parameter(required = true)
	private String testPlugin;

	@Parameter(required = true)
	private String testVersion;

	@Parameter
	private String testClass;

	@Parameter
	private String argLine;

	@Parameter
	private String appArgLine;

	@Parameter(defaultValue = "false")
	private boolean useUIThread;

	@Parameter(alias = "surefire.timeout", defaultValue = "3600")
	private int surefireTimeout;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Hello eclipse-surefire-plugin");
		String tychoGroup = "org.eclipse.tycho";
		String dropinsFolder = target + "/" + DROPINS_FOLDER;
		Dropins dropins = new Dropins(project, session, manager, dropinsFolder);
		dropins.addDropin(tychoGroup, "org.eclipse.tycho.surefire.junit4", tychoVersion);
		dropins.addDropin(tychoGroup, "org.eclipse.tycho.surefire.osgibooter", tychoVersion);

		List<ArtifactRequest> artifactRequests = new ArrayList<ArtifactRequest>();

		getLog().info("Dependency Artifacts:");
		for (Artifact artifact : project.getDependencyArtifacts()) {
			getLog().info(artifact.getArtifactId() + " / " + artifact.toString());
			ArtifactRequest artifactRequest = new ArtifactRequest();
			artifactRequest.setArtifact(new DefaultArtifact(artifact.getGroupId() + ":"
					+ artifact.getArtifactId() + ":" + artifact.getVersion()));
			artifactRequests.add(artifactRequest);
		}

		ArtifactResult result;
		try {
			for (ArtifactRequest artifactRequest : artifactRequests) {
				getLog().info("Resolving " + artifactRequest.getArtifact());
				result = repoSystem.resolveArtifact(repoSession, artifactRequest);
				org.sonatype.aether.artifact.Artifact a = result.getArtifact();
				JarFile jarFile = new JarFile(a.getFile());
				Manifest manifest = jarFile.getManifest();
				String shape = manifest.getMainAttributes().getValue(ECLIPSE_BUNDLESHAPE);
				if (shape != null && shape.toUpperCase().equals("DIR")) {
					dropins.addDropin(a.getGroupId(), a.getArtifactId(), a.getVersion(), "unpack");
				} else {
					dropins.addDropin(a.getGroupId(), a.getArtifactId(), a.getVersion(), "copy");
				}
			}
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		dropins.createDropinsFolder();

		JarLauncher launcher = new JarLauncher(new Eclipse(eclipseHome).getJarFile());
		launcher.setTimeOut(surefireTimeout);
		// launcher.addJvmOption("-Xms512m", "-Xmx1024m", "-XX:PermSize=256m",
		// "-XX:MaxPermSize=512m");
		launcher.setDropinsFolder(dropinsFolder);
		if (argLine != null && argLine.length() > 0) {
			launcher.addJvmOption(argLine.split(" "));
		}
		launcher.addProgramOption("-application", "org.eclipse.tycho.surefire.osgibooter.uitest");
		launcher.addProgramOption("-clean");
		launcher.addProgramOption("-data", workspace);
		launcher.addProgramOption("-testproperties", target + "/surefire.properties");
		launcher.addProgramOption("-testApplication", "org.eclipse.ui.ide.workbench");
		launcher.addProgramOption("-product");
		if (!useUIThread) {
			launcher.addProgramOption("-nouithread");
		}
		if (appArgLine != null && appArgLine.length() > 0) {
			launcher.addProgramOption(appArgLine.split(" "));
		}

		createProperties(dropinsFolder);
		runTest(launcher);
	}

	private void createProperties(String dropinsFolder) throws MojoExecutionException {
		Properties props = new Properties();
		props.setProperty("testprovider", "org.apache.maven.surefire.junit4.JUnit4Provider");
		props.setProperty("failifnotests", "true");
		props.setProperty("reportsdirectory", target + "/surefire-reports");
		if (testClass == null) {
			testClass = "**/AllTests.class";
		} else {
			testClass = testClass.replace(".", "/") + ".class";
		}
		props.setProperty("includes", testClass);
		props.setProperty("redirectTestOutputToFile", "false");
		props.setProperty("testclassesdirectory", dropinsFolder + "/" + testPlugin + "-"
				+ testVersion);
		props.setProperty("testpluginname", testPlugin);

		try {
			props.store(new FileOutputStream(target + "/surefire.properties"), null);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void runTest(JarLauncher launcher) throws MojoExecutionException, MojoFailureException {
		int result;
		try {
			getLog().info("Command line:\n\t" + launcher.getCLI().toString());
			result = launcher.execute();
		} catch (Exception e) {
			throw new MojoExecutionException("Error while executing platform", e);
		}
		switch (result) {
		case 0:
			getLog().info("All tests passed!");
			break;
		case RunResult.NO_TESTS:
			String message = "No tests found.";
			throw new MojoFailureException(message);
		case RunResult.FAILURE:
			String errorMessage = "There are test failures.\n\nPlease refer for the individual test results.";
			throw new MojoFailureException(errorMessage);
		default:
			throw new MojoFailureException("An unexpected error occured (return code " + result
					+ "). See log for details.");
		}
	}
}
