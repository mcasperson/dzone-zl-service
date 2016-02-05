package com.matthewcasperson.dzonezl.jpa;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.Role;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the tag database table.
 * 
 */
@SharePermission(any={Role.ALL.class })
@UpdatePermission(any={Role.ALL.class })
@Entity
@NamedQuery(name="Tag.findAll", query="SELECT t FROM Tag t")
public class Tag implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private String name;
	private List<TagToImage> tagToImages;
	private List<TagToMVBDomain> tagToMvbdomains;

	public Tag() {
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


	//bi-directional many-to-one association to TagToImage
	@OneToMany(mappedBy="tag")
	public List<TagToImage> getTagToImages() {
		return this.tagToImages;
	}

	public void setTagToImages(List<TagToImage> tagToImages) {
		this.tagToImages = tagToImages;
	}

	public TagToImage addTagToImage(TagToImage tagToImage) {
		getTagToImages().add(tagToImage);
		tagToImage.setTag(this);

		return tagToImage;
	}

	public TagToImage removeTagToImage(TagToImage tagToImage) {
		getTagToImages().remove(tagToImage);
		tagToImage.setTag(null);

		return tagToImage;
	}


	//bi-directional many-to-one association to TagToMVBDomain
	@OneToMany(mappedBy="tag")
	public List<TagToMVBDomain> getTagToMvbdomains() {
		return this.tagToMvbdomains;
	}

	public void setTagToMvbdomains(List<TagToMVBDomain> tagToMvbdomains) {
		this.tagToMvbdomains = tagToMvbdomains;
	}

	public TagToMVBDomain addTagToMvbdomain(TagToMVBDomain tagToMvbdomain) {
		getTagToMvbdomains().add(tagToMvbdomain);
		tagToMvbdomain.setTag(this);

		return tagToMvbdomain;
	}

	public TagToMVBDomain removeTagToMvbdomain(TagToMVBDomain tagToMvbdomain) {
		getTagToMvbdomains().remove(tagToMvbdomain);
		tagToMvbdomain.setTag(null);

		return tagToMvbdomain;
	}

}