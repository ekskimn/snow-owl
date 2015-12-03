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
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.datastore.events.CreateMergeReviewRequest;
import com.b2international.snowowl.datastore.review.MergeReview;

public final class MergeReviewCreateRequestBuilder {
	
	private String sourceBranch;
	private String targetBranch;
	
	private final String repositoryId;

	MergeReviewCreateRequestBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public MergeReviewCreateRequestBuilder setSource(String source) {
		this.sourceBranch = source;
		return this;
	}
	
	public MergeReviewCreateRequestBuilder setTarget(String target) {
		this.targetBranch = target;
		return this;
	}
	
	public Request<ServiceProvider, MergeReview> build() {
		return RepositoryRequests.wrap(repositoryId, new CreateMergeReviewRequest(sourceBranch, targetBranch));
	}

}
