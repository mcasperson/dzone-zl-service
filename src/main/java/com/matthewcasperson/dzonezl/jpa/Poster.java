package com.matthewcasperson.dzonezl.jpa;

import java.io.Serializable;
import javax.persistence.*;


/**
 * The persistent class for the poster database table.
 * 
 */
@Entity
@Table(name="poster")
@NamedQuery(name="Poster.findAll", query="SELECT p FROM Poster p")
public class Poster implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private String name;
	private int username;

	public Poster() {
	}


	@Id
	@Column(unique=true, nullable=false)
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}


	@Column(length=255)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	@Column(length=45)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}


	@Column(nullable=false)
	public int getUsername() {
		return this.username;
	}

	public void setUsername(int username) {
		this.username = username;
	}

}