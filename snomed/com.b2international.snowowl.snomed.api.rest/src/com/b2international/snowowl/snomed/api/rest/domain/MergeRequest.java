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
package com.b2international.snowowl.snomed.api.rest.domain;

import org.hibernate.validator.constraints.NotEmpty;

import com.b2international.snowowl.core.events.Event;
import com.b2international.snowowl.datastore.server.events.MergeEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @since 4.1
 */
@ApiModel
public class MergeRequest {

	@ApiModelProperty(required = true)
	@JsonProperty
	@NotEmpty
	private String source;

	@ApiModelProperty(required = true)
	@JsonProperty
	@NotEmpty
	private String target;

	@ApiModelProperty(required = false)
	@JsonProperty
	private String commitComment;
	
	@ApiModelProperty(required = false)
	@JsonProperty
	private String reviewId;
	
	public Event toEvent(final String repositoryId) {
		return new MergeEvent(repositoryId, source, target, commitComment, reviewId);
	}
}
