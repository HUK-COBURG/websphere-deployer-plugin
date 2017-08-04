/*
 * 
 */
package org.jenkinsci.plugins.websphere.services.deployment;

/**
 * The Class Server.
 */
public class Server implements Comparable<Server> {

	/** The object name. */
	private String objectName;

	/** The target. */
	private String target;

	/** The selected. */
	private boolean selected;

	/** The index. */
	private int index;

	/**
	 * Gets the index.
	 *
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index.
	 *
	 * @param index
	 *            the new index
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Gets the object name.
	 *
	 * @return the object name
	 */
	public String getObjectName() {
		return objectName;
	}

	/**
	 * Sets the object name.
	 *
	 * @param objectName
	 *            the new object name
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * Checks if is selected.
	 *
	 * @return true, if is selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * Sets the selected.
	 *
	 * @param selected
	 *            the new selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Gets the target.
	 *
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Sets the target.
	 *
	 * @param target
	 *            the new target
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Server other) {
		return getTarget().compareTo(other.getTarget());
	}
}
