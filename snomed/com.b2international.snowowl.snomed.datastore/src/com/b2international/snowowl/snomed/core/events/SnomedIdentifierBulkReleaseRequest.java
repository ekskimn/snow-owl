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
package com.b2international.snowowl.snomed.core.events;

import java.util.Collection;

import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;

/**
 * @since 4.5
 */
final class SnomedIdentifierBulkReleaseRequest extends BaseRequest<BranchContext, Boolean> {

	private final Collection<String> componentIds;

	SnomedIdentifierBulkReleaseRequest(final Collection<String> componentIds) {
		this.componentIds = componentIds;
	}

	@Override
	public Boolean execute(BranchContext context) {
		context.service(ISnomedIdentifierService.class).release(componentIds);
		return Boolean.TRUE;
	}

	@Override
	protected Class<Boolean> getReturnType() {
		return Boolean.class;
	}

}
