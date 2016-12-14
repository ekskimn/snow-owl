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
package com.b2international.snowowl.datastore.server.internal.merge;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.status.Statuses;
import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.exceptions.ApiError;
import com.b2international.snowowl.core.exceptions.ApiException;
import com.b2international.snowowl.core.exceptions.MergeConflictException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.server.remotejobs.BranchExclusiveRule;

/**
 * @since 4.6
 */
public abstract class AbstractBranchChangeJob extends Job {

	private static final Logger LOGGER = LoggerFactory.getLogger("merge-job");
	
	public static AbstractBranchChangeJob create(Repository repository, UUID id, String source, String target, String commitMessage, String reviewId) {
		final IBranchPath sourcePath = BranchPathUtils.createPath(source);
		final IBranchPath targetPath = BranchPathUtils.createPath(target);
		
		if (targetPath.getParent().equals(sourcePath)) {
			return new BranchRebaseJob(repository, id, source, target, commitMessage, reviewId);
		} else {
			return new BranchMergeJob(repository, id, source, target, commitMessage, reviewId);
		}
	}

	protected MergeImpl merge;
	
	protected Repository repository;
	protected String commitComment;
	protected String reviewId;

	protected AbstractBranchChangeJob(final Repository repository, UUID id, final String sourcePath, final String targetPath, final String commitComment, final String reviewId) {
		super(commitComment);
		
		this.repository = repository;
		this.merge = new MergeImpl(id, sourcePath, targetPath);
		this.commitComment = commitComment;
		this.reviewId = reviewId;
		
		setSystem(true);
		
		setRule(MultiRule.combine(
				new BranchExclusiveRule(repository.id(), BranchPathUtils.createPath(sourcePath)), 
				new BranchExclusiveRule(repository.id(), BranchPathUtils.createPath(targetPath))));
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		merge.start();
		
		try {
			applyChanges();
			merge.completed();
		} catch (MergeConflictException e) {
			merge.failedWithConflicts(e.getConflicts(), e.toApiError());
		} catch (ApiException e) {
			LOGGER.error("Caught ApiException while applying changes for merge {}.", merge.getId(), e);
			merge.failed(e.toApiError());
		} catch (RuntimeException e) {
			LOGGER.error("Caught RuntimeException while applying changes for merge {}.", merge.getId(), e);
			merge.failed(ApiError.Builder.of(e.getMessage()).build());
		} finally {
			// Send a notification event with the final state to the global event bus
			repository.events().publish(String.format(Merge.ADDRESS_TEMPLATE, repository.id(), merge.getId()), merge);
		}
		
		return Statuses.ok();
	}
	
	protected abstract void applyChanges();

	@Override
	protected void canceling() {
		merge.cancelRequested();
		super.canceling();
	}

	public Merge getMerge() {
		return merge;
	}
}