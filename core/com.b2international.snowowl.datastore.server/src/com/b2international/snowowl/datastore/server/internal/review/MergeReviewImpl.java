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

import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.datastore.server.review.MergeReview;
import com.b2international.snowowl.datastore.server.review.Review;
import com.b2international.snowowl.datastore.server.review.ReviewStatus;

/**
 * @since 4.2
 */
public class MergeReviewImpl implements MergeReview {

	private final String id;
	private String sourceToTargetReviewId;
	private String targetToSourceReviewId;
	
	private ReviewManagerImpl reviewManager;
	private String lastUpdated;
	private ReviewStatus status;
	
	public MergeReviewImpl(String id) {
		this.id = id;
	}
	
	public MergeReviewImpl(final String id, final String sourceToTargetReviewId, final String targetToSourceReviewId, final String lastUpdated, final String status) {
		this.id = id;
		this.sourceToTargetReviewId = sourceToTargetReviewId;
		this.targetToSourceReviewId = targetToSourceReviewId;
	}
	
	public String id() {
		return this.id;
	}

	public String getSourceToTargetReviewId() {
		return sourceToTargetReviewId;
	}

	public void setSourceToTargetReviewId(String sourceToTargetReviewId) {
		this.sourceToTargetReviewId = sourceToTargetReviewId;
	}

	public String getTargetToSourceReviewId() {
		return targetToSourceReviewId;
	}

	public void setTargetToSourceReviewId(String targetToSourceReviewId) {
		this.targetToSourceReviewId = targetToSourceReviewId;
	}

	public void setReviewManager(ReviewManagerImpl reviewManager) {
		this.reviewManager = reviewManager;
	}
	
	public String getLastUpdated() {
		//return the later of the two reviews last updated
		Review sourceToTargetReview = reviewManager.getReview(sourceToTargetReviewId);
		Review targetToSourceReview = reviewManager.getReview(targetToSourceReviewId);
		Date left = Dates.parse(sourceToTargetReview.lastUpdated(), DateFormats.ISO_8601);
		Date right = Dates.parse(targetToSourceReview.lastUpdated(), DateFormats.ISO_8601);
		return left.after(right) ? Dates.formatByGmt(left, DateFormats.ISO_8601) : Dates.formatByGmt(right, DateFormats.ISO_8601);
	}
	
	public ReviewStatus getStatus() {
		//return the more relevant of the states of the two reviews
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
			throw new IllegalStateException ("Unexpected state combination: " + left.status() + " / " + right.status());
		}
	}
}
