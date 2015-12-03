package com.b2international.snowowl.snomed.api;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdate;
import com.b2international.snowowl.snomed.api.domain.mergereview.ISnomedBrowserMergeReviewDetail;

public interface ISnomedMergeReviewService {

	Set<ISnomedBrowserMergeReviewDetail> getMergeReviewDetails(String mergeReviewId, List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException;

	void mergeAndReplayConceptUpdates(String mergeReviewId, String userId, List<ExtendedLocale> extendedLocales) throws IOException;

	void persistManualConceptMerge(MergeReview mergeReview, ISnomedBrowserConceptUpdate concept);

}
