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
package com.b2international.snowowl.datastore.server.internal.review;

import static com.google.common.base.Preconditions.checkNotNull;

import com.b2international.snowowl.core.events.util.ApiEventHandler;
import com.b2international.snowowl.core.events.util.Handler;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.core.exceptions.NotImplementedException;
import com.b2international.snowowl.datastore.branch.Branch;
import com.b2international.snowowl.datastore.branch.BranchManager;
import com.b2international.snowowl.datastore.server.events.*;
import com.b2international.snowowl.datastore.server.review.ConceptChanges;
import com.b2international.snowowl.datastore.server.review.MergeReview;
import com.b2international.snowowl.datastore.server.review.MergeReviewIntersection;
import com.b2international.snowowl.datastore.server.review.ReviewManager;

/**
 * @since 4.2
 */
public class MergeReviewEventHandler extends ApiEventHandler {

	private final BranchManager branchManager;
	private final ReviewManager reviewManager;

	public MergeReviewEventHandler(final BranchManager branchManager, final ReviewManager reviewManager) {
		this.branchManager = checkNotNull(branchManager, "branchManager");
		this.reviewManager = checkNotNull(reviewManager, "reviewManager");
	}

	@Handler
	protected MergeReviewReply handle(final CreateMergeReviewEvent event) {
		try {

			final Branch source = branchManager.getBranch(event.getSourcePath());
			final Branch target = branchManager.getBranch(event.getTargetPath());
			return new MergeReviewReply(reviewManager.createMergeReview(source, target));

		} catch (final NotFoundException e) {
			// Non-existent branches are reported as Bad Requests for reviews
			throw e.toBadRequestException();
		}
	}

	@Handler
	protected MergeReviewReply handle(final ReadMergeReviewEvent event) {
		return new MergeReviewReply(getMergeReview(event));
	}

	@Handler
	protected MergeReviewDetailsReply handle(final ReadMergeReviewDetailsEvent event) {
		return new MergeReviewDetailsReply(getMergeReviewIntersection(event));
	}


	private MergeReview getMergeReview(final MergeReviewEvent event) {
		return reviewManager.getMergeReview(event.getReviewId());
	}

	private MergeReviewIntersection getMergeReviewIntersection(final MergeReviewEvent event) {
		return reviewManager.getMergeReviewIntersection(event.getReviewId());
	}
}
