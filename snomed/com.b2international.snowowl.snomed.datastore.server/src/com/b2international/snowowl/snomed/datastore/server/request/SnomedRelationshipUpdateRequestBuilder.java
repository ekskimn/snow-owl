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
package com.b2international.snowowl.snomed.datastore.server.request;

import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;

/**
 * @since 4.5
 */
public class SnomedRelationshipUpdateRequestBuilder extends BaseSnomedComponentUpdateRequestBuilder<SnomedRelationshipUpdateRequestBuilder, SnomedRelationshipUpdateRequest> {

	private CharacteristicType characteristicType;
	private Integer group;
	private Integer unionGroup;
	private RelationshipModifier modifier;

	SnomedRelationshipUpdateRequestBuilder(String repositoryId, String componentId) {
		super(repositoryId, componentId);
	}
	
	public SnomedRelationshipUpdateRequestBuilder setCharacteristicType(CharacteristicType characteristicType) {
		this.characteristicType = characteristicType;
		return getSelf();
	}
	
	public SnomedRelationshipUpdateRequestBuilder setGroup(Integer group) {
		this.group = group;
		return getSelf();
	}
	
	public SnomedRelationshipUpdateRequestBuilder setModifier(RelationshipModifier modifier) {
		this.modifier = modifier;
		return getSelf();
	}
	
	public SnomedRelationshipUpdateRequestBuilder setUnionGroup(Integer unionGroup) {
		this.unionGroup = unionGroup;
		return getSelf();
	}
	
	@Override
	protected SnomedRelationshipUpdateRequest create(String componentId) {
		return new SnomedRelationshipUpdateRequest(componentId);
	}
	
	@Override
	protected void init(SnomedRelationshipUpdateRequest req) {
		super.init(req);
		req.setCharacteristicType(characteristicType);
		req.setGroup(group);
		req.setUnionGroup(unionGroup);
		req.setModifier(modifier);
	}

	@Override
	protected SnomedRelationshipUpdateRequestBuilder getSelf() {
		return this;
	}

}
