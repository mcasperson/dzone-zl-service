package com.matthewcasperson.dzonezl.jpa;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Set;


/**
 * The persistent class for the Image database table.
 * 
 */
@Entity
@NamedQuery(name="Image.findAll", query="SELECT i FROM Image i")
public class Image implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String description;
	private int dzoneId;
	private String name;
	private Set<TagToImage> tagToImages;

	public Image() {
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


	public int getDzoneId() {
		return this.dzoneId;
	}

	public void setDzoneId(int dzoneId) {
		this.dzoneId = dzoneId;
	}


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}


	//bi-directional many-to-one association to TagToImage
	@OneToMany(mappedBy="image")
	public Set<TagToImage> getTagToImages() {
		return this.tagToImages;
	}

	public void setTagToImages(Set<TagToImage> tagToImages) {
		this.tagToImages = tagToImages;
	}

	public TagToImage addTagToImage(TagToImage tagToImage) {
		getTagToImages().add(tagToImage);
		tagToImage.setImage(this);

		return tagToImage;
	}

	public TagToImage removeTagToImage(TagToImage tagToImage) {
		getTagToImages().remove(tagToImage);
		tagToImage.setImage(null);

		return tagToImage;
	}

}