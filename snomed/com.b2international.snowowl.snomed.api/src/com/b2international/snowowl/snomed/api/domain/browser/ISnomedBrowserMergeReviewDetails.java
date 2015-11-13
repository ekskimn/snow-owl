package com.b2international.snowowl.snomed.api.domain.browser;

import java.util.Set;

public interface ISnomedBrowserMergeReviewDetails {
	public String getId();

	public Set<ISnomedBrowserConcept> getSourceChanges();

	public Set<ISnomedBrowserConcept> getTargetChanges();

	public Set<ISnomedBrowserConcept> getMergedChanges();

}
