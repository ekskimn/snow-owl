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
package com.b2international.snowowl.snomed.api.rest.domain;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.core.domain.IdGenerationStrategy;
import com.b2international.snowowl.snomed.core.domain.NamespaceIdGenerationStrategy;
import com.b2international.snowowl.snomed.core.domain.UserIdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedComponentCreateRequestBuilder;

/**
 * @since 4.0
 */
public abstract class AbstractSnomedComponentRestInput<I extends SnomedComponentCreateRequestBuilder<I>> {

	private String id;
	private String moduleId;
	private String namespaceId;

	/**
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return
	 */
	public String getModuleId() {
		return moduleId;
	}
	
	/**
	 * @return
	 */
	public String getNamespaceId() {
		return namespaceId;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setModuleId(final String moduleId) {
		this.moduleId = moduleId;
	}
	
	public void setNamespaceId(final String namespaceId) {
		this.namespaceId = namespaceId;
	}

	protected abstract I createComponentInput();

	/**
	 * Returns with the component category for the concrete SNOMED&nbsp;CT component input.
	 * @return the component category of the concrete component.
	 */
	protected abstract ComponentCategory getComponentCategory();
	
	protected I toComponentInput() {
		final I req = createComponentInput();
		req.setId(createIdGenerationStrategy(getId()));
		req.setModuleId(getModuleId());
		return req;
	}

	protected IdGenerationStrategy createIdGenerationStrategy(final String idOrNull) {
		return createIdGenerationStrategy(idOrNull, getComponentCategory());
	}

	protected IdGenerationStrategy createIdGenerationStrategy(final String idOrNull, ComponentCategory componentCategory) {
		if (null == idOrNull) {
			return new NamespaceIdGenerationStrategy(componentCategory, getNamespaceId());
		} else {
			return new UserIdGenerationStrategy(idOrNull);
		}
	}
}
