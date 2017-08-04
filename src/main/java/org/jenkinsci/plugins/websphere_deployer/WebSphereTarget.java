/*
 * 
 */
package org.jenkinsci.plugins.websphere_deployer;

/**
 * The Class WebSphereTarget.
 */
public class WebSphereTarget {

	/** The cell. */
	private String cell;

	/** The node. */
	private String node;

	/** The server. */
	private String server;

	/** The cluster. */
	private String cluster;

	/** The selected. */
	private boolean selected;

	/**
	 * Gets the cell.
	 *
	 * @return the cell
	 */
	public String getCell() {
		return cell;
	}

	/**
	 * Sets the cell.
	 *
	 * @param cell
	 *            the new cell
	 */
	public void setCell(String cell) {
		this.cell = cell;
	}

	/**
	 * Gets the node.
	 *
	 * @return the node
	 */
	public String getNode() {
		return node;
	}

	/**
	 * Sets the node.
	 *
	 * @param node
	 *            the new node
	 */
	public void setNode(String node) {
		this.node = node;
	}

	/**
	 * Gets the server.
	 *
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Sets the server.
	 *
	 * @param server
	 *            the new server
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * Gets the cluster.
	 *
	 * @return the cluster
	 */
	public String getCluster() {
		return cluster;
	}

	/**
	 * Sets the cluster.
	 *
	 * @param cluster
	 *            the new cluster
	 */
	public void setCluster(String cluster) {
		this.cluster = cluster;
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
}
