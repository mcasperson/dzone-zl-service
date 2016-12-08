package com.matthewcasperson.dzonezl.jpa;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.Role;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the MvbDomain database table.
 * 
 */
@SharePermission(any={Role.ALL.class })
@UpdatePermission(any={Role.ALL.class })
@Entity
@NamedQuery(name="MvbDomain.findAll", query="SELECT m FROM MvbDomain m")
@Table(name="MVBDomain")
public class MvbDomain implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private String domain;
	private String name;
	private Integer daysBeforePublishing;
	private String emailWhenPublishing;
	private List<Author> authors;
	private List<TagToMVBDomain> tagToMvbdomains;

	public MvbDomain() {
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

	@Column(name = "daysBeforePublishing")
	public Integer getDaysBeforePublishing() {
		return this.daysBeforePublishing;
	}

	public void setDaysBeforePublishing(Integer daysBeforePublishing) {
		this.daysBeforePublishing = daysBeforePublishing;
	}

	public String getEmailWhenPublishing() {
		return this.emailWhenPublishing;
	}

	public void setEmailWhenPublishing(String emailWhenPublishing) {
		this.emailWhenPublishing = emailWhenPublishing;
	}


	//bi-directional many-to-one association to Author
	@OneToMany(mappedBy="mvbdomain")
	public List<Author> getAuthors() {
		return this.authors;
	}

	public void setAuthors(List<Author> authors) {
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
	public List<TagToMVBDomain> getTagToMvbdomains() {
		return this.tagToMvbdomains;
	}

	public void setTagToMvbdomains(List<TagToMVBDomain> tagToMvbdomains) {
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