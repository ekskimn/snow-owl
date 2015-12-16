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
package com.b2international.snowowl.snomed.core.store;

import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.snomedrefset.SnomedAttributeValueRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetFactory;

/**
 * @since 4.5
 */
public final class SnomedAttributeValueReferenceSetMemberBuilder extends SnomedMemberBuilder<SnomedAttributeValueReferenceSetMemberBuilder, SnomedAttributeValueRefSetMember> {

	private String valueId;
	
	SnomedAttributeValueReferenceSetMemberBuilder() {
		super(ComponentCategory.SET_MEMBER);
	}
	
	public SnomedAttributeValueReferenceSetMemberBuilder withValueId(String valueId) {
		this.valueId = valueId;
		return getSelf();
	}
	
	@Override
	protected SnomedAttributeValueRefSetMember create() {
		return SnomedRefSetFactory.eINSTANCE.createSnomedAttributeValueRefSetMember();
	}
	
	@Override
	protected void init(SnomedAttributeValueRefSetMember component, TransactionContext context) {
		super.init(component, context);
		component.setValueId(valueId);
	}

}