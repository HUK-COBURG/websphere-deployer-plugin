/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

/**
 * The Class DeploymentServiceException.
 *
 * @author Greg Peters
 */
public class DeploymentServiceException extends RuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new deployment service exception.
	 *
	 * @param message
	 *            the message
	 * @param t
	 *            the t
	 */
	public DeploymentServiceException(String message, Throwable t) {
		super(message, t);
	}

	/**
	 * Instantiates a new deployment service exception.
	 *
	 * @param message
	 *            the message
	 */
	public DeploymentServiceException(String message) {
		super(message);
	}
}
