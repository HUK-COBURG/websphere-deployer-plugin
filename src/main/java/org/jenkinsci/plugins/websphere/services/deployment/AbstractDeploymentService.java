/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.io.File;

/**
 * The Class AbstractDeploymentService.
 *
 * @author Greg Peters
 */
public abstract class AbstractDeploymentService implements DeploymentService {

	/** The trust store location. */
	private File trustStoreLocation;

	/** The key store location. */
	private File keyStoreLocation;

	/** The trust store password. */
	private String trustStorePassword;

	/** The key store password. */
	private String keyStorePassword;

	/** The username. */
	private String username;

	/** The password. */
	private String password;

	/** The host. */
	private String host;

	/** The port. */
	private String port;

	/**
	 * Gets the trust store location.
	 *
	 * @return the trust store location
	 */
	public File getTrustStoreLocation() {
		return trustStoreLocation;
	}

	/**
	 * Gets the key store location.
	 *
	 * @return the key store location
	 */
	public File getKeyStoreLocation() {
		return keyStoreLocation;
	}

	/**
	 * Gets the trust store password.
	 *
	 * @return the trust store password
	 */
	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	/**
	 * Gets the key store password.
	 *
	 * @return the key store password
	 */
	public String getKeyStorePassword() {
		return keyStorePassword;
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
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Gets the host.
	 *
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setPassword(java.lang.String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setUsername(java.lang.String)
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setHost(java.lang.String)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setPort(java.lang.String)
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setTrustStoreLocation(java.io.File)
	 */
	public void setTrustStoreLocation(File location) {
		this.trustStoreLocation = location;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setKeyStoreLocation(java.io.File)
	 */
	public void setKeyStoreLocation(File location) {
		this.keyStoreLocation = location;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setTrustStorePassword(java.lang.String)
	 */
	public void setTrustStorePassword(String password) {
		this.trustStorePassword = password;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jenkinsci.plugins.websphere.services.deployment.DeploymentService#
	 * setKeyStorePassword(java.lang.String)
	 */
	public void setKeyStorePassword(String password) {
		this.keyStorePassword = password;
	}

}
