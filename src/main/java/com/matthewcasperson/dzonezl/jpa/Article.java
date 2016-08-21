package com.matthewcasperson.dzonezl.jpa;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.Role;

import javax.persistence.*;
import java.io.Serializable;


/**
 * The persistent class for the Article database table.
 * 
 */
@SharePermission(any={Role.ALL.class })
@UpdatePermission(any={Role.ALL.class })
@Entity
@NamedQuery(name="Article.findAll", query="SELECT a FROM Article a")
public class Article implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String source;

	public Article() {
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}


	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}