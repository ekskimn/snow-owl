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
package com.b2international.snowowl.datastore.server.cdo;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.LogUtils;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.IBranchPathMap;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.eventbus.IEventBus;

/**
 * Creates task branches and freezes state in the parent branch index, so a differencing task index can be based on it.
 */
public class PrepareBranchAction extends AbstractCDOBranchAction {

	// Allowing 10 seconds for simultaneous task activation
	private static final long PREPARE_TIMEOUT = TimeUnit.SECONDS.toMillis(10L);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PrepareBranchAction.class);

	public PrepareBranchAction(final IBranchPathMap branchPathMap, final String userId) {
		super(branchPathMap, userId, DatastoreLockContextDescriptions.PREPARE, PREPARE_TIMEOUT);
	}

	@Override
	protected boolean isApplicable(String repositoryId, IBranchPath taskBranchPath) throws Throwable {
		if (!super.isApplicable(repositoryId, taskBranchPath)) {
			return false;
		}
		
		final ICDOConnection connection = getConnection(repositoryId);
		final IBranchPath parentBranchPath = taskBranchPath.getParent();
		final CDOBranch parentBranch = connection.getBranch(parentBranchPath);
		
		if (parentBranch == null) {
			throw new IllegalStateException(MessageFormat.format("Parent branch ''{0}'' not found on connection ''{1}''.", parentBranchPath, connection.getUuid()));
		}

		final CDOBranch taskBranch = connection.getBranch(taskBranchPath);
		return (taskBranch == null);
	}
	
	@Override
	protected void apply(final String repositoryId, final IBranchPath taskBranchPath) throws Throwable {

		final ICDOConnection connection = getConnection(repositoryId);
		final IBranchPath parentBranchPath = taskBranchPath.getParent();
		final CDOBranch taskBranch = connection.getBranch(taskBranchPath);

		if (taskBranch == null) {
			final String message = MessageFormat.format("Creating branch {0} in ''{1}''...", taskBranchPath, connection.getRepositoryName());
			LOGGER.info(message);
			LogUtils.logUserEvent(LOGGER, getUserId(), parentBranchPath, message);

			final IEventBus eventBus = ApplicationContext.getServiceForClass(IEventBus.class);
			
			RepositoryRequests.branching(repositoryId)
					.prepareCreate()
					.setParent(parentBranchPath.getPath())
					.setName(taskBranchPath.lastSegment())
					.build()
					.executeSync(eventBus);
		}
	}
}
