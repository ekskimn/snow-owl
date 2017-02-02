/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.request;

import com.b2international.snowowl.snomed.datastore.request.SnomedComponentSearchRequest.OptionKey;

/**
 * @since 5.3
 */
public abstract class SnomedComponentSearchRequestBuilder<B extends SnomedComponentSearchRequestBuilder<B, R>, R> extends SnomedSearchRequestBuilder<B, R> {
	
	public final B isActiveMemberOf(String refSetId) {
		return addOption(OptionKey.ACTIVE_MEMBER_OF, refSetId);
	}
	
	/**
	 * Filter matches to have their ID from the specified namespace.
	 * 
	 * @param namespaceId
	 *            - the namespace identifier
	 * @return
	 */
	public final B filterByNamespace(String namespaceId) {
		return addOption(OptionKey.NAMESPACE, namespaceId);
	}

}
