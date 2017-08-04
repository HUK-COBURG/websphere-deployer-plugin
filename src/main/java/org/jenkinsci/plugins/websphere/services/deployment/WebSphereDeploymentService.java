/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.enterprise.deploy.spi.Target;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.application.AppConstants;
import com.ibm.websphere.management.application.AppManagement;
import com.ibm.websphere.management.application.AppManagementProxy;
import com.ibm.websphere.management.application.AppNotification;
import com.ibm.websphere.management.application.client.AppDeploymentController;
import com.ibm.websphere.management.application.client.AppDeploymentTask;
import com.ibm.websphere.management.exception.AdminException;
import com.ibm.websphere.management.exception.ConnectorException;

import hudson.model.BuildListener;

/**
 * The Class WebSphereDeploymentService.
 *
 * @author Greg Peters
 */
public class WebSphereDeploymentService extends AbstractDeploymentService {

	/** The Constant CONNECTOR_TYPE_SOAP. */
	public static final String CONNECTOR_TYPE_SOAP = "SOAP";

	/** The Constant CLASSNAME. */
	private static final String CLASSNAME = WebSphereDeploymentService.class.getName();

	/** The log. */
	private static Logger log = Logger.getLogger(CLASSNAME);

	/** The client. */
	private AdminClient client;

	/** The connector type. */
	private String connectorType;

	/** The verbose. */
	private boolean verbose;

	/** The build listener. */
	private BuildListener buildListener;
	/**
	 * This is used to prevent weird behaviors caused by IBM wsadmin that
	 * overrides system properties.
	 *
	 * @see <a href=
	 *      "https://github.com/jenkinsci/websphere-deployer-plugin/pull/11">
	 *      GitHub discussion</a> for a reference.
	 */
	private Properties storedProperties;

	/**
	 * List servers.
	 *
	 * @return the list
	 */
	public List<Server> listServers() {
		try {
			if (!isConnected()) {
				throw new DeploymentServiceException("Cannot list servers, please connect to WebSphere first");
			}
			ObjectName targetQuery = new ObjectName("WebSphere:*,type=J2EEAppDeployment");
			Set<ObjectName> appDeployments = client.queryNames(targetQuery, null);
			List<Server> servers = new ArrayList<Server>();
			for (ObjectName appDeployment : appDeployments) {
				// reference:
				// http://www-01.ibm.com/support/knowledgecenter/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/rwlp_mbeans_operation.html?cp=SSEQTP_8.5.5%2F1-3-11-0-3-2-14-2-1
				Target[] targets = (Target[]) client.invoke(appDeployment, "getTargets", new Object[] { null, null },
						new String[] { Hashtable.class.getName(), String.class.getName() });
				for (Target target : targets) {
					if (target.getName().contains("J2EEServer")) { // only J2EE
																	// servers
																	// can be
																	// deployed
																	// to
						Server server = new Server();
						server.setObjectName(target.getName());
						server.setTarget(getFormattedTarget(target.getName()));
						servers.add(server);
					}
				}
				Collections.sort(servers);
				int i = 0;
				for (Server server : servers) {
					server.setIndex(i++); // set index after sort
				}
			}
			return servers;
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException(e.getMessage(), e);
		}
	}

