package com.b2international.snowowl.snomed.api.rest.domain;

import com.b2international.snowowl.datastore.review.ReviewStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface MergeReviewMixin {
	
	@JsonProperty
	String id();

	@JsonProperty
	ReviewStatus status();

	@JsonProperty
	String sourcePath();

	@JsonProperty
	String targetPath();

	@JsonProperty
	String sourceToTargetReviewId();

	@JsonProperty
	String targetToSourceReviewId();

}
