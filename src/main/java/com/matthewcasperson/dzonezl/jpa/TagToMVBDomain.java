package com.matthewcasperson.dzonezl.jpa;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.security.Role;

import java.io.Serializable;
import javax.persistence.*;


/**
 * The persistent class for the TagToMVBDomain database table.
 * 
 */
@SharePermission(any={Role.ALL.class })
@Entity
@NamedQuery(name="TagToMVBDomain.findAll", query="SELECT t FROM TagToMVBDomain t")
public class TagToMVBDomain implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private Tag tag;
	private MvbDomain mvbdomain;

	public TagToMVBDomain() {
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