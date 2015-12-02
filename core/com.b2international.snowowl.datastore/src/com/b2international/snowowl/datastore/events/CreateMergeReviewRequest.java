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

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.datastore.review.ReviewManager;

public class CreateMergeReviewRequest extends BaseRequest<RepositoryContext, MergeReview> {

	private final String sourcePath;
	private final String targetPath;

	public CreateMergeReviewRequest(final String sourcePath, final String targetPath) {
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
	}
	
	@Override
	public MergeReview execute(RepositoryContext context) {
		try {
			final BranchManager branchManager = context.service(BranchManager.class);
			final ReviewManager reviewManager = context.service(ReviewManager.class);
			
			final Branch source = branchManager.getBranch(sourcePath);
			final Branch target = branchManager.getBranch(targetPath);
			
			return reviewManager.createMergeReview(source, target);
		} catch (final NotFoundException e) {
			// Non-existent branches are reported as Bad Requests for reviews
			throw e.toBadRequestException();
		}
	}
	
	@Override
	protected Class<MergeReview> getReturnType() {
		return MergeReview.class;
	}

}
