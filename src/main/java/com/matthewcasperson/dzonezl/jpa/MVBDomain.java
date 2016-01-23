package com.matthewcasperson.dzonezl.jpa;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Set;


/**
 * The persistent class for the MVBDomain database table.
 * 
 */
@Entity
@NamedQuery(name="MVBDomain.findAll", query="SELECT m FROM MVBDomain m")
public class MVBDomain implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private String domain;
	private String name;
	private Set<Author> authors;
	private Set<TagToMVBDomain> tagToMvbdomains;

	public MVBDomain() {
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


	public String getDomain() {
		return this.domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}


	//bi-directional many-to-one association to Author
	@OneToMany(mappedBy="mvbdomain")
	public Set<Author> getAuthors() {
		return this.authors;
	}

	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}

	public Author addAuthor(Author author) {
		getAuthors().add(author);
		author.setMvbdomain(this);

		return author;
	}

	public Author removeAuthor(Author author) {
		getAuthors().remove(author);
		author.setMvbdomain(null);

		return author;
	}


	//bi-directional many-to-one association to TagToMVBDomain
	@OneToMany(mappedBy="mvbdomain")
	public Set<TagToMVBDomain> getTagToMvbdomains() {
		return this.tagToMvbdomains;
	}

	public void setTagToMvbdomains(Set<TagToMVBDomain> tagToMvbdomains) {
		this.tagToMvbdomains = tagToMvbdomains;
	}

	public TagToMVBDomain addTagToMvbdomain(TagToMVBDomain tagToMvbdomain) {
		getTagToMvbdomains().add(tagToMvbdomain);
		tagToMvbdomain.setMvbdomain(this);

		return tagToMvbdomain;
	}

	public TagToMVBDomain removeTagToMvbdomain(TagToMVBDomain tagToMvbdomain) {
		getTagToMvbdomains().remove(tagToMvbdomain);
		tagToMvbdomain.setMvbdomain(null);

		return tagToMvbdomain;
	}

}