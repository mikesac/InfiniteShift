package org.infinite.db.dto;

// Generated 8-lug-2009 10.08.07 by Hibernate Tools 3.2.4.CR1

/**
 * TomcatRoles generated by hbm2java
 */
public class TomcatRoles implements java.io.Serializable {

	private static final long serialVersionUID = -4169326741600827218L;
	
	private String user;
	private TomcatUsers tomcatUsers;
	private String role;

	public TomcatRoles() {
	}

	public TomcatRoles(TomcatUsers tomcatUsers, String role) {
		this.tomcatUsers = tomcatUsers;
		this.role = role;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public TomcatUsers getTomcatUsers() {
		return this.tomcatUsers;
	}

	public void setTomcatUsers(TomcatUsers tomcatUsers) {
		this.tomcatUsers = tomcatUsers;
	}

	public String getRole() {
		return this.role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
