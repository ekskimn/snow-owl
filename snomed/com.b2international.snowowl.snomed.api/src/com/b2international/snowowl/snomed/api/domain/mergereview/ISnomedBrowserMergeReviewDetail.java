package com.b2international.snowowl.snomed.api.domain.mergereview;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;

public interface ISnomedBrowserMergeReviewDetail {

	ISnomedBrowserConcept getSourceConcept();

	ISnomedBrowserConcept getTargetConcept();

	ISnomedBrowserConcept getAutoMergedConcept();

	ISnomedBrowserConcept getManuallyMergedConcept();

}
