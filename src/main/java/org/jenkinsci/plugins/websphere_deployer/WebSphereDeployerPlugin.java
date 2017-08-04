/*
 * 
 */
package org.jenkinsci.plugins.websphere_deployer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.websphere.services.deployment.Artifact;
import org.jenkinsci.plugins.websphere.services.deployment.DeploymentServiceException;
import org.jenkinsci.plugins.websphere.services.deployment.WebSphereDeploymentService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.ibm.icu.text.SimpleDateFormat;

import hudson.EnvVars;
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
import net.sf.json.JSONObject;

/**
 * A Jenkins plugin for deploying to WebSphere Application Server either locally
 * or remotely.
 *
 * @author Greg Peters
 */
public class WebSphereDeployerPlugin extends Notifier {

	/** The Constant OPERATION_REINSTALL. */
	private final static String OPERATION_REINSTALL = "1";

	/** The ip address. */
	private final String ipAddress;

	/** The connector type. */
	private final String connectorType;

	/** The port. */
	private final String port;

	/** The artifacts. */
	private final String artifacts;

	/** The ear level. */
	private final String earLevel;

	/** The deployment timeout. */
	private final String deploymentTimeout;

	/** The class loader order. */
	private final String classLoaderOrder;

	/** The class loader policy. */
	private final String classLoaderPolicy;

	/** The operations. */
	private final String operations;

	/** The context. */
	private final String context;

	/** The install path. */
	private final String installPath;

	/** The targets. */
	private final String targets;

	/** The virtual host. */
	private final String virtualHost;

	/** The application name. */
	private final String applicationName;

	/** The precompile. */
	private final boolean precompile;

	/** The reloading. */
	private final boolean reloading;

	/** The jsp reloading. */
	private final boolean jspReloading;

	/** The verbose. */
	private final boolean verbose;

	/** The distribute. */
	private final boolean distribute;

	/** The rollback. */
	private final boolean rollback;

	/** The unstable deploy. */
	private final boolean unstableDeploy;

	/** The security. */
	private final WebSphereSecurity security;

	/**
	 * Instantiates a new web sphere deployer plugin.
	 *
	 * @param ipAddress
	 *            the ip address
	 * @param connectorType
	 *            the connector type
	 * @param port
	 *            the port
	 * @param installPath
	 *            the install path
	 * @param security
	 *            the security
	 * @param artifacts
	 *            the artifacts
	 * @param earLevel
	 *            the ear level
	 * @param deploymentTimeout
	 *            the deployment timeout
	 * @param operations
	 *            the operations
	 * @param context
	 *            the context
	 * @param targets
	 *            the targets
	 * @param virtualHost
	 *            the virtual host
	 * @param applicationName
	 *            the application name
	 * @param precompile
	 *            the precompile
	 * @param reloading
	 *            the reloading
	 * @param jspReloading
	 *            the jsp reloading
	 * @param verbose
	 *            the verbose
	 * @param distribute
	 *            the distribute
	 * @param rollback
	 *            the rollback
	 * @param unstableDeploy
	 *            the unstable deploy
	 * @param classLoaderPolicy
	 *            the class loader policy
	 * @param classLoaderOrder
	 *            the class loader order
	 */
	@DataBoundConstructor
	public WebSphereDeployerPlugin(String ipAddress, String connectorType, String port, String installPath,
			WebSphereSecurity security, String artifacts, String earLevel, String deploymentTimeout, String operations,
			String context, String targets, String virtualHost, String applicationName, boolean precompile,
			boolean reloading, boolean jspReloading, boolean verbose, boolean distribute, boolean rollback,
			boolean unstableDeploy, String classLoaderPolicy, String classLoaderOrder) {
		this.context = context;
		this.targets = targets;
		this.virtualHost = virtualHost;
		this.installPath = installPath;
		this.ipAddress = ipAddress;
		this.connectorType = connectorType;
		this.artifacts = artifacts;
		this.port = port;
		this.operations = operations;
		this.earLevel = earLevel;
		this.deploymentTimeout = deploymentTimeout;
		this.precompile = precompile;
		this.reloading = reloading;
		this.jspReloading = jspReloading;
		this.verbose = verbose;
		this.distribute = distribute;
		this.rollback = rollback;
		this.unstableDeploy = unstableDeploy;
		this.security = security;
		this.classLoaderPolicy = classLoaderPolicy;
		this.classLoaderOrder = classLoaderOrder;
		this.applicationName = applicationName;
	}

