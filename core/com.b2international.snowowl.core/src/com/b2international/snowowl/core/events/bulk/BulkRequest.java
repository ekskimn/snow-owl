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
package com.b2international.snowowl.core.events.bulk;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.core.events.Request;

/**
 * @since 4.5
 * @see BulkRequestBuilder
 */
public final class BulkRequest<C extends ServiceProvider> extends BaseRequest<C, BulkResponse> {

	private List<Request<C, ?>> requests;

	BulkRequest(List<Request<C, ?>> requests) {
		this.requests = requests == null ? Collections.<Request<C, ?>> emptyList() : requests;
	}

	@Override
	public BulkResponse execute(C context) {
		final List<Object> responses = newArrayList();
		
		for (Request<C, ?> req : requests) {
			responses.add(req.execute(context));
		}
		
		return new BulkResponse(responses);
	}

	@Override
	protected Class<BulkResponse> getReturnType() {
		return BulkResponse.class;
	}

	/**
	 * Creates a new {@link BulkRequestBuilder} instance to create a {@link BulkRequest}.
	 * 
	 * @return
	 */
	public static <C extends ServiceProvider> BulkRequestBuilder<C> create() {
		return new BulkRequestBuilder<C>();
	}
}
