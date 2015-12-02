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
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;

/**
 * @since 4.5
 */
public final class SnomedRefSetCreateRequestBuilder implements RequestBuilder<TransactionContext, String> {

	private SnomedRefSetType type;
	private String referencedComponentType;
	private Request<TransactionContext, String> conceptReq;

	SnomedRefSetCreateRequestBuilder() {
	}

	public SnomedRefSetCreateRequestBuilder setType(SnomedRefSetType type) {
		this.type = type;
		return this;
	}
	
	public SnomedRefSetCreateRequestBuilder setReferencedComponentType(String referencedComponentType) {
		this.referencedComponentType = referencedComponentType;
		return this;
	}
	
	public SnomedRefSetCreateRequestBuilder setIdentifierConcept(SnomedConceptCreateRequestBuilder conceptReq) {
		this.conceptReq = conceptReq.build();
		return this;
	}
	
	public SnomedRefSetCreateRequestBuilder setIdentifierConcept(Request<TransactionContext, String> conceptReq) {
		this.conceptReq = conceptReq;
		return this;
	}
	
	@Override
	public Request<TransactionContext, String> build() {
		return new SnomedRefSetCreateRequest(type, referencedComponentType, (SnomedConceptCreateRequest) conceptReq);
	}
	
}
