package com.matthewcasperson.dzonezl.jpa;

import java.io.Serializable;
import javax.persistence.*;


/**
 * The persistent class for the Author database table.
 * 
 */
@Entity
@NamedQuery(name="Author.findAll", query="SELECT a FROM Author a")
public class Author implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private String name;
	private String username;
	private MvbDomain mvbdomain;

	public Author() {
	}


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}


	//bi-directional many-to-one association to MvbDomain
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="mvbDomainId")
	public MvbDomain getMvbdomain() {
		return this.mvbdomain;
	}

	public void setMvbdomain(MvbDomain mvbdomain) {
		this.mvbdomain = mvbdomain;
	}

}