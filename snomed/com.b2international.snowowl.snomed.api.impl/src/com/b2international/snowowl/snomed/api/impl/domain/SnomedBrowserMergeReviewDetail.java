package com.b2international.snowowl.snomed.api.impl.domain;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.mergereview.ISnomedBrowserMergeReviewDetail;

public class SnomedBrowserMergeReviewDetail implements ISnomedBrowserMergeReviewDetail {

	private ISnomedBrowserConcept sourceConcept;
	private ISnomedBrowserConcept targetConcept;
	private ISnomedBrowserConcept autoMergedConcept;
	private ISnomedBrowserConcept manuallyMergedConcept;
	
	public SnomedBrowserMergeReviewDetail(ISnomedBrowserConcept sourceConcept,
			ISnomedBrowserConcept targetConcept,
			ISnomedBrowserConcept autoMergedConcept,
			ISnomedBrowserConcept manuallyMergedConcept) {
		this.sourceConcept = sourceConcept;
		this.targetConcept = targetConcept;
		this.autoMergedConcept = autoMergedConcept;
		this.manuallyMergedConcept = manuallyMergedConcept;
	}

	@Override
	public ISnomedBrowserConcept getSourceConcept() {
		return sourceConcept;
	}

	@Override
	public ISnomedBrowserConcept getTargetConcept() {
		return targetConcept;
	}

	@Override
	public ISnomedBrowserConcept getAutoMergedConcept() {
		return autoMergedConcept;
	}

	@Override
	public ISnomedBrowserConcept getManuallyMergedConcept() {
		return manuallyMergedConcept;
	}
	
}
