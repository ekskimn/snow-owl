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
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.Request;

/**
 * @since 4.5
 */
public class RepositoryRequests {

	private RepositoryRequests() {
	}
	
	public static <B> Request<ServiceProvider, B> wrap(String repositoryId, Request<RepositoryContext, B> next) {
		return new RepositoryRequest<>(repositoryId, next);
	}
	
	public static Branching branching(String repositoryId) {
		return new Branching(repositoryId);
	}
	
	public static Merging merging(String repositoryId) {
		return new Merging(repositoryId);
	}
	
	public static Reviews reviews(String repositoryId) {
		return new Reviews(repositoryId);
	}

	public static MergeReviews mergeReviews(String repositoryId) {
		return new MergeReviews(repositoryId);
	}
	public static DeleteRequestBuilder prepareDelete(String repositoryId) {
		return new DeleteRequestBuilder(repositoryId);
	}
}
