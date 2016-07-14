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

import java.util.Date;
import java.util.Set;

import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.datastore.review.Review;
import com.b2international.snowowl.datastore.review.ReviewStatus;

public class MergeReviewImpl implements MergeReview {

	private final String id;
	private final String sourcePath;
	private final String targetPath;
	private final String sourceToTargetReviewId;
	private final String targetToSourceReviewId;
	
	private ReviewManagerImpl reviewManager;
	
	public MergeReviewImpl(final String id, final String sourcePath, final String targetPath, 
			final String sourceToTargetReviewId, final String targetToSourceReviewId) {
		this.id = id;
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
		this.sourceToTargetReviewId = sourceToTargetReviewId;
		this.targetToSourceReviewId = targetToSourceReviewId;
	}
	
	public String id() {
		return this.id;
	}

	public String sourceToTargetReviewId() {
		return sourceToTargetReviewId;
	}

	public String targetToSourceReviewId() {
		return targetToSourceReviewId;
	}

	public String lastUpdated() {
		// return the later of the two reviews last updated
		Review sourceToTargetReview = reviewManager.getReview(sourceToTargetReviewId);
		Review targetToSourceReview = reviewManager.getReview(targetToSourceReviewId);
		Date left = Dates.parse(sourceToTargetReview.lastUpdated(), DateFormats.ISO_8601);
		Date right = Dates.parse(targetToSourceReview.lastUpdated(), DateFormats.ISO_8601);
		return left.after(right) ? Dates.formatByGmt(left, DateFormats.ISO_8601) : Dates.formatByGmt(right, DateFormats.ISO_8601);
	}
	
	public ReviewStatus status() {
		// return the more relevant of the states of the two reviews
		try {
			Review left = reviewManager.getReview(sourceToTargetReviewId);
			Review right = reviewManager.getReview(targetToSourceReviewId);
			if (left.status() == ReviewStatus.FAILED || right.status() == ReviewStatus.FAILED) {
				return ReviewStatus.FAILED;
			} else if (left.status() == ReviewStatus.PENDING || right.status() == ReviewStatus.PENDING) {
				return ReviewStatus.PENDING;
			} else if (left.status() == ReviewStatus.STALE || right.status() == ReviewStatus.STALE) {
				return ReviewStatus.STALE;
			} else  if (left.status() == ReviewStatus.CURRENT && right.status() == ReviewStatus.CURRENT) {
				return ReviewStatus.CURRENT;
			} else {
				throw new IllegalStateException("Unexpected state combination: " + left.status() + " / " + right.status());
			}
		} catch (NotFoundException e) {
			return ReviewStatus.STALE;
		}
	}
	
	public String targetPath() {
		return targetPath;
	}

	public String sourcePath() {
		return sourcePath;
	}
	
	public void setReviewManager(ReviewManagerImpl reviewManager) {
		this.reviewManager = reviewManager;
	}

	@Override
	public MergeReview delete() {
		return reviewManager.deleteMergeReview(this);
	}

	@Override
	public Set<String> mergeReviewIntersection() {
		return reviewManager.getMergeReviewIntersection(this);
	}
	
}