	/**
	 * Generate EAR.
	 *
	 * @param artifact
	 *            the artifact
	 * @param destination
	 *            the destination
	 * @param earLevel
	 *            the ear level
	 */
	public void generateEAR(Artifact artifact, File destination, String earLevel) {
		byte[] buf = new byte[1024];
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));
				FileInputStream in = new FileInputStream(artifact.getSourcePath());) {
			out.putNextEntry(new ZipEntry(artifact.getSourcePath().getName()));
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
			in.close();
			out.putNextEntry(new ZipEntry("META-INF/"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("META-INF/application.xml"));
			out.write(getApplicationXML(artifact, earLevel).getBytes());
			out.closeEntry();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the context root.
	 *
	 * @param artifact
	 *            the artifact
	 * @return the context root
	 */
	/*
	 * This method tries to read ibm-web-ext.xml and extract the value of
	 * context-root. If any exception is thrown, it will fall back to the WAR
	 * name.
	 */
	private String getContextRoot(Artifact artifact) {
		try (ZipFile zipFile = new ZipFile(artifact.getSourcePath())) {
			if (artifact.getContext() != null) {
				return artifact.getContext();
			}
			// open WAR and find ibm-web-ext.xml
			ZipEntry webExt = zipFile.getEntry("WEB-INF/ibm-web-ext.xml");
			if (webExt != null) { // not an IBM based WAR
				InputStream webExtContent = zipFile.getInputStream(webExt);

				// parse ibm-web-ext.xml
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(webExtContent);

				// find uri attribute in context-root element
				Element contextRoot = (Element) doc.getElementsByTagName("context-root").item(0);
				String uri = contextRoot.getAttribute("uri");
				uri = uri.startsWith("/") ? uri : "/" + uri;
				return uri;
			}
			zipFile.close();
			return getContextRootFromWarName(artifact);
		} catch (Exception e) {
			e.printStackTrace();
			return getContextRootFromWarName(artifact);
		}
	}

	/**
	 * Gets the context root from war name.
	 *
	 * @param artifact
	 *            the artifact
	 * @return the context root from war name
	 */
	private String getContextRootFromWarName(Artifact artifact) {
		String warName = artifact.getSourcePath().getName();
		return warName.substring(0, warName.lastIndexOf('.'));
	}

	/**
	 * Gets the application XML.
	 *
	 * @param artifact
	 *            the artifact
	 * @param earLevel
	 *            the ear level
	 * @return the application XML
	 */
	private String getApplicationXML(Artifact artifact, String earLevel) {
		String contextRoot = getContextRoot(artifact);
		String warName = artifact.getSourcePath().getName();
		String displayName = StringUtils.trimToNull(artifact.getAppName());
		if (displayName == null) {
			displayName = warName;
		}
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<application xmlns=\"http://java.sun.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ getSchemaVersion(earLevel) + ">\n" + "  <description>" + warName
				+ " was deployed using WebSphere Deployer Plugin</description>\n" + "  <display-name>" + displayName
				+ "</display-name>\n" + "  <module>\n" + "    <web>\n" + "      <web-uri>" + warName + "</web-uri>\n"
				+ "      <context-root>" + contextRoot + "</context-root>\n" + "    </web>\n" + "  </module>\n"
				+ "</application>";
	}

	/**
	 * Gets the schema version.
	 *
	 * @param earLevel
	 *            the ear level
	 * @return the schema version
	 */
	private String getSchemaVersion(String earLevel) {
		if (earLevel == "7") {
			return "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/application_"
					+ earLevel + ".xsd\" version=\"" + earLevel + "\"";
		} else { // EAR is EE5 or EE6
			return "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_"
					+ earLevel + ".xsd\" version=\"" + earLevel + "\"";
		}
	}

	/**
	 * Gets the app name.
	 *
	 * @param path
	 *            the path
	 * @return the app name
	 */
	public String getAppName(String path) {
		return getAppName(new File(path));
	}

	/**
	 * Gets the app name.
	 *
	 * @param file
	 *            the file
	 * @return the app name
	 */
	public String getAppName(File file) {
		try {
			Hashtable<String, Object> preferences = new Hashtable<String, Object>();
			preferences.put(AppConstants.APPDEPL_LOCALE, Locale.getDefault());

			preferences.put(AppConstants.APPDEPL_DFLTBNDG, new Properties());

			AppDeploymentController controller = AppDeploymentController.readArchive(file.getAbsolutePath(),
					preferences);

			AppDeploymentTask task = controller.getFirstTask();
			while (task != null) {
				String[][] data = task.getTaskData();
				task.setTaskData(data);
				task = controller.getNextTask();
			}
			controller.saveAndClose();

			Hashtable<String, Object> config = controller.getAppDeploymentSavedResults();
			return (String) config.get(AppConstants.APPDEPL_APPNAME);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException(e.getMessage(), e);
		}
	}

	/**
	 * Builds the deployment preferences.
	 *
	 * @param artifact
	 *            the artifact
	 * @return the hashtable
	 * @throws Exception
	 *             the exception
	 */
	private Hashtable<String, Object> buildDeploymentPreferences(Artifact artifact) throws Exception {
		Hashtable<String, Object> preferences = new Hashtable<String, Object>();
		preferences.put(AppConstants.APPDEPL_LOCALE, Locale.getDefault());
		preferences.put(AppConstants.APPDEPL_DFLTBNDG, new Properties());
		AppDeploymentController controller = AppDeploymentController
				.readArchive(artifact.getSourcePath().getAbsolutePath(), preferences);

		String[] validationResult = controller.validate();
		if (validationResult != null && validationResult.length > 0) {
			throw new DeploymentServiceException("Unable to complete all task data for deployment preparation. Reason: "
					+ Arrays.toString(validationResult));
		}

		// controller.saveAndClose(); //block editing of EAR upon validation

		preferences.put(AppConstants.APPDEPL_LOCALE, Locale.getDefault());
		preferences.put(AppConstants.APPDEPL_ARCHIVE_UPLOAD, Boolean.TRUE);
		preferences.put(AppConstants.APPDEPL_PRECOMPILE_JSP, artifact.isPrecompile());
		preferences.put(AppConstants.APPDEPL_DISTRIBUTE_APP, artifact.isDistribute());
		preferences.put(AppConstants.APPDEPL_JSP_RELOADENABLED, artifact.isJspReloading());
		preferences.put(AppConstants.APPDEPL_RELOADENABLED, artifact.isReloading());
		if (!artifact.isJspReloading()) {
			preferences.put(AppConstants.APPDEPL_JSP_RELOADINTERVAL, Integer.valueOf(0));
		} else {
			preferences.put(AppConstants.APPDEPL_JSP_RELOADINTERVAL, Integer.valueOf(15));
		}
		if (!artifact.isReloading()) {
			preferences.put(AppConstants.APPDEPL_RELOADINTERVAL, Integer.valueOf(0));
		} else {
			preferences.put(AppConstants.APPDEPL_RELOADINTERVAL, Integer.valueOf(15));
		}
		if (StringUtils.trimToNull(artifact.getAppName()) != null) {
			preferences.put(AppConstants.APPDEPL_APPNAME, artifact.getAppName());
		}
		if (StringUtils.trimToNull(artifact.getInstallPath()) != null) {
			preferences.put(AppConstants.APPDEPL_INSTALL_DIR, artifact.getInstallPath());
		}
		if (StringUtils.trimToNull(artifact.getClassLoaderOrder()) != null) {
			preferences.put(AppConstants.APPDEPL_CLASSLOADINGMODE, artifact.getClassLoaderOrder());
		}
		if (StringUtils.trimToNull(artifact.getClassLoaderPolicy()) != null) {
			preferences.put(AppConstants.APPDEPL_CLASSLOADERPOLICY, artifact.getClassLoaderPolicy());
		}
		if (artifact.getContext() != null) {
			buildListener.getLogger().println("Setting context root to: " + artifact.getContext());
			preferences.put(AppConstants.APPDEPL_WEBMODULE_CONTEXTROOT, artifact.getContext());
			preferences.put(AppConstants.APPDEPL_WEB_CONTEXTROOT, artifact.getContext());
		}

		if (artifact.getVirtualHost() != null && !"".equals(artifact.getVirtualHost())) {
			buildListener.getLogger().println("===================");
			buildListener.getLogger().println(">>>" + artifact.getVirtualHost());
			preferences.put(AppConstants.APPDEPL_VIRTUAL_HOST, artifact.getVirtualHost());
		}

		Hashtable<String, Object> module2server = new Hashtable<String, Object>();
		buildListener.getLogger().println("Deploying to targets: " + getFormattedTargets(artifact.getTargets()));
		module2server.put("*", getFormattedTargets(artifact.getTargets()));
		preferences.put(AppConstants.APPDEPL_MODULE_TO_SERVER, module2server);
		return preferences;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * installArtifact(org.jenkinsci.plugins.websphere.services.deployment.
	 * Artifact)
	 */
	public void installArtifact(Artifact artifact) {
		if (!isConnected()) {
			throw new DeploymentServiceException(
					"Cannot install artifact, no connection to IBM WebSphere Application Server exists");
		}
		try {
			Hashtable<String, Object> preferences = buildDeploymentPreferences(artifact);
			AppManagement appManagementProxy = AppManagementProxy.getJMXProxyForClient(getAdminClient());
			appManagementProxy.installApplication(artifact.getSourcePath().getAbsolutePath(), artifact.getAppName(),
					preferences, null);

			NotificationFilterSupport filterSupport = createFilterSupport();
			DeploymentNotificationListener notifyListener = new DeploymentNotificationListener(getAdminClient(),
					filterSupport, "Install " + artifact.getAppName(), AppNotification.INSTALL, buildListener, verbose);

			synchronized (notifyListener) {
				notifyListener.wait();
			}

			if (!notifyListener.isSuccessful())
				throw new DeploymentServiceException(
						"Application not successfully deployed: " + notifyListener.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Failed to install artifact: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * updateArtifact(org.jenkinsci.plugins.websphere.services.deployment.
	 * Artifact)
	 */
	public void updateArtifact(Artifact artifact) {
		if (!isConnected()) {
			throw new DeploymentServiceException(
					"Cannot update artifact, no connection to IBM WebSphere Application Server exists");
		}
		try {
			Hashtable<String, Object> preferences = buildDeploymentPreferences(artifact);

			AppManagement appManagementProxy = AppManagementProxy.getJMXProxyForClient(getAdminClient());

			appManagementProxy.redeployApplication(artifact.getSourcePath().getAbsolutePath(), artifact.getAppName(),
					preferences, null);

			NotificationFilterSupport filterSupport = createFilterSupport();
			DeploymentNotificationListener notifyListener = new DeploymentNotificationListener(getAdminClient(),
					filterSupport, "Update " + artifact.getAppName(), AppNotification.INSTALL, buildListener, verbose);

			synchronized (notifyListener) {
				notifyListener.wait();
			}

			if (!notifyListener.isSuccessful())
				throw new DeploymentServiceException(
						"Application not successfully updated: " + notifyListener.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Failed to update artifact: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * uninstallArtifact(java.lang.String)
	 */
	public void uninstallArtifact(String appName) throws Exception {
		try {
			Hashtable<Object, Object> prefs = new Hashtable<Object, Object>();
			NotificationFilterSupport filterSupport = createFilterSupport();

			DeploymentNotificationListener notifyListener = new DeploymentNotificationListener(getAdminClient(),
					filterSupport, "Uninstall " + appName, AppNotification.UNINSTALL, buildListener, verbose);

			AppManagement appManagementProxy = AppManagementProxy.getJMXProxyForClient(getAdminClient());

			appManagementProxy.uninstallApplication(appName, prefs, null);

			synchronized (notifyListener) {
				notifyListener.wait();
			}
			if (!notifyListener.isSuccessful()) {
				throw new DeploymentServiceException(
						"Application not successfully undeployed: " + notifyListener.getMessage());
			}
		} catch (Exception e) {
			throw new DeploymentServiceException("Could not undeploy application", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * startArtifact(java.lang.String)
	 */
	public void startArtifact(String appName) throws Exception {
		startArtifact(appName, 5);
	}

	/**
	 * Start artifact.
	 *
	 * @param appName
	 *            the app name
	 * @param deploymentTimeout
	 *            the deployment timeout
	 * @throws Exception
	 *             the exception
	 */
	public void startArtifact(String appName, int deploymentTimeout) throws Exception {
		try {
			AppManagement appManagementProxy = AppManagementProxy.getJMXProxyForClient(getAdminClient());
			if (waitForApplicationDistribution(appManagementProxy, appName, deploymentTimeout * 60)) {
				String targetsStarted = appManagementProxy.startApplication(appName, null, null);
				log.info("Application was started on the following targets: " + targetsStarted);
				if (targetsStarted == null) {
					// wait X seconds to let deployment settle
					// TODO check if app really is started, if not throw an
					// error
					throw new DeploymentServiceException(
							"Application did not start successfully. WAS JVM logs should contain more detailed information.");
				}
			} else {
				throw new DeploymentServiceException("Distribution of application did not succeed on all nodes.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Could not start artifact '" + appName + "': " + e.toString());
		}
	}

	/**
	 * Wait for application distribution.
	 *
	 * @param appManagementProxy
	 *            the app management proxy
	 * @param appName
	 *            the app name
	 * @param secondsToWait
	 *            the seconds to wait
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	private boolean waitForApplicationDistribution(AppManagement appManagementProxy, String appName, int secondsToWait)
			throws Exception {
		int totalSeconds = 0;
		NotificationFilterSupport filterSupport = createFilterSupport();
		DeploymentNotificationListener distributionListener = null;
		while (checkDistributionStatus(distributionListener) != AppNotification.DISTRIBUTION_DONE
				&& totalSeconds < secondsToWait) {
			Thread.sleep(1000);
			totalSeconds++;
			distributionListener = new DeploymentNotificationListener(getAdminClient(), filterSupport, null,
					AppNotification.DISTRIBUTION_STATUS_NODE, buildListener, verbose);
			synchronized (distributionListener) {
				appManagementProxy.getDistributionStatus(appName, new Hashtable<Object, Object>(), null);
				distributionListener.wait();
			}
		}
		return totalSeconds <= secondsToWait;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * stopArtifact(java.lang.String)
	 */
	public void stopArtifact(String appName) throws Exception {
		try {
			AppManagementProxy.getJMXProxyForClient(getAdminClient()).stopApplication(appName,
					new Hashtable<Object, Object>(), null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Could not stop artifact '" + appName + "': " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * isArtifactInstalled(java.lang.String)
	 */
	public boolean isArtifactInstalled(String name) {
		try {
			buildListener.getLogger().println("Checking if app, looking up JMX interface...");
			AppManagement appManagement = AppManagementProxy.getJMXProxyForClient(getAdminClient());
			buildListener.getLogger().println("Getting JMX proxy client... AppManagementProxy Found: " + appManagement);
			buildListener.getLogger().println("Checking if app exists via AppMangementProxy...");
			boolean result = appManagement.checkIfAppExists(name, new Hashtable<Object, Object>(), null);
			buildListener.getLogger().println(name + " is installed on WebSphere: " + result);
			return result;
		} catch (AdminException e) {
			e.printStackTrace();
			throw new DeploymentServiceException(
					"Could not determine if artifact '" + name + "' is installed: AdminException: " + e.getMessage());
		} catch (ConnectorException e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Could not determine if artifact '" + name
					+ "' is installed: ConnectorException: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeploymentServiceException("Could not determine if artifact '" + name
					+ "' is installed: General Exception: " + e.getMessage());
		}
	}

	/**
	 * Creates the filter support.
	 *
	 * @return the notification filter support
	 */
	private NotificationFilterSupport createFilterSupport() {
		NotificationFilterSupport filterSupport = new NotificationFilterSupport();
		filterSupport.enableType(AppConstants.NotificationType);
		return filterSupport;
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		try {
			return client != null && client.isAlive() != null;
		} catch (Exception e) {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * connect()
	 */
	public void connect() throws Exception {
		// store the current environment, before that wsadmin client overrides
		// it
		storedProperties = (Properties) System.getProperties().clone();
		if (isConnected()) {
			log.warning("Already connected to WebSphere Application Server");
		}
		Properties config = new Properties();
		config.put(AdminClient.CONNECTOR_HOST, getHost());
		config.put(AdminClient.CONNECTOR_PORT, getPort());
		if (StringUtils.trimToNull(getUsername()) != null) {
			injectSecurityConfiguration(config);
		}
		config.put(AdminClient.CONNECTOR_TYPE, getConnectorType());
		client = AdminClientFactory.createAdminClient(config);
		if (client == null) {
			throw new DeploymentServiceException(
					"Unable to connect to IBM WebSphere Application Server @ " + getHost() + ":" + getPort());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * disconnect()
	 */
	public void disconnect() {
		// restore environment after execution
		if (storedProperties != null) {
			System.setProperties(storedProperties);
			storedProperties = null;
		}
		if (client != null) {
			client.getConnectorProperties().clear();
			client = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * isAvailable()
	 */
	public boolean isAvailable() {
		try {
			Class.forName("com.ibm.websphere.management.AdminClientFactory", false, getClass().getClassLoader());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Gets the admin client.
	 *
	 * @return the admin client
	 */
	private AdminClient getAdminClient() {
		if (client == null) {
			throw new DeploymentServiceException("No connection to WebSphere exists");
		}
		return client;
	}

	/**
	 * Inject security configuration.
	 *
	 * @param config
	 *            the config
	 */
	private void injectSecurityConfiguration(Properties config) {
		config.put(AdminClient.CACHE_DISABLED, "true");
		config.put(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
		config.put(AdminClient.USERNAME, getUsername());
		config.put(AdminClient.PASSWORD, getPassword());

		if (getTrustStoreLocation() != null) {
			config.put("com.ibm.ssl.trustStore", getTrustStoreLocation().getAbsolutePath());
			config.put("javax.net.ssl.trustStore", getTrustStoreLocation().getAbsolutePath());
		}

		if (getKeyStoreLocation() != null) {
			config.put("com.ibm.ssl.keyStore", getKeyStoreLocation().getAbsolutePath());
			config.put("javax.net.ssl.keyStore", getKeyStoreLocation().getAbsolutePath());
		}

		if (getTrustStorePassword() != null) {
			config.put("com.ibm.ssl.trustStorePassword", getTrustStorePassword());
			config.put("javax.net.ssl.trustStorePassword", getTrustStorePassword());
		}

		if (getKeyStorePassword() != null) {
			config.put("com.ibm.ssl.keyStorePassword", getKeyStorePassword());
			config.put("javax.net.ssl.keyStorePassword", getKeyStorePassword());
		}
	}

	/**
	 * Gets the formatted targets.
	 *
	 * @param targets
	 *            the targets
	 * @return the formatted targets
	 */
	private String getFormattedTargets(String targets) {
		List<String> result = new ArrayList<String>();
		for (StringTokenizer st = new StringTokenizer(targets.trim(), "\r\n"); st.hasMoreTokens();) {
			result.add(st.nextToken());
		}
		return StringUtils.join(result, "+");
	}

	/**
	 * Gets the formatted target.
	 *
	 * @param target
	 *            the target
	 * @return the formatted target
	 */
	private String getFormattedTarget(String target) {
		target = target.replace("WebSphere:", "").replace(",j2eeType=J2EEServer", ""); // remove
																						// 'WebSphere:'
																						// &
																						// 'j2eeType'
																						// to
																						// work
																						// on
																						// comma
																						// delimited
																						// array
		String[] elements = target.split(",");
		ArrayUtils.reverse(elements);
		return "WebSphere:" + StringUtils.join(elements, ",");
	}

	/**
	 * Sets the connector type.
	 *
	 * @param type
	 *            the new connector type
	 */
	public void setConnectorType(String type) {
		this.connectorType = type;
	}

	/**
	 * Gets the connector type.
	 *
	 * @return the connector type
	 */
	public String getConnectorType() {
		return this.connectorType;
	}

	/**
	 * Sets the verbose.
	 *
	 * @param verbose
	 *            the new verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Sets the builds the listener.
	 *
	 * @param listener
	 *            the new builds the listener
	 */
	public void setBuildListener(BuildListener listener) {
		this.buildListener = listener;
	}

	/**
	 * Check distribution status.
	 *
	 * @param listener
	 *            the listener
	 * @return the string
	 * @throws MalformedObjectNameException
	 *             the malformed object name exception
	 */
	/*
	 * Checks the listener and figures out the aggregate distribution status of
	 * all nodes
	 */
	private String checkDistributionStatus(DeploymentNotificationListener listener)
			throws MalformedObjectNameException {
		String distributionState = AppNotification.DISTRIBUTION_UNKNOWN;
		if (listener != null) {
			String compositeServers = listener.getNotificationProps()
					.getProperty(AppNotification.DISTRIBUTION_STATUS_COMPOSITE);
			if (compositeServers != null) {
				if (verbose) {
					buildListener.getLogger().println("Server Composite: " + compositeServers);
				}
				String[] servers = compositeServers.split("\\+");
				int countTrue = 0;
				int countFalse = 0;
				int countUnknown = 0;
				for (String server : servers) {
					ObjectName serverObject = new ObjectName(server);
					distributionState = serverObject.getKeyProperty("distribution");
					if (verbose) {
						buildListener.getLogger().println("Distributed to " + server + ": " + distributionState);
					}
					if (distributionState.equals("true"))
						countTrue++;
					if (distributionState.equals("false"))
						countFalse++;
					if (distributionState.equals("unknown"))
						countUnknown++;
				}
				if (countUnknown > 0) {
					distributionState = AppNotification.DISTRIBUTION_UNKNOWN;
				} else if (countFalse > 0) {
					distributionState = AppNotification.DISTRIBUTION_NOT_DONE;
				} else if (countTrue > 0) {
					distributionState = AppNotification.DISTRIBUTION_DONE;
				} else {
					throw new DeploymentServiceException("Reported distribution status is invalid.");
				}
			}
		}
		return distributionState;
	}

}
