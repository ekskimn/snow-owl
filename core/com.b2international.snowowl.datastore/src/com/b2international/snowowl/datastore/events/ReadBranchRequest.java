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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.datastore.oplock.IOperationLockTarget;
import com.b2international.snowowl.datastore.oplock.OperationLockInfo;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContext;
import com.b2international.snowowl.datastore.oplock.impl.IDatastoreOperationLockManager;
import com.b2international.snowowl.datastore.oplock.impl.SingleRepositoryAndBranchLockTarget;

/**
 * @since 4.1
 */
public final class ReadBranchRequest extends BranchRequest<Branch> {

	private final boolean expandLock;

	public ReadBranchRequest(final String branchPath, boolean expandLock) {
		super(branchPath);
		this.expandLock = expandLock;
	}
	
	@Override
	public Branch execute(RepositoryContext context) {
		Branch branch = context.service(BranchManager.class).getBranch(getBranchPath());
		
		if (expandLock) {
			final IDatastoreOperationLockManager lockManager = ApplicationContext.getInstance().getService(IDatastoreOperationLockManager.class);
			if (lockManager != null) {
				final List<OperationLockInfo<DatastoreLockContext>> locks = lockManager.getLocks();
				for (OperationLockInfo<DatastoreLockContext> operationLockInfo : locks) {
					final IOperationLockTarget target = operationLockInfo.getTarget();
					if (target instanceof SingleRepositoryAndBranchLockTarget) {
						SingleRepositoryAndBranchLockTarget lockTarget = (SingleRepositoryAndBranchLockTarget) target;
						if (lockTarget.getBranchPath().equals(branch.branchPath())) {
							Map<String, Object> lockInfo = new HashMap<>();
							lockInfo.put("creationDate", operationLockInfo.getCreationDate());
							lockInfo.put("context", operationLockInfo.getContext());
							branch.metadata().put("lock", lockInfo);
						}
					}
				}
			}
		}

		return branch;
	}
	
	@Override
	protected Class<Branch> getReturnType() {
		return Branch.class;
	}
	
}
