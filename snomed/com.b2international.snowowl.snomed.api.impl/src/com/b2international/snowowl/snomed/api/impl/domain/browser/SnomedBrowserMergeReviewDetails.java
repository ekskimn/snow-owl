package com.b2international.snowowl.snomed.api.impl.domain.browser;

import java.util.HashSet;
import java.util.Set;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserMergeReviewDetails;

public class SnomedBrowserMergeReviewDetails implements ISnomedBrowserMergeReviewDetails {
	
	private String id;
	
	private Set<ISnomedBrowserConcept> sourceChanges;

	private Set<ISnomedBrowserConcept> targetChanges;

	private Set<ISnomedBrowserConcept> mergedChanges;

	public SnomedBrowserMergeReviewDetails() {
		sourceChanges = new HashSet<ISnomedBrowserConcept>();
		targetChanges = new HashSet<ISnomedBrowserConcept>();
		mergedChanges = new HashSet<ISnomedBrowserConcept>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<ISnomedBrowserConcept> getSourceChanges() {
		return sourceChanges;
	}

	public void setSourceChanges(Set<ISnomedBrowserConcept> sourceChanges) {
		this.sourceChanges = sourceChanges;
	}

	public Set<ISnomedBrowserConcept> getTargetChanges() {
		return targetChanges;
	}

	public void setTargetChanges(Set<ISnomedBrowserConcept> targetChanges) {
		this.targetChanges = targetChanges;
	}

	public Set<ISnomedBrowserConcept> getMergedChanges() {
		return mergedChanges;
	}

	public void setMergedChanges(Set<ISnomedBrowserConcept> mergedChanges) {
		this.mergedChanges = mergedChanges;
	}
}
