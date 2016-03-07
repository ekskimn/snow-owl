package com.b2international.snowowl.snomed.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.exceptions.InvalidStateException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.ISnomedMergeReviewService;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdate;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.mergereview.ISnomedBrowserMergeReviewDetail;
import com.b2international.snowowl.snomed.api.impl.domain.SnomedBrowserMergeReviewDetail;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;

public class SnomedMergeReviewServiceImpl implements ISnomedMergeReviewService {

	@Resource
	private IEventBus bus;
	
	@Resource
	protected ISnomedBrowserService browserService;
	
	@Resource
	private SnomedManualConceptMergeServiceImpl manualConceptMergeService;
	
	private final ExecutorService executors;
	
	public SnomedMergeReviewServiceImpl() {
		executors = Executors.newFixedThreadPool(3);
	}

	@Override
	public Set<ISnomedBrowserMergeReviewDetail> getMergeReviewDetails(String mergeReviewId, List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException {
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		Set<String> mergeReviewIntersection = mergeReview.mergeReviewIntersection();
		return getConceptDetails(mergeReview, mergeReviewIntersection, extendedLocales);
	}
	
	private Set<ISnomedBrowserMergeReviewDetail> getConceptDetails(
			final MergeReview mergeReview, Set<String> mergeReviewIntersection,
			final List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException {
		
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();

		List<Future<SnomedBrowserMergeReviewDetail>> futures = new ArrayList<Future<SnomedBrowserMergeReviewDetail>>();
		
		for (final String conceptId : mergeReviewIntersection) {
			
			futures.add(executors.submit(new Callable<SnomedBrowserMergeReviewDetail>() {

				@Override
				public SnomedBrowserMergeReviewDetail call() throws Exception {
					ISnomedBrowserConcept sourceConcept = browserService.getConceptDetails(SnomedServiceHelper.createComponentRef(sourcePath, conceptId), extendedLocales);
					ISnomedBrowserConcept targetConcept = browserService.getConceptDetails(SnomedServiceHelper.createComponentRef(targetPath, conceptId), extendedLocales);
					ISnomedBrowserConcept autoMergedConcept = mergeConcepts(sourceConcept, targetConcept, extendedLocales);
					final ISnomedBrowserConceptUpdate manuallyMergedConcept = manualConceptMergeService.exists(targetPath, mergeReview.id(), sourceConcept.getConceptId()) ?
							manualConceptMergeService.retrieve(targetPath, mergeReview.id(), sourceConcept.getConceptId()) : null;
					return new SnomedBrowserMergeReviewDetail(sourceConcept, targetConcept, autoMergedConcept, manuallyMergedConcept);
				}
			}));
			
		}

		Set<ISnomedBrowserMergeReviewDetail> details = new HashSet<ISnomedBrowserMergeReviewDetail>();
		for (Future<SnomedBrowserMergeReviewDetail> future : futures) {
				details.add(future.get());
		}
	
		return details;
	}

	private SnomedBrowserConcept mergeConcepts(
			ISnomedBrowserConcept sourceConcept,
			ISnomedBrowserConcept targetConcept, 
			List<ExtendedLocale> locales) {
		SnomedBrowserConcept mergedConcept = new SnomedBrowserConcept();
		// If one of the concepts is unpublished, then it's values are newer.  If both are unpublished, source would win
		ISnomedBrowserConcept winner = sourceConcept;
		if (targetConcept.getEffectiveTime() == null && sourceConcept.getEffectiveTime() != null) {
			winner = targetConcept;
		}
		// Set directly owned values
		mergedConcept.setConceptId(winner.getConceptId());
		mergedConcept.setActive(winner.isActive());
		mergedConcept.setDefinitionStatus(winner.getDefinitionStatus());
		mergedConcept.setEffectiveTime(winner.getEffectiveTime());
		mergedConcept.setModuleId(winner.getModuleId());
		mergedConcept.setIsLeafInferred(winner.getIsLeafInferred());
		mergedConcept.setIsLeafStated(winner.getIsLeafStated());
		
		// Merge Descriptions - take all the descriptions from source, and add in from target
		// if they're unpublished, which will cause an overwrite in the Set if the Description Id matches
		// TODO UNLESS the source description is also unpublished (Change to use map?)
		Set<ISnomedBrowserDescription> mergedDescriptions = new HashSet<ISnomedBrowserDescription>(sourceConcept.getDescriptions());
		for (ISnomedBrowserDescription thisDescription : targetConcept.getDescriptions()) {
			if (thisDescription.getEffectiveTime() == null) {
				mergedDescriptions.add(thisDescription);
			}
		}
		mergedConcept.setDescriptions(new ArrayList<ISnomedBrowserDescription>(mergedDescriptions));
		
		// Merge Relationships  - same process using Set to remove duplicated
		Set<ISnomedBrowserRelationship> mergedRelationships = new HashSet<ISnomedBrowserRelationship>(sourceConcept.getRelationships());
		for (ISnomedBrowserRelationship thisRelationship : targetConcept.getRelationships()) {
			if (thisRelationship.getEffectiveTime() == null) {
				mergedRelationships.add(thisRelationship);
			}
		}
		mergedConcept.setRelationships(new ArrayList<ISnomedBrowserRelationship>(mergedRelationships));
		
		return mergedConcept;
	}
	
	@Override
	public Merge mergeAndReplayConceptUpdates(final String mergeReviewId, final String userId, final List<ExtendedLocale> extendedLocales) throws IOException {
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();

		// Check we have a full set of manually merged concepts 
		final Set<String> mergeReviewIntersection = mergeReview.mergeReviewIntersection();
		final List<ISnomedBrowserConceptUpdate> conceptUpdates = new ArrayList<ISnomedBrowserConceptUpdate>();
		for (String conceptId : mergeReviewIntersection) {
			if (!manualConceptMergeService.exists(targetPath, mergeReviewId, conceptId)) {
				throw new InvalidStateException("Manually merged concept " + conceptId + " does not exist for merge review " + mergeReviewId);
			}
			conceptUpdates.add(manualConceptMergeService.retrieve(targetPath, mergeReviewId, conceptId));	
		}

		// Auto merge branches
		return SnomedRequests
			.merging()
			.prepareCreate()
			.setSource(sourcePath)
			.setTarget(targetPath)
			.setReviewId(mergeReview.sourceToTargetReviewId())
			.setCommitComment("Auto merging branches before applying manually merged concepts. " + sourcePath + " > " + targetPath)
			.setPostCommitRunnable(new Runnable() { @Override public void run() {
				// Apply manually merged concepts
				browserService.update(targetPath, conceptUpdates, userId, extendedLocales);

				// Clean up
				mergeReview.delete();
				manualConceptMergeService.deleteAll(targetPath, mergeReviewId);					
			}})
			.build()
			.executeSync(bus);
	}

	@Override
	public void persistManualConceptMerge(MergeReview mergeReview,
			ISnomedBrowserConceptUpdate concept) {
		manualConceptMergeService.storeConceptChanges(mergeReview.targetPath(), mergeReview.id(), concept);
	}
	
	private MergeReview getMergeReview(String mergeReviewId) {
		return SnomedRequests.mergeReview().prepareGet(mergeReviewId).executeSync(bus);
	}


}
