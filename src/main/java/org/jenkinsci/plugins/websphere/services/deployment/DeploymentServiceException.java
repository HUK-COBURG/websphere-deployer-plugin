package org.jenkinsci.plugins.websphere.services.deployment;

/**
 * @author Greg Peters
 */
public class DeploymentServiceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DeploymentServiceException(String message, Throwable t) {
		super(message, t);
	}

	public DeploymentServiceException(String message) {
		super(message);
	}
}
