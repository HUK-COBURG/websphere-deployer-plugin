/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.util.Properties;

import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.application.AppNotification;

import hudson.model.BuildListener;

/**
 * The listener interface for receiving deploymentNotification events. The class
 * that is interested in processing a deploymentNotification event implements
 * this interface, and the object created with that class is registered with a
 * component using the component's <code>addDeploymentNotificationListener<code>
 * method. When the deploymentNotification event occurs, that object's
 * appropriate method is invoked.
 *
 * @see DeploymentNotificationEvent
 */
public class DeploymentNotificationListener implements NotificationListener {

	/** The admin client. */
	private AdminClient adminClient;

	/** The filter support. */
	private NotificationFilterSupport filterSupport;

	/** The object name. */
	private ObjectName objectName;

	/** The event type to check. */
	private String eventTypeToCheck;

	/** The successful. */
	private boolean successful = true;

	/** The message. */
	private String message = "";

	/** The notification props. */
	private Properties notificationProps = new Properties();

	/** The listener. */
	private BuildListener listener;

	/** The verbose. */
	private boolean verbose;

	/**
	 * Instantiates a new deployment notification listener.
	 *
	 * @param adminClient
	 *            the admin client
	 * @param support
	 *            the support
	 * @param handBack
	 *            the hand back
	 * @param eventTypeToCheck
	 *            the event type to check
	 * @param listener
	 *            the listener
	 * @param verbose
	 *            the verbose
	 * @throws Exception
	 *             the exception
	 */
	public DeploymentNotificationListener(AdminClient adminClient, NotificationFilterSupport support, Object handBack,
			String eventTypeToCheck, BuildListener listener, boolean verbose) throws Exception {
		super();
		this.adminClient = adminClient;
		this.filterSupport = support;
		this.eventTypeToCheck = eventTypeToCheck;
		this.listener = listener;
		this.verbose = verbose;
		this.objectName = (ObjectName) adminClient.queryNames(new ObjectName("WebSphere:type=AppManagement,*"), null)
				.iterator().next();
		adminClient.addNotificationListener(objectName, this, filterSupport, handBack);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.management.NotificationListener#handleNotification(javax.management
	 * .Notification, java.lang.Object)
	 */
	public void handleNotification(Notification notification, Object handback) {
		AppNotification appNotification = (AppNotification) notification.getUserData();
		if (verbose) {
			listener.getLogger().println(
					appNotification.taskName + "] " + appNotification.message + "[" + appNotification.taskStatus + "]");
		}
		message += ("\n" + appNotification.message);
		if (appNotification.taskName.equals(eventTypeToCheck)
				&& (appNotification.taskStatus.equals(AppNotification.STATUS_COMPLETED)
						|| appNotification.taskStatus.equals(AppNotification.STATUS_FAILED))) {
			try {
				adminClient.removeNotificationListener(objectName, this);
				if (appNotification.taskStatus.equals(AppNotification.STATUS_FAILED)) {
					successful = false;
				} else {
					notificationProps = appNotification.props;
				}

				synchronized (this) {
					notifyAll();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets the message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the notification props.
	 *
	 * @return the notification props
	 */
	public Properties getNotificationProps() {
		return notificationProps;
	}

	/**
	 * Checks if is successful.
	 *
	 * @return true, if is successful
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Gets the admin client.
	 *
	 * @return the admin client
	 */
	public AdminClient getAdminClient() {
		return adminClient;
	}

	/**
	 * Gets the filter support.
	 *
	 * @return the filter support
	 */
	public NotificationFilterSupport getFilterSupport() {
		return filterSupport;
	}

	/**
	 * Gets the object name.
	 *
	 * @return the object name
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

}