	/**
	 * Gets the virtual host.
	 *
	 * @return the virtual host
	 */
	public String getVirtualHost() {
		return virtualHost;
	}

	/**
	 * Gets the class loader order.
	 *
	 * @return the class loader order
	 */
	public String getClassLoaderOrder() {
		return classLoaderOrder;
	}

	/**
	 * Gets the application name.
	 *
	 * @return the application name
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Gets the class loader policy.
	 *
	 * @return the class loader policy
	 */
	public String getClassLoaderPolicy() {
		return classLoaderPolicy;
	}

	/**
	 * Gets the targets.
	 *
	 * @return the targets
	 */
	public String getTargets() {
		return targets;
	}

	/**
	 * Gets the ear level.
	 *
	 * @return the ear level
	 */
	public String getEarLevel() {
		return earLevel;
	}

	/**
	 * Gets the security.
	 *
	 * @return the security
	 */
	public WebSphereSecurity getSecurity() {
		return security;
	}

	/**
	 * Checks if is distribute.
	 *
	 * @return true, if is distribute
	 */
	public boolean isDistribute() {
		return distribute;
	}

	/**
	 * Checks if is precompile.
	 *
	 * @return true, if is precompile
	 */
	public boolean isPrecompile() {
		return precompile;
	}

	/**
	 * Checks if is reloading.
	 *
	 * @return true, if is reloading
	 */
	public boolean isReloading() {
		return reloading;
	}

	/**
	 * Checks if is jsp reloading.
	 *
	 * @return true, if is jsp reloading
	 */
	public boolean isJspReloading() {
		return jspReloading;
	}

	/**
	 * Checks if is verbose.
	 *
	 * @return true, if is verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * Checks if is rollback.
	 *
	 * @return true, if is rollback
	 */
	public boolean isRollback() {
		return rollback;
	}

