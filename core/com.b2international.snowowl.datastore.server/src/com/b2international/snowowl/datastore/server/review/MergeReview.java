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
package com.b2international.snowowl.datastore.server.review;


/**
 * Represents a terminology review comparing changes on branches.
 *
 * @since 4.2
 */
public interface MergeReview {

	/**
	 * Returns the unique identifier of this review.
	 */
	String id();
	
	/**
	 * Sets the unique identifier of the source to target review
	 */
	void setSourceToTargetReviewId(String sourceToTargetReviewId);
	
	/**
	 * Returns the unique identifier of the source to target review
	 */
	String getSourceToTargetReviewId();

	/**
	 * Sets the unique identifier of the target to source review
	 */
	void setTargetToSourceReviewId(String targetToSourceReviewId);
	
	/**
	 * Returns the unique identifier of the target to source review
	 */
	String getTargetToSourceReviewId();

}
