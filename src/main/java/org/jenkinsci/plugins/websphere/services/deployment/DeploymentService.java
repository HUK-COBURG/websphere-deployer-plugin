/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.io.File;

/**
 * The Interface DeploymentService.
 */
public interface DeploymentService {

	/**
	 * Install artifact.
	 *
	 * @param artifact
	 *            the artifact
	 */
	void installArtifact(Artifact artifact);

	/**
	 * Update artifact.
	 *
	 * @param artifact
	 *            the artifact
	 */
	void updateArtifact(Artifact artifact);

	/**
	 * Uninstall artifact.
	 *
	 * @param name
	 *            the name
	 * @throws Exception
	 *             the exception
	 */
	void uninstallArtifact(String name) throws Exception;

	/**
	 * Start artifact.
	 *
	 * @param name
	 *            the name
	 * @throws Exception
	 *             the exception
	 */
	void startArtifact(String name) throws Exception;

	/**
	 * Stop artifact.
	 *
	 * @param name
	 *            the name
	 * @throws Exception
	 *             the exception
	 */
	void stopArtifact(String name) throws Exception;

	/**
	 * Checks if is artifact installed.
	 *
	 * @param name
	 *            the name
	 * @return true, if is artifact installed
	 */
	boolean isArtifactInstalled(String name);

	/**
	 * Sets the trust store location.
	 *
	 * @param location
	 *            the new trust store location
	 */
	void setTrustStoreLocation(File location);

	/**
	 * Sets the key store location.
	 *
	 * @param location
	 *            the new key store location
	 */
	void setKeyStoreLocation(File location);

	/**
	 * Sets the trust store password.
	 *
	 * @param password
	 *            the new trust store password
	 */
	void setTrustStorePassword(String password);

	/**
	 * Sets the key store password.
	 *
	 * @param password
	 *            the new key store password
	 */
	void setKeyStorePassword(String password);

	/**
	 * Sets the host.
	 *
	 * @param host
	 *            the new host
	 */
	void setHost(String host);

	/**
	 * Sets the port.
	 *
	 * @param port
	 *            the new port
	 */
	void setPort(String port);

	/**
	 * Sets the username.
	 *
	 * @param username
	 *            the new username
	 */
	void setUsername(String username);

	/**
	 * Sets the password.
	 *
	 * @param password
	 *            the new password
	 */
	void setPassword(String password);

	/**
	 * Connect.
	 *
	 * @throws Exception
	 *             the exception
	 */
	void connect() throws Exception;

	/**
	 * Disconnect.
	 */
	void disconnect();

	/**
	 * Checks if is available.
	 *
	 * @return true, if is available
	 */
	boolean isAvailable();
}
