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
package com.b2international.snowowl.datastore.server.domain;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.exceptions.CodeSystemNotFoundException;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.IBranchPathMap;
import com.b2international.snowowl.datastore.ICodeSystem;
import com.b2international.snowowl.datastore.TerminologyRegistryService;
import com.b2international.snowowl.datastore.UserBranchPathMap;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.eventbus.IEventBus;

/**
 * @since 1.0
 */
public class StorageRef implements InternalStorageRef {

	private static final IBranchPathMap MAIN_BRANCH_PATH_MAP = new UserBranchPathMap();
	private static final long DEFAULT_ASYNC_TIMEOUT_DELAY = 20000; // TODO: Put back to 5000 after debugging
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageRef.class);

	private static ICDOConnectionManager getConnectionManager() {
		return ApplicationContext.getServiceForClass(ICDOConnectionManager.class);
	}

	private static TerminologyRegistryService getRegistryService() {
		return ApplicationContext.getServiceForClass(TerminologyRegistryService.class);
	}
	
	private static IEventBus getEventBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}

	private final String shortName;
	private final String branchPath;
	private Branch branch;

	public StorageRef(String codeSystem, String branchPath) {
		this.shortName = codeSystem;
		this.branchPath = branchPath;
	}
	
	@Override
	public String getShortName() {
		return shortName;
	}
	
	@Override
	public String getBranchPath() {
		return branchPath;
	}

	@Override
	public ICodeSystem getCodeSystem() {
		// XXX: in case of a non-MAIN-registered code system, we would need a repository UUID to get the code system to get the repository UUID
		final ICodeSystem codeSystem = getRegistryService().getCodeSystemByShortName(MAIN_BRANCH_PATH_MAP, shortName);
		if (null != codeSystem) {
			return codeSystem;
		}

		throw new CodeSystemNotFoundException(shortName);
	}

	@Override
	public String getRepositoryUuid() {
		return getCodeSystem().getRepositoryUuid();
	}

	protected final void setBranch(Branch branch) {
		this.branch = branch;
	}
	
	@Override
	public Branch getBranch() {
		if (branch == null) {
			LOGGER.info("Requesting branch {}", branchPath);
			branch = RepositoryRequests
						.branching(getRepositoryUuid())
						.prepareGet(branchPath)
						.executeSync(getEventBus(), DEFAULT_ASYNC_TIMEOUT_DELAY);
		}
		if (branch == null) {
			throw new NotFoundException("Branch", getBranchPath());
		}
		return branch;
	}

	@Override
	public CDOBranch getCdoBranch() {
		final CDOBranch cdoBranch = getCdoBranchOrNull();
		if (null != cdoBranch) {
			return cdoBranch;
		}

		throw new NotFoundException("Branch", getBranchPath());
	}

	private CDOBranch getCdoBranchOrNull() {
		return getConnectionManager().getByUuid(getRepositoryUuid()).getBranch(getBranch().branchPath());
	}

	@Override
	public final void checkStorageExists() {
		if (getBranch().isDeleted()) {
			throw new BadRequestException("Branch '%s' has been deleted and cannot accept further modifications.", getBranchPath());
		}
		getCdoBranch();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("StorageRef [shortName=");
		builder.append(shortName);
		builder.append(", branchPath=");
		builder.append(branchPath);
		builder.append("]");
		return builder.toString();
	}
}