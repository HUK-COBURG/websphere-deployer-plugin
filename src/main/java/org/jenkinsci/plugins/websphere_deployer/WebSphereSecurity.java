/*
 * 
 */
package org.jenkinsci.plugins.websphere_deployer;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.util.Scrambler;

/**
 * The Class WebSphereSecurity.
 */
public class WebSphereSecurity {

	/** The username. */
	private String username;

	/** The password. */
	private String password;

	/** The client key file. */
	private String clientKeyFile;

	/** The client key password. */
	private String clientKeyPassword;

	/** The client trust file. */
	private String clientTrustFile;

	/** The client trust password. */
	private String clientTrustPassword;

	/**
	 * Instantiates a new web sphere security.
	 *
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param clientKeyFile
	 *            the client key file
	 * @param clientKeyPassword
	 *            the client key password
	 * @param clientTrustFile
	 *            the client trust file
	 * @param clientTrustPassword
	 *            the client trust password
	 */
	@DataBoundConstructor
	public WebSphereSecurity(String username, String password, String clientKeyFile, String clientKeyPassword,
			String clientTrustFile, String clientTrustPassword) {
		this.username = username;
		setPassword(password);
		this.clientKeyFile = clientKeyFile;
		setClientKeyPassword(clientKeyPassword);
		this.clientTrustFile = clientTrustFile;
		setClientTrustPassword(clientTrustPassword);
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
	 * Sets the username.
	 *
	 * @param username
	 *            the new username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return Scrambler.descramble(password);
	}

	/**
	 * Sets the password.
	 *
	 * @param password
	 *            the new password
	 */
	public void setPassword(String password) {
		this.password = Scrambler.scramble(password);
	}

	/**
	 * Gets the client key file.
	 *
	 * @return the client key file
	 */
	public String getClientKeyFile() {
		return clientKeyFile;
	}

	/**
	 * Sets the client key file.
	 *
	 * @param clientKeyFile
	 *            the new client key file
	 */
	public void setClientKeyFile(String clientKeyFile) {
		this.clientKeyFile = clientKeyFile;
	}

	/**
	 * Gets the client key password.
	 *
	 * @return the client key password
	 */
	public String getClientKeyPassword() {
		return Scrambler.descramble(clientKeyPassword);
	}

	/**
	 * Sets the client key password.
	 *
	 * @param clientKeyPassword
	 *            the new client key password
	 */
	public void setClientKeyPassword(String clientKeyPassword) {
		this.clientKeyPassword = Scrambler.scramble(clientKeyPassword);
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
	 * Sets the client trust file.
	 *
	 * @param clientTrustFile
	 *            the new client trust file
	 */
	public void setClientTrustFile(String clientTrustFile) {
		this.clientTrustFile = clientTrustFile;
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
	 * Sets the client trust password.
	 *
	 * @param clientTrustPassword
	 *            the new client trust password
	 */
	public void setClientTrustPassword(String clientTrustPassword) {
		this.clientTrustPassword = Scrambler.scramble(clientTrustPassword);
	}
}
