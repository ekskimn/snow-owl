/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.api.impl.codesystem.domain;

import com.b2international.snowowl.api.codesystem.domain.ICodeSystem;

/**
 */
public class CodeSystem implements ICodeSystem {

	private String oid;
	private String name;
	private String shortName;
	private String organizationLink;
	private String primaryLanguage;
	private String citation;

	@Override
	public String getOid() {
		return oid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getShortName() {
		return shortName;
	}

	@Override
	public String getOrganizationLink() {
		return organizationLink;
	}

	@Override
	public String getPrimaryLanguage() {
		return primaryLanguage;
	}

	@Override
	public String getCitation() {
		return citation;
	}

	public void setOid(final String oid) {
		this.oid = oid;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	public void setOrganizationLink(final String organizationLink) {
		this.organizationLink = organizationLink;
	}

	public void setPrimaryLanguage(final String primaryLanguage) {
		this.primaryLanguage = primaryLanguage;
	}

	public void setCitation(final String citation) {
		this.citation = citation;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CodeSystem [oid=");
		builder.append(oid);
		builder.append(", name=");
		builder.append(name);
		builder.append(", shortName=");
		builder.append(shortName);
		builder.append(", organizationLink=");
		builder.append(organizationLink);
		builder.append(", primaryLanguage=");
		builder.append(primaryLanguage);
		builder.append(", citation=");
		builder.append(citation);
		builder.append("]");
		return builder.toString();
	}
}