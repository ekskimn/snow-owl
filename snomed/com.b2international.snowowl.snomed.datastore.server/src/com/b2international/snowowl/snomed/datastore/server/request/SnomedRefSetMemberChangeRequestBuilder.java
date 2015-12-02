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

import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.RequestBuilder;
import com.b2international.snowowl.snomed.core.domain.refset.MemberChange;

/**
 * @since 4.5
 */
public class SnomedRefSetMemberChangeRequestBuilder implements RequestBuilder<TransactionContext, Void> {

	private final MemberChange change;
	private final String moduleId;
	private final String referenceSetId;

	SnomedRefSetMemberChangeRequestBuilder(MemberChange change, String moduleId, String referenceSetId) {
		this.change = change;
		this.moduleId = moduleId;
		this.referenceSetId = referenceSetId;
	}
	
	@Override
	public Request<TransactionContext, Void> build() {
		switch (change.getChangeKind()) {
		case ADD:
			return SnomedRequests
					.prepareNewMember()
					.setModuleId(moduleId)
					.setReferencedComponentId(change.getReferencedComponentId())
					.setReferenceSetId(referenceSetId)
					.buildNoContent();
		case REMOVE:
			// FIXME what happens when we remove a published member, currently we don't inactivate it
			return SnomedRequests.prepareDeleteMember(change.getMemberId());
		default: throw new UnsupportedOperationException("Not implemented case: " + change.getChangeKind()); 
		}
	}

}
