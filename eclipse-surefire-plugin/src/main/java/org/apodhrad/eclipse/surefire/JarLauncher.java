package org.apodhrad.eclipse.surefire;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * 
 * @author apodhrad
 *
 */
public class JarLauncher {

	private File jarFile;
	private String executable;
	private int timeoutInSeconds;
	private List<String> jvmOptions;
	private List<String> programOptions;

	public JarLauncher(File jarFile) {
		this(jarFile, getExecutable());
	}

	public JarLauncher(File jarFile, String executable) {
		this.jarFile = jarFile;
		this.executable = executable;
		jvmOptions = new ArrayList<String>();
		programOptions = new ArrayList<String>();
	}

	public void addJvmOption(String... jvmOption) {
		for (int i = 0; i < jvmOption.length; i++) {
			jvmOptions.add(jvmOption[i].trim());
		}
	}

	public void addProgramOption(String... programOption) {
		for (int i = 0; i < programOption.length; i++) {
			programOptions.add(programOption[i]);
		}
	}

	public void setTimeOut(int seconds) {
		this.timeoutInSeconds = seconds;
	}

	public void setDropinsFolder(String folder) {
		jvmOptions.add("-Dorg.eclipse.equinox.p2.reconciler.dropins.directory=" + folder);
	}

	public static String getExecutable() {
		String slash = File.separator;
		String executable = System.getProperty("java.home") + slash + "bin" + slash + "java";
		if (File.separatorChar == '\\') {
			executable = executable + ".exe";
		}
		return executable;
	}

	public Commandline getCLI() {
		Commandline cli = new Commandline();
		cli.setExecutable(executable);
		cli.addArguments(jvmOptions.toArray(new String[jvmOptions.size()]));
		cli.addArguments(new String[] { "-jar", jarFile.getAbsolutePath() });
		cli.addArguments(programOptions.toArray(new String[programOptions.size()]));
		return cli;
	}

	public int execute() throws CommandLineException {
		StreamConsumer out = new StreamConsumer() {
			public void consumeLine(String line) {
				System.out.println(line);
			}
		};

		StreamConsumer err = new StreamConsumer() {
			public void consumeLine(String line) {
				System.err.println(line);
			}
		};

		return CommandLineUtils.executeCommandLine(getCLI(), out, err, timeoutInSeconds);
	}
}
