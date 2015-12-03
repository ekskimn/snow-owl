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
package com.b2international.snowowl.datastore.review;

import java.util.Set;


/**
 * Represents a terminology review comparing bidirectional changes on branches while catering for manual merges.
 */
public interface MergeReview {

	/**
	 * Returns the unique identifier of this review.
	 */
	String id();

	/**
	 * Returns the current status of this review.
	 */
	ReviewStatus status();

	/**
	 * Returns the branch used as the comparison source.
	 */
	String sourcePath();

	/**
	 * Returns the branch used as the comparison target.
	 */
	String targetPath();

	/**
	 * Deletes this review and corresponding concept changes from the review repository.
	 */
	MergeReview delete();

	/**
	 * Returns the last update time in ISO8601 format. Update time is registered at creation and whenever the review's
	 * state changes.
	 * 
	 * @return the time of last update
	 */
	String lastUpdated();
	
	/**
	 * Returns the unique identifier of the source to target review
	 */
	String sourceToTargetReviewId();
	
	/**
	 * Returns the unique identifier of the target to source review
	 */
	String targetToSourceReviewId();

	/**
	 * Retrieve the intersection of concepts modified in both source and target
	 **/
	Set<String> mergeReviewIntersection(); 
	
}
