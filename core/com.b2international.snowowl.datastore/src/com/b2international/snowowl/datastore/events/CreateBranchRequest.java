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
import com.b2international.snowowl.core.exceptions.NotFoundException;


/**
 * @since 4.1
 */
public final class CreateBranchRequest extends BranchRequest<Branch> {

	private final String parent;
	private final String name;
	private final Metadata metadata;

	public CreateBranchRequest(final String parent, final String name, final Metadata metadata) {
		super(path(parent, name));
		this.parent = parent;
		this.name = name;
		this.metadata = metadata;
	}

	private static String path(final String parent, final String name) {
		return parent + Branch.SEPARATOR + name;
	}

	public String getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public Metadata getMetadata() {
		return metadata;
	}
	
	@Override
	public Branch execute(RepositoryContext context) {
		try {
			final Branch parent = context.service(BranchManager.class).getBranch(getParent());
			return parent.createChild(getName(), getMetadata());
		} catch (NotFoundException e) {
			// if parent not found, convert it to BadRequestException
			throw e.toBadRequestException();
		}
	}
	
	@Override
	protected Class<Branch> getReturnType() {
		return Branch.class;
	}
}
