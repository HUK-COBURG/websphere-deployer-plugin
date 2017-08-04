/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

import java.io.File;

/**
 * The Class Artifact.
 */
public class Artifact {

	/** The Constant TYPE_EAR. */
	public static final int TYPE_EAR = 1;

	/** The Constant TYPE_WAR. */
	public static final int TYPE_WAR = 2;

	/** The Constant TYPE_JAR. */
	public static final int TYPE_JAR = 3;

	/** The Constant TYPE_RAR. */
	public static final int TYPE_RAR = 4;

	/** The source path. */
	private File sourcePath;

	/** The app name. */
	private String appName;

	/** The context. */
	private String context;

	/** The targets. */
	private String targets;

	/**
	 * Gets the virtual host.
	 *
	 * @return the virtual host
	 */
	public String getVirtualHost() {
		return virtualHost;
	}

	/**
	 * Sets the virtual host.
	 *
	 * @param virtualHost
	 *            the new virtual host
	 */
	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/** The virtual host. */
	private String virtualHost;

	/** The type. */
	private int type;

	/** The distribute. */
	private boolean distribute;

	/** The precompile. */
	private boolean precompile;

	/** The jsp reloading. */
	private boolean jspReloading;

	/** The reloading. */
	private boolean reloading;

	/** The install path. */
	private String installPath;

	/** The class loader order. */
	private String classLoaderOrder;

	/** The class loader policy. */
	private String classLoaderPolicy;

	/**
	 * Gets the type name.
	 *
	 * @return the type name
	 */
	public String getTypeName() {
		switch (type) {
		case TYPE_EAR: {
			return "ear";
		}
		case TYPE_WAR: {
			return "war";
		}
		case TYPE_JAR: {
			return "jar";
		}
		case TYPE_RAR: {
			return "rar";
		}
		default: {
			return "ear";
		}
		}
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
	 * Sets the class loader order.
	 *
	 * @param classLoaderOrder
	 *            the new class loader order
	 */
	public void setClassLoaderOrder(String classLoaderOrder) {
		this.classLoaderOrder = classLoaderOrder;
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
	 * Sets the class loader policy.
	 *
	 * @param classLoaderPolicy
	 *            the new class loader policy
	 */
	public void setClassLoaderPolicy(String classLoaderPolicy) {
		this.classLoaderPolicy = classLoaderPolicy;
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
	 * Sets the reloading.
	 *
	 * @param reloading
	 *            the new reloading
	 */
	public void setReloading(boolean reloading) {
		this.reloading = reloading;
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
	 * Sets the targets.
	 *
	 * @param targets
	 *            the new targets
	 */
	public void setTargets(String targets) {
		this.targets = targets;
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
	 * Sets the jsp reloading.
	 *
	 * @param jspReloading
	 *            the new jsp reloading
	 */
	public void setJspReloading(boolean jspReloading) {
		this.jspReloading = jspReloading;
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
	 * Sets the precompile.
	 *
	 * @param precompile
	 *            the new precompile
	 */
	public void setPrecompile(boolean precompile) {
		this.precompile = precompile;
	}

	/**
	 * Gets the app name.
	 *
	 * @return the app name
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * Sets the app name.
	 *
	 * @param appName
	 *            the new app name
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

	/**
	 * Sets the type.
	 *
	 * @param type
	 *            the new type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Gets the source path.
	 *
	 * @return the source path
	 */
	public File getSourcePath() {
		return sourcePath;
	}

	/**
	 * Sets the source path.
	 *
	 * @param sourcePath
	 *            the new source path
	 */
	public void setSourcePath(File sourcePath) {
		this.sourcePath = sourcePath;
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
	 * Sets the context.
	 *
	 * @param context
	 *            the new context
	 */
	public void setContext(String context) {
		this.context = context;
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
	 * Sets the distribute.
	 *
	 * @param distribute
	 *            the new distribute
	 */
	public void setDistribute(boolean distribute) {
		this.distribute = distribute;
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
	 * Sets the install path.
	 *
	 * @param installPath
	 *            the new install path
	 */
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}
}