	/**
	 * Checks if is unstable deploy.
	 *
	 * @return true, if is unstable deploy
	 */
	public boolean isUnstableDeploy() {
		return unstableDeploy;
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
	 * Gets the context.
	 *
	 * @return the context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Gets the install path.
	 *
	 * @return the install path
	 */
	public String getInstallPath() {
		return installPath;
	}

	/**
	 * Gets the connector type.
	 *
	 * @return the connector type
	 */
	public String getConnectorType() {
		return connectorType;
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
	 * Gets the artifacts.
	 *
	 * @return the artifacts
	 */
	public String getArtifacts() {
		return artifacts;
	}

	/**
	 * Gets the operations.
	 *
	 * @return the operations
	 */
	public String getOperations() {
		return operations;
	}

	/**
	 * Gets the deployment timeout.
	 *
	 * @return the deployment timeout
	 */
	public String getDeploymentTimeout() {
		return deploymentTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.
	 * AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		if (shouldDeploy(build.getResult())) {
			WebSphereDeploymentService service = new WebSphereDeploymentService();
			Artifact artifact = null;
			try {
				EnvVars env = build.getEnvironment(listener);
				preInitializeService(listener, service, env);
				service.connect();
				for (FilePath path : gatherArtifactPaths(build, listener)) {
					artifact = createArtifact(path, listener, service);
					stopArtifact(artifact.getAppName(), listener, service);
					if (getOperations().equals(OPERATION_REINSTALL)) {
						uninstallArtifact(artifact.getAppName(), listener, service);
						deployArtifact(artifact, listener, service);
					} else { // otherwise update application
						if (!service.isArtifactInstalled(artifact.getAppName())) {
							deployArtifact(artifact, listener, service); // do
																			// initial
																			// deployment
						} else {
							updateArtifact(artifact, listener, service);
						}
					}
					startArtifact(artifact.getAppName(), listener, service);
					if (rollback) {
						saveArtifactToRollbackRepository(build, listener, artifact);
					}
				}
			} catch (Exception e) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream p = new PrintStream(out);
				e.printStackTrace(p);
				if (verbose) {
					logVerbose(listener,
							"Error deploying to IBM WebSphere Application Server: " + new String(out.toByteArray()));
				} else {
					log(listener, "Error deploying to IBM WebSphere Application Server: " + e.getMessage());
				}
				rollbackArtifact(service, build, listener, artifact);
				build.setResult(Result.FAILURE);
			} finally {
				service.disconnect();
			}
		} else {
			listener.getLogger().println(
					"Unable to deploy to IBM WebSphere Application Server, Build Result = " + build.getResult());
		}
		return true;
	}

	/**
	 * Should deploy.
	 *
	 * @param result
	 *            the result
	 * @return true, if successful
	 */
	private boolean shouldDeploy(Result result) {
		if (result.equals(Result.SUCCESS))
			return true;
		if (unstableDeploy && result.equals(Result.UNSTABLE))
			return true;
		return false;
	}

	/**
	 * Log.
	 *
	 * @param listener
	 *            the listener
	 * @param data
	 *            the data
	 */
	private void log(BuildListener listener, String data) {
		listener.getLogger().println(data);
	}

	/**
	 * Log verbose.
	 *
	 * @param listener
	 *            the listener
	 * @param data
	 *            the data
	 */
	private void logVerbose(BuildListener listener, String data) {
		if (verbose) {
			log(listener, data);
		}
	}

	/**
	 * Rollback artifact.
	 *
	 * @param service
	 *            the service
	 * @param build
	 *            the build
	 * @param listener
	 *            the listener
	 * @param artifact
	 *            the artifact
	 */
	private void rollbackArtifact(WebSphereDeploymentService service, AbstractBuild<?, ?> build, BuildListener listener,
			Artifact artifact) {
		if (artifact == null) {
			log(listener, "Cannot rollback to previous version: artifact is null");
			return;
		}
		log(listener, "Performing rollback of '" + artifact.getAppName() + "'");
		File installablePath = new File(build.getWorkspace().getRemote() + File.separator + "Rollbacks" + File.separator
				+ artifact.getAppName() + "." + artifact.getTypeName());
		if (installablePath.exists()) {
			artifact.setSourcePath(installablePath);
			try {
				updateArtifact(artifact, listener, service);
				startArtifact(artifact.getAppName(), listener, service);
				log(listener, "Rollback of '" + artifact.getAppName() + "' was successful");
			} catch (Exception e) {
				e.printStackTrace();
				log(listener, "Error while trying to rollback to previous version: " + e.getMessage());
			}
		} else {
			log(listener, "WARNING: Artifact doesn't exist rollback repository");
		}
	}

	/**
	 * Save artifact to rollback repository.
	 *
	 * @param build
	 *            the build
	 * @param listener
	 *            the listener
	 * @param artifact
	 *            the artifact
	 */
	private void saveArtifactToRollbackRepository(AbstractBuild<?, ?> build, BuildListener listener,
			Artifact artifact) {
		listener.getLogger()
				.println("Performing save operations on '" + artifact.getAppName() + "' for future rollbacks");
		File rollbackDir = new File(build.getWorkspace().getRemote() + File.separator + "Rollbacks");
		createIfNotExists(rollbackDir);
		logVerbose(listener, "Rollback Path: " + rollbackDir.getAbsolutePath());
		File destination = new File(rollbackDir, artifact.getAppName() + "." + artifact.getTypeName());
		if (destination.exists()) {
			log(listener, "Deleting old rollback version...");
			if (!destination.delete()) {
				log(listener, "Failed to delete old rollback version, permissions?: " + destination.getAbsolutePath());
				return;
			}
		}
		log(listener, "Saving new rollback version...");
		if (!artifact.getSourcePath().renameTo(destination)) {
			logVerbose(listener, "Failed to save '" + artifact.getAppName() + "' to rollback repository");
		} else {
			log(listener, "Saved '" + artifact.getAppName() + "' to rollback repository");
		}
	}

	/**
	 * Creates the if not exists.
	 *
	 * @param directory
	 *            the directory
	 */
	private void createIfNotExists(File directory) {
		if (directory.exists() || directory.mkdir()) {
			return;
		}
		throw new DeploymentServiceException(
				"Failed to create directory, is write access allowed?: " + directory.getAbsolutePath());
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
	private void deployArtifact(Artifact artifact, BuildListener listener, WebSphereDeploymentService service)
			throws Exception {
		listener.getLogger().println("Deploying '" + artifact.getAppName() + "' to IBM WebSphere Application Server");
		service.installArtifact(artifact);
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
	private void uninstallArtifact(String appName, BuildListener listener, WebSphereDeploymentService service)
			throws Exception {
		if (service.isArtifactInstalled(appName)) {
			listener.getLogger().println("Uninstalling Old Application '" + appName + "'...");
			service.uninstallArtifact(appName);
		}
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
	private void startArtifact(String appName, BuildListener listener, WebSphereDeploymentService service)
			throws Exception {
		listener.getLogger().println("Starting Application '" + appName + "'...");
		try {
			service.startArtifact(appName, Integer.parseInt(getDeploymentTimeout()));
		} catch (NumberFormatException e) {
			service.startArtifact(appName);
		}
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
	private void stopArtifact(String appName, BuildListener listener, WebSphereDeploymentService service)
			throws Exception {
		if (service.isArtifactInstalled(appName)) {
			listener.getLogger().println("Stopping Old Application '" + appName + "'...");
			service.stopArtifact(appName);
		}
	}

	/**
	 * Update artifact.
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
	private void updateArtifact(Artifact artifact, BuildListener listener, WebSphereDeploymentService service)
			throws Exception {
		if (service.isArtifactInstalled(artifact.getAppName())) {
			listener.getLogger()
					.println("Updating '" + artifact.getAppName() + "' on IBM WebSphere Application Server");
			service.updateArtifact(artifact);
		}
	}

	/**
	 * Creates the artifact.
	 *
	 * @param path
	 *            the path
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @return the artifact
	 */
	private Artifact createArtifact(FilePath path, BuildListener listener, WebSphereDeploymentService service) {
		Artifact artifact = new Artifact();
		if (path.getRemote().endsWith(".ear")) {
			artifact.setType(Artifact.TYPE_EAR);
		} else if (path.getRemote().endsWith(".war")) {
			artifact.setType(Artifact.TYPE_WAR);
		}
		if (StringUtils.trimToNull(context) != null) {
			artifact.setContext(context);
		}
		artifact.setClassLoaderOrder(classLoaderOrder);
		artifact.setClassLoaderPolicy(classLoaderPolicy);
		artifact.setTargets(targets);
		artifact.setVirtualHost(virtualHost);
		artifact.setInstallPath(installPath);
		artifact.setJspReloading(reloading);
		artifact.setDistribute(distribute);
		artifact.setPrecompile(isPrecompile());
		artifact.setSourcePath(new File(path.getRemote()));
		if (StringUtils.trimToNull(applicationName) != null) {
			artifact.setAppName(applicationName);
		} else {
			artifact.setAppName(getAppName(artifact, service));
		}
		if (artifact.getType() == Artifact.TYPE_WAR) {
			generateEAR(artifact, listener, service);
		}
		return artifact;
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
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
			for (FilePath path : paths) {
				listener.getLogger().println(path.getRemote() + " Last modified on " + sdf.format(path.lastModified()));
			}
			listener.getLogger().println("-------------------------------------------");
		}
		return paths;
	}

	/**
	 * Pre initialize service.
	 *
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 * @param env
	 *            the env
	 * @throws Exception
	 *             the exception
	 */
	private void preInitializeService(BuildListener listener, WebSphereDeploymentService service, EnvVars env)
			throws Exception {
		listener.getLogger().println("Connecting to IBM WebSphere Application Server...");
		service.setVerbose(isVerbose());
		service.setBuildListener(listener);
		;
		service.setConnectorType(getConnectorType());
		service.setHost(env.expand(getIpAddress()));
		service.setPort(env.expand(getPort()));
		if (security != null) {
			service.setUsername(env.expand(security.getUsername()));
			service.setPassword(env.expand(security.getPassword()));
			service.setKeyStoreLocation(new File(env.expand(security.getClientKeyFile())));
			service.setKeyStorePassword(env.expand(security.getClientKeyPassword()));
			service.setTrustStoreLocation(new File(env.expand(security.getClientTrustFile())));
			service.setTrustStorePassword(env.expand(security.getClientTrustPassword()));
		}
	}

	/**
	 * Gets the app name.
	 *
	 * @param artifact
	 *            the artifact
	 * @param service
	 *            the service
	 * @return the app name
	 */
	private String getAppName(Artifact artifact, WebSphereDeploymentService service) {
		if (artifact.getType() == Artifact.TYPE_EAR) {
			return service.getAppName(artifact.getSourcePath().getAbsolutePath());
		} else {
			String filename = artifact.getSourcePath().getName();
			return filename.substring(0, filename.lastIndexOf("."));
		}
	}

	/**
	 * Generate EAR.
	 *
	 * @param artifact
	 *            the artifact
	 * @param listener
	 *            the listener
	 * @param service
	 *            the service
	 */
	private void generateEAR(Artifact artifact, BuildListener listener, WebSphereDeploymentService service) {
		listener.getLogger().println("Generating EAR For Artifact: " + artifact.getAppName());
		File modified = new File(artifact.getSourcePath().getParent(), artifact.getAppName() + ".ear");
		service.generateEAR(artifact, modified, getEarLevel());
		artifact.setSourcePath(modified);
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

		/** The admin client path. */
		private String adminClientPath;

		/** The orb client path. */
		private String orbClientPath;

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
		 * @param connectorType
		 *            the connector type
		 * @param port
		 *            the port
		 * @param username
		 *            the username
		 * @param password
		 *            the password
		 * @param clientKeyFile
		 *            the client key file
		 * @param clientTrustFile
		 *            the client trust file
		 * @param clientKeyPassword
		 *            the client key password
		 * @param clientTrustPassword
		 *            the client trust password
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doTestConnection(@QueryParameter("ipAddress") String ipAddress,
				@QueryParameter("connectorType") String connectorType, @QueryParameter("port") String port,
				@QueryParameter("username") String username, @QueryParameter("password") String password,
				@QueryParameter("clientKeyFile") String clientKeyFile,
				@QueryParameter("clientTrustFile") String clientTrustFile,
				@QueryParameter("clientKeyPassword") String clientKeyPassword,
				@QueryParameter("clientTrustPassword") String clientTrustPassword)
				throws IOException, ServletException {
			WebSphereDeploymentService service = new WebSphereDeploymentService();
			try {
				if (!service.isAvailable()) {
					String destination = System.getProperty("user.home") + File.separator + ".jenkins" + File.separator
							+ "plugins" + File.separator + "websphere-deployer" + File.separator + "WEB-INF"
							+ File.separator + "lib" + File.separator;
					return FormValidation.warning(
							"Cannot find the required IBM WebSphere Application Server jar files in '" + destination
									+ "'. Please copy them from IBM WebSphere Application Server (see plugin documentation)");
				}
				service.setConnectorType(connectorType);
				service.setHost(ipAddress);
				service.setPort(port);
				service.setUsername(username);
				service.setPassword(password);
				service.setKeyStoreLocation(new File(clientKeyFile));
				service.setKeyStorePassword(clientKeyPassword);
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

		/**
		 * Do check port.
		 *
		 * @param value
		 *            the value
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doCheckPort(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Select a port");
			if (value.length() > 5)
				return FormValidation.warning("Cannot be greater than 65535");
			return FormValidation.ok();
		}

		/**
		 * Do check application name.
		 *
		 * @param value
		 *            the value
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doCheckApplicationName(@QueryParameter String value)
				throws IOException, ServletException {
			if (StringUtils.trimToNull(value) == null) {
				return FormValidation.warning("This setting is required for rollback support");
			} else {
				return FormValidation.ok();
			}
		}

		/**
		 * Do check admin client path.
		 *
		 * @param value
		 *            the value
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doCheckAdminClientPath(@QueryParameter String value)
				throws IOException, ServletException {
			if (!new File(value).exists()) {
				return FormValidation.error("Path '" + value + "' is not found");
			}
			return FormValidation.ok();
		}

		/**
		 * Do check orb client path.
		 *
		 * @param value
		 *            the value
		 * @return the form validation
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ServletException
		 *             the servlet exception
		 */
		public FormValidation doCheckOrbClientPath(@QueryParameter String value) throws IOException, ServletException {
			if (!new File(value).exists()) {
				return FormValidation.error("Path '" + value + "' is not found");
			}
			return FormValidation.ok();
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
			return "Deploy To IBM WebSphere Application Server";
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
			adminClientPath = formData.getString("adminClientPath");
			orbClientPath = formData.getString("orbClientPath");
			save();
			return super.configure(req, formData);
		}

		/**
		 * Gets the admin client path.
		 *
		 * @return the admin client path
		 */
		public String getAdminClientPath() {
			return adminClientPath;
		}

		/**
		 * Gets the orb client path.
		 *
		 * @return the orb client path
		 */
		public String getOrbClientPath() {
			return orbClientPath;
		}
	}
}
