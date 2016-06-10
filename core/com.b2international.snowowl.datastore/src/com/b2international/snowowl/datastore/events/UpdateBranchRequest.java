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
package com.b2international.snowowl.datastore.events;

import com.b2international.snowowl.core.Metadata;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.domain.RepositoryContext;

public final class UpdateBranchRequest extends BranchRequest<Branch> {

	private Metadata metadata;

	public UpdateBranchRequest(final String branchPath, final Metadata metadata) {
		super(branchPath);
		this.metadata = metadata;
	}

	@Override
	public Branch execute(RepositoryContext context) {
		final BranchManager service = context.service(BranchManager.class);
		return service.updateBranchMetadata(getBranchPath(), metadata);
	}

	@Override
	protected Class<Branch> getReturnType() {
		return Branch.class;
	}
}
