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
package com.b2international.snowowl.datastore.request;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.core.merge.MergeService;
import com.b2international.snowowl.datastore.BranchPathUtils;

/**
 * @since 4.5
 * @deprecated Use requests in {@link Merging} for asynchronous operation
 */
@Deprecated
public final class BranchMergeRequestBuilder {
	
	private String source;
	private String target;
	private String commitComment;
	private String reviewId;
	
	private String repositoryId;
	
	BranchMergeRequestBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public BranchMergeRequestBuilder setSource(String source) {
		this.source = source;
		return this;
	}
	
	public BranchMergeRequestBuilder setTarget(String target) {
		this.target = target;
		return this;
	}
	
	public BranchMergeRequestBuilder setCommitComment(String commitComment) {
		this.commitComment = commitComment;
		return this;
	}
	
	public BranchMergeRequestBuilder setReviewId(String reviewId) {
		this.reviewId = reviewId;
		return this;
	}
	
	public Request<ServiceProvider, Merge> build() {
		final IBranchPath sourcePath = BranchPathUtils.createPath(source);
		final IBranchPath targetPath = BranchPathUtils.createPath(target);
		final BaseRequest<RepositoryContext, Merge> next;
		
		if (sourcePath.getParent().equals(targetPath)) {
			next = new BranchMergeRequest(source, target, commitComment, reviewId) {
				@Override
				protected Merge execute(RepositoryContext context, Branch source, Branch target) {
					return context.service(MergeService.class).executeBlocking(sourcePath, targetPath, commitMessage, reviewId);
				}
			};
		} else if (targetPath.getParent().equals(sourcePath)) {
			next = new BranchRebaseRequest(source, target, commitComment, reviewId) {
				@Override
				protected Merge execute(RepositoryContext context, Branch source, Branch target) {
					return context.service(MergeService.class).executeBlocking(sourcePath, targetPath, commitMessage, reviewId);
				}
			};
		} else {
			throw new BadRequestException("Branches '%s' and '%s' can only be merged or rebased if one branch is the direct parent of the other.", source, target);
		}
		
		return RepositoryRequests.wrap(repositoryId, next);
	}
}
