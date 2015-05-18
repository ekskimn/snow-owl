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
package com.b2international.snowowl.api.impl.domain;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.cdo.common.branch.CDOBranch;

import com.b2international.commons.collections.Procedure;
import com.b2international.snowowl.api.codesystem.exception.CodeSystemNotFoundException;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.events.util.AsyncSupport;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.IBranchPathMap;
import com.b2international.snowowl.datastore.ICodeSystem;
import com.b2international.snowowl.datastore.TerminologyRegistryService;
import com.b2international.snowowl.datastore.UserBranchPathMap;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.server.branch.Branch;
import com.b2international.snowowl.datastore.server.events.BranchReply;
import com.b2international.snowowl.datastore.server.events.ReadBranchEvent;
import com.b2international.snowowl.eventbus.IEventBus;

/**
 * @since 1.0
 */
public class StorageRef implements InternalStorageRef {

	private static final IBranchPathMap MAIN_BRANCH_PATH_MAP = new UserBranchPathMap();

	private static ICDOConnectionManager getConnectionManager() {
		return ApplicationContext.getServiceForClass(ICDOConnectionManager.class);
	}

	private static TerminologyRegistryService getRegistryService() {
		return ApplicationContext.getServiceForClass(TerminologyRegistryService.class);
	}
	
	private static IEventBus getEventBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}

	private String shortName;
	private String branchPath;
	private Branch branch;

	@Override
	public String getShortName() {
		return shortName;
	}
	
	@Override
	public String getBranchPath() {
		return branchPath;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}
	
	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
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

	@Override
	public Branch getBranch() {
		if (branch == null) {
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<Branch> ref = new AtomicReference<>();
			new AsyncSupport<>(getEventBus(), BranchReply.class).send(new ReadBranchEvent(getRepositoryUuid(), getBranchPath()))
				.then(new Procedure<BranchReply>() {
					@Override
					protected void doApply(BranchReply input) {
						ref.set(input.getBranch());
						latch.countDown();
					}
				}).fail(new Procedure<Throwable>() {
					@Override
					protected void doApply(Throwable input) {
						latch.countDown();
					}
				});
			try {
				if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
					throw new SnowowlRuntimeException("Timeout when reading branch: " + getRepositoryUuid() + ", path: " + getBranchPath());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			branch = ref.get();
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
	public void checkStorageExists() {
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