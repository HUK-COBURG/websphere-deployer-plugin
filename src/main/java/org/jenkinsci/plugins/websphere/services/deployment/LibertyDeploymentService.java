/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.io.IOException;
import java.util.HashMap;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.application.ApplicationMBean;
import com.ibm.websphere.filetransfer.FileTransferMBean;

/**
 * The Class LibertyDeploymentService.
 *
 * @author Greg Peters
 */
public class LibertyDeploymentService extends AbstractDeploymentService {

	/** The Constant WEB_SPHERE_SERVICE_M_BEAN_NAME. */
	private static final String WEB_SPHERE_SERVICE_M_BEAN_NAME = "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=";

	/** The connector. */
	private JMXConnector connector;

	/** The client. */
	private MBeanServerConnection client;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * installArtifact(org.jenkinsci.plugins.websphere.services.deployment.
	 * Artifact)
	 */
	public void installArtifact(Artifact artifact) {
		try {
			ObjectName fileTransferServiceMBean = new ObjectName(
					"WebSphere:feature=restConnector,type=FileTransfer,name=FileTransfer");
			if (client.isRegistered(fileTransferServiceMBean)) {
				FileTransferMBean bean = JMX.newMBeanProxy(client, fileTransferServiceMBean, FileTransferMBean.class);
				bean.uploadFile(artifact.getSourcePath().getAbsolutePath(),
						"${server.output.dir}/dropins/" + artifact.getSourcePath().getName(), false);
			} else {
				throw new Exception("FileTransfer MBean not registered on WebSphere Liberty Profile");
			}
		} catch (Exception e) {
			throw new DeploymentServiceException("Failed to install artifact: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * uninstallArtifact(java.lang.String)
	 */
	public void uninstallArtifact(String name) {
		try {
			ObjectName fileTransferServiceMBean = new ObjectName(
					"WebSphere:feature=restConnector,type=FileTransfer,name=FileTransfer");
			if (client.isRegistered(fileTransferServiceMBean)) {
				FileTransferMBean bean = JMX.newMBeanProxy(client, fileTransferServiceMBean, FileTransferMBean.class);
				bean.deleteFile("${server.output.dir}/dropins/" + name);
			} else {
				throw new Exception("FileTransfer MBean not registered on WebSphere Liberty Profile");
			}
		} catch (Exception e) {
			throw new DeploymentServiceException("Failed to uninstall artifact: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * startArtifact(java.lang.String)
	 */
	public void startArtifact(String name) {
		try {
			ObjectName applicationMBean = new ObjectName(WEB_SPHERE_SERVICE_M_BEAN_NAME + name);
			if (client.isRegistered(applicationMBean)) {
				ApplicationMBean bean = JMX.newMBeanProxy(client, applicationMBean, ApplicationMBean.class);
				bean.start();
			} else {
				throw new Exception("Application '" + name + "' is not installed");
			}
		} catch (Exception e) {
			throw new DeploymentServiceException("Failed to start artifact: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * stopArtifact(java.lang.String)
	 */
	public void stopArtifact(String name) {
		try {
			ObjectName applicationMBean = new ObjectName(WEB_SPHERE_SERVICE_M_BEAN_NAME + name);
			if (client.isRegistered(applicationMBean)) {
				ApplicationMBean bean = JMX.newMBeanProxy(client, applicationMBean, ApplicationMBean.class);
				bean.stop();
			} else {
				throw new Exception("Application '" + name + "' is not installed");
			}
		} catch (Exception e) {
			throw new DeploymentServiceException("Failed to stop artifact: " + e.getMessage());
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
			ObjectName applicationMBean = new ObjectName(WEB_SPHERE_SERVICE_M_BEAN_NAME + name);
			return client.isRegistered(applicationMBean);
		} catch (Exception e) {
			e.printStackTrace();
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
		ClassLoader loader = getClass().getClassLoader();
		if (loader.toString().startsWith("AntClassLoader")) { // for development
																// running under
																// maven hpi:run
			// Liberty jars won't be found unless the following happens
			Thread.currentThread().setContextClassLoader(loader);
		}
		System.setProperty("javax.net.ssl.trustStore", getTrustStoreLocation().getAbsolutePath());
		System.setProperty("javax.net.ssl.trustStorePassword", getTrustStorePassword());

		HashMap<String, Object> environment = new HashMap<String, Object>();
		environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
		environment.put("com.ibm.ws.jmx.connector.client.disableURLHostnameVerification", true);
		environment.put(JMXConnector.CREDENTIALS, new String[] { getUsername(), getPassword() });

		JMXServiceURL url = new JMXServiceURL(
				"service:jmx:rest://" + getHost() + ":" + getPort() + "/IBMJMXConnectorREST");
		connector = JMXConnectorFactory.newJMXConnector(url, environment);
		connector.connect();
		client = connector.getMBeanServerConnection();
		if (client == null) {
			throw new Exception("Failed to connect to IBM WebSphere Liberty Profile");
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
		if (connector != null) {
			try {
				connector.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				connector = null;
			}
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
			Class.forName("com.ibm.ws.jmx.connector.client.rest.ClientProvider", false, getClass().getClassLoader());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		return connector != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * updateArtifact(org.jenkinsci.plugins.websphere.services.deployment.
	 * Artifact)
	 */
	@Override
	public void updateArtifact(Artifact artifact) {
		throw new UnsupportedOperationException();
	}
}
