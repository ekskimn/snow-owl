/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.internal.review;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.review.ConceptChanges;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.datastore.review.MergeReviewManager;
import com.b2international.snowowl.datastore.review.Review;
import com.b2international.snowowl.datastore.review.ReviewStatus;
import com.b2international.snowowl.datastore.store.Store;

/**
 * TODO migrate to new index api
 */
public class MergeReviewManagerImpl implements MergeReviewManager {

	private final ReviewManagerImpl reviews;
	private final Store<MergeReviewImpl> mergeReviewStore;

	public MergeReviewManagerImpl(Store<MergeReviewImpl> mergeReviewStore, ReviewManagerImpl reviews) {
		this.mergeReviewStore = mergeReviewStore;
		this.reviews = reviews;
	}
	
	@Override
	public MergeReview createMergeReview(Branch source, Branch target) {	
		Review sourceToTarget = reviews.createReview(source, target);
		Review targetToSource = reviews.createReview(target, source);

		MergeReviewImpl mergeReview = new MergeReviewImpl(UUID.randomUUID().toString(), source.path(), target.path(), sourceToTarget.id(), targetToSource.id());
		mergeReview.setMergeReviewManager(this);
		mergeReview.setReviewManager(reviews);

		synchronized (mergeReviewStore) {
			mergeReviewStore.put(mergeReview.id(), mergeReview);
		}
		
		return mergeReview;
	}

	@Override
	public MergeReview getMergeReview(String id) {
		final MergeReviewImpl mergeReview;
		synchronized (mergeReviewStore) {
			mergeReview = mergeReviewStore.get(id);
		}

		if (mergeReview == null) {
			throw new NotFoundException(MergeReview.class.getSimpleName(), id);
		} 

		mergeReview.setMergeReviewManager(this);
		mergeReview.setReviewManager(reviews);
		return mergeReview;
	}
	
	@Override
	public Set<String> getMergeReviewIntersection(MergeReview mergeReview) {
		// Get the concept changes for both source to target 
		// and target to source reviews
		
		// Are we all complete and still relevant?
		if (!mergeReview.status().equals(ReviewStatus.CURRENT)){
			throw new IllegalStateException ("Merge Review in invalid state - " + mergeReview.status());
		}
		
		ConceptChanges sourceChanges = reviews.getConceptChanges(mergeReview.sourceToTargetReviewId());
		ConceptChanges targetChanges = reviews.getConceptChanges(mergeReview.targetToSourceReviewId());
		Set<String>commonChanges = new HashSet<String>(sourceChanges.changedConcepts());
		// If concepts are new then they won't intersect.  Also if they're deleted, they won't
		// conflict, so we're only interested in the change set.
		commonChanges.retainAll(targetChanges.changedConcepts());
		
		return commonChanges;
	}
	
	public MergeReview deleteMergeReview(MergeReviewImpl mergeReview) {
		Review sourceToTargetReview = reviews.getReview(mergeReview.sourceToTargetReviewId());
		Review targetToSourceReview = reviews.getReview(mergeReview.targetToSourceReviewId());
		sourceToTargetReview.delete();
		targetToSourceReview.delete();
		
		synchronized (mergeReviewStore) {
			mergeReviewStore.remove(mergeReview.id());
		}
		
		return mergeReview;
	}

}
