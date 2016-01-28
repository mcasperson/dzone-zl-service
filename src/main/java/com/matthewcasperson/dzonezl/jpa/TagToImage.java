package com.matthewcasperson.dzonezl.jpa;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.Role;

import java.io.Serializable;
import javax.persistence.*;


/**
 * The persistent class for the TagToImage database table.
 * 
 */
@SharePermission(any={Role.ALL.class })
@Entity
@NamedQuery(name="TagToImage.findAll", query="SELECT t FROM TagToImage t")
public class TagToImage implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private Tag tag;
	private Image image;

	public TagToImage() {
	}


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}


	//bi-directional many-to-one association to Tag
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="tagId")
	public Tag getTag() {
		return this.tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}


	//bi-directional many-to-one association to Image
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="imageId")
	public Image getImage() {
		return this.image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

}