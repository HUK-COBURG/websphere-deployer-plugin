/*
 * 
 */
package org.jenkinsci.plugins.websphere_deployer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.websphere.services.deployment.Artifact;
import org.jenkinsci.plugins.websphere.services.deployment.LibertyDeploymentService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Scrambler;
import net.sf.json.JSONObject;

/**
 * A Jenkins plugin for deploying to WebSphere Liberty Server either locally or
 * remotely.
 *
 * @author Greg Peters
 */
public class LibertyDeployerPlugin extends Notifier {

	/** The ip address. */
	private final String ipAddress;

	/** The port. */
	private final String port;

	/** The username. */
	private final String username;

	/** The console password. */
	private final String consolePassword;

	/** The client trust file. */
	private final String clientTrustFile;

	/** The client trust password. */
	private final String clientTrustPassword;

	/** The artifacts. */
	private final String artifacts;

	/**
	 * Instantiates a new liberty deployer plugin.
	 *
	 * @param ipAddress
	 *            the ip address
	 * @param port
	 *            the port
	 * @param username
	 *            the username
	 * @param consolePassword
	 *            the console password
	 * @param clientTrustFile
	 *            the client trust file
	 * @param clientTrustPassword
	 *            the client trust password
	 * @param artifacts
	 *            the artifacts
	 */
	@DataBoundConstructor
	public LibertyDeployerPlugin(String ipAddress, String port, String username, String consolePassword,
			String clientTrustFile, String clientTrustPassword, String artifacts) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.username = username;
		this.consolePassword = Scrambler.scramble(consolePassword);
		this.clientTrustFile = clientTrustFile;
		this.clientTrustPassword = Scrambler.scramble(clientTrustPassword);
		this.artifacts = artifacts;
	}

	/**
	 * Gets the ip address.
	 *
	 * @return the ip address
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Gets the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the console password.
	 *
	 * @return the console password
	 */
	public String getConsolePassword() {
		return Scrambler.descramble(consolePassword);
	}

	/**
	 * Gets the client trust file.
	 *
	 * @return the client trust file
	 */
	public String getClientTrustFile() {
		return clientTrustFile;
	}

	/**
	 * Gets the client trust password.
	 *
	 * @return the client trust password
	 */
	public String getClientTrustPassword() {
		return Scrambler.descramble(clientTrustPassword);
	}

	/**
	 * Gets the artifacts.
	 *
	 * @return the artifacts
	 */
	public String getArtifacts() {
		return artifacts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.
	 * AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		if (build.getResult().equals(Result.SUCCESS)) {
			LibertyDeploymentService service = new LibertyDeploymentService();
			try {
				connect(listener, service);
				for (FilePath path : gatherArtifactPaths(build, listener)) {
					Artifact artifact = createArtifact(path);
					stopArtifact(artifact.getAppName(), listener, service);
					uninstallArtifact(artifact.getAppName(), listener, service);
					deployArtifact(artifact, listener, service);
					Thread.sleep(2000); // wait 2 seconds for deployment to
										// settle
					startArtifact(artifact.getAppName(), listener, service);
				}
			} catch (Exception e) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream p = new PrintStream(out);
				e.printStackTrace(p);
				listener.getLogger()
						.println("Error deploying to IBM WebSphere Liberty Profile: " + new String(out.toByteArray()));
				build.setResult(Result.FAILURE);
			} finally {
				try {
					disconnect(listener, service);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * Creates the artifact.
	 *
	 * @param path
	 *            the path
	 * @return the artifact
	 */
	private Artifact createArtifact(FilePath path) {
		Artifact artifact = new Artifact();
		if (path.getRemote().endsWith(".ear")) {
			artifact.setType(Artifact.TYPE_EAR);
		} else if (path.getRemote().endsWith(".war")) {
			artifact.setType(Artifact.TYPE_WAR);
		} else if (path.getRemote().endsWith(".rar")) {
			artifact.setType(Artifact.TYPE_RAR);
		} else if (path.getRemote().endsWith(".jar")) {
			artifact.setType(Artifact.TYPE_JAR);
		}
		artifact.setSourcePath(new File(path.getRemote()));
		artifact.setAppName(path.getBaseName());
		return artifact;
	}

	/**
	 * Connect.
	 *
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void connect(BuildListener listener, LibertyDeploymentService service) throws Exception {
		listener.getLogger().println("Connecting to IBM WebSphere Liberty Profile...");
		service.setHost(getIpAddress());
		service.setPort(getPort());
		service.setUsername(getUsername());
		service.setPassword(getConsolePassword());
		service.setTrustStoreLocation(new File(getClientTrustFile()));
		service.setTrustStorePassword(getClientTrustPassword());
		service.connect();
	}

	/**
	 * Disconnect.
	 *
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void disconnect(BuildListener listener, LibertyDeploymentService service) throws Exception {
		listener.getLogger().println("Disconnecting from IBM WebSphere Liberty Profile...");
		service.disconnect();
	}

	/**
	 * Stop artifact.
	 *
	 * @param appName
	 *            the app name
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void stopArtifact(String appName, BuildListener listener, LibertyDeploymentService service)
			throws Exception {
		if (service.isArtifactInstalled(appName)) {
			listener.getLogger().println("Stopping Old Application '" + appName + "'...");
			service.stopArtifact(appName);
		}
	}

	/**
	 * Uninstall artifact.
	 *
	 * @param appName
	 *            the app name
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void uninstallArtifact(String appName, BuildListener listener, LibertyDeploymentService service)
			throws Exception {
		if (service.isArtifactInstalled(appName)) {
			listener.getLogger().println("Uninstalling Old Application '" + appName + "'...");
			service.uninstallArtifact(appName);
		}
	}

	/**
	 * Deploy artifact.
	 *
	 * @param artifact
	 *            the artifact
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void deployArtifact(Artifact artifact, BuildListener listener, LibertyDeploymentService service)
			throws Exception {
		listener.getLogger().println("Deploying '" + artifact.getAppName() + "' to IBM WebSphere Liberty Profile");
		service.installArtifact(artifact);
	}

	/**
	 * Start artifact.
	 *
	 * @param appName
	 *            the app name
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @throws Exception
	 *             the exception
	 */
	private void startArtifact(String appName, BuildListener listener, LibertyDeploymentService service)
			throws Exception {
		listener.getLogger().println("Starting Application '" + appName + "'...");
		service.startArtifact(appName);
	}

	/**
	 * Gather artifact paths.
	 *
	 * @param build
	 *            the build
	 * @param listener
	 *            the listener
	 * @return the file path[]
	 * @throws Exception
	 *             the exception
	 */
	private FilePath[] gatherArtifactPaths(AbstractBuild<?, ?> build, BuildListener listener) throws Exception {
		FilePath[] paths = build.getWorkspace().getParent().list(getArtifacts());
		if (paths.length == 0) {
			listener.getLogger().println("No deployable artifacts found in path: " + build.getWorkspace().getParent()
					+ File.separator + getArtifacts());
			throw new Exception("No deployable artifacts found!");
		} else {
			listener.getLogger().println("The following artifacts will be deployed in this order...");
			listener.getLogger().println("-------------------------------------------");
			for (FilePath path : paths) {
				listener.getLogger().println(path.getName());
			}
			listener.getLogger().println("-------------------------------------------");
		}
		return paths;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.Notifier#getDescriptor()
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * The Class DescriptorImpl.
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		/**
		 * Instantiates a new descriptor impl.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Do test connection.
		 *
		 * @param ipAddress
		 *            the ip address
		 * @param port
		 *            the port
		 * @param username
		 *            the username
		 * @param password
		 *            the password
		 * @param clientTrustFile
		 *            the client trust file
		 * @param clientTrustPassword
		 *            the client trust password
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doTestConnection(@QueryParameter("ipAddress") String ipAddress,
				@QueryParameter("port") String port, @QueryParameter("username") String username,
				@QueryParameter("consolePassword") String password,
				@QueryParameter("clientTrustFile") String clientTrustFile,
				@QueryParameter("clientTrustPassword") String clientTrustPassword)
				throws IOException, ServletException {
			LibertyDeploymentService service = new LibertyDeploymentService();
			try {
				if (!service.isAvailable()) {
					String destination = System.getProperty("user.home") + File.separator + ".jenkins" + File.separator
							+ "plugins" + File.separator + "websphere-deployer" + File.separator + "WEB-INF"
							+ File.separator + "lib" + File.separator;
					return FormValidation
							.warning("Cannot find the required IBM WebSphere Liberty jar files in '" + destination
									+ "'. Please copy them from IBM WebSphere Liberty (see plugin documentation)");
				}
				service.setHost(ipAddress);
				service.setPort(port);
				service.setUsername(username);
				service.setPassword(password);
				service.setTrustStoreLocation(new File(clientTrustFile));
				service.setTrustStorePassword(clientTrustPassword);
				service.connect();
				return FormValidation.ok("Connection Successful!");
			} catch (Exception e) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream p = new PrintStream(out);
				e.printStackTrace(p);
				return FormValidation.error("Connection failed: " + new String(out.toByteArray()));
			} finally {
				service.disconnect();
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "Deploy To IBM WebSphere Liberty Profile";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest,
		 * net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}
