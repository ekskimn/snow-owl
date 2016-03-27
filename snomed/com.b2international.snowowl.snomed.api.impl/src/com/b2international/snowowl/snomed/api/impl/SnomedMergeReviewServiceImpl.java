package com.b2international.snowowl.snomed.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.commons.collections.BeanMap;

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
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

					if (hasRelevantChanges(sourceConcept, targetConcept)) {
						ISnomedBrowserConcept autoMergedConcept = mergeConcepts(sourceConcept, targetConcept, extendedLocales);
						ISnomedBrowserConceptUpdate manuallyMergedConcept = manualConceptMergeService.exists(targetPath, mergeReview.id(), conceptId) 
								? manualConceptMergeService.retrieve(targetPath, mergeReview.id(), conceptId) 
								: null;

						return new SnomedBrowserMergeReviewDetail(sourceConcept, targetConcept, autoMergedConcept, manuallyMergedConcept);
					} else {
						return null;
					}
				}
			}));
		}

		Set<ISnomedBrowserMergeReviewDetail> details = new HashSet<ISnomedBrowserMergeReviewDetail>();
		for (Future<SnomedBrowserMergeReviewDetail> future : futures) {
			SnomedBrowserMergeReviewDetail detail = future.get();
			if (detail != null) {
				details.add(detail);
			}
		}
	
		return details;
	}

	private boolean hasRelevantChanges(ISnomedBrowserConcept sourceConcept, ISnomedBrowserConcept targetConcept) {

		if (hasPropertyChanges(sourceConcept, targetConcept)) {
			return true;
		}
		
		if (hasDescriptionChanges(sourceConcept.getDescriptions(), targetConcept.getDescriptions())) {
			return true;
		}
		
		if (hasNonInferredChanges(sourceConcept.getRelationships(), targetConcept.getRelationships())) {
			return true;
		}
		
		return false;
	}
	
	private boolean hasPropertyChanges(Object sourceBean, Object targetBean) {
		BeanMap sourceMap = new BeanMap(sourceBean);
		BeanMap targetMap = new BeanMap(targetBean);
		
	    for (Object key : sourceMap.keySet()) {
	        
	    	Object sourceValue = sourceMap.get(key);
	    	Object targetValue = targetMap.get(key);
	        
	    	// Ignore multi-valued properties
	    	if (sourceValue instanceof Iterable) {
	    		continue;
	    	}
	    	
		    if (!Objects.equals(sourceValue, targetValue)) {
		    	return true;
		    }
	    }
	    
	    return false;
	}

	private boolean hasDescriptionChanges(List<ISnomedBrowserDescription> sourceDescriptions, List<ISnomedBrowserDescription> targetDescriptions) {

		if (sourceDescriptions.size() != targetDescriptions.size()) {
			return true;
		}
			
		Map<String, ISnomedBrowserDescription> sourceMap = Maps.uniqueIndex(sourceDescriptions, new Function<ISnomedBrowserDescription, String>() {
			@Override public String apply(ISnomedBrowserDescription input) { return input.getId(); }
		});
		
		Map<String, ISnomedBrowserDescription> targetMap = Maps.uniqueIndex(targetDescriptions, new Function<ISnomedBrowserDescription, String>() {
			@Override public String apply(ISnomedBrowserDescription input) { return input.getId(); }
		});
		
		for (String descriptionId : sourceMap.keySet()) {
			
			if (!targetMap.containsKey(descriptionId)) {
				return true;
			}
			
			ISnomedBrowserDescription sourceDescription = sourceMap.get(descriptionId);
			ISnomedBrowserDescription targetDescription = targetMap.get(descriptionId);
			
			if (hasPropertyChanges(sourceDescription, targetDescription)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean hasNonInferredChanges(List<ISnomedBrowserRelationship> sourceRelationships, List<ISnomedBrowserRelationship> targetRelationships) {
		
		Map<String, ISnomedBrowserRelationship> sourceMap = FluentIterable.from(sourceRelationships)
				.filter(new Predicate<ISnomedBrowserRelationship>() {
					@Override public boolean apply(ISnomedBrowserRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
				})
				.uniqueIndex(new Function<ISnomedBrowserRelationship, String>() {
					@Override public String apply(ISnomedBrowserRelationship input) { return input.getId(); }
				});
		
		Map<String, ISnomedBrowserRelationship> targetMap = FluentIterable.from(targetRelationships)
				.filter(new Predicate<ISnomedBrowserRelationship>() {
					@Override public boolean apply(ISnomedBrowserRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
				})
				.uniqueIndex(new Function<ISnomedBrowserRelationship, String>() {
					@Override public String apply(ISnomedBrowserRelationship input) { return input.getId(); }
				});

		// XXX: Need to process the relationships first so that filtered sizes can be compared
		if (sourceMap.size() != targetMap.size()) {
			return true;
		}
		
		for (String relationshipId : sourceMap.keySet()) {
			
			if (!targetMap.containsKey(relationshipId)) {
				return true;
			}
			
			ISnomedBrowserRelationship sourceRelationship = sourceMap.get(relationshipId);
			ISnomedBrowserRelationship targetRelationship = targetMap.get(relationshipId);
			
			if (hasPropertyChanges(sourceRelationship, targetRelationship)) {
				return true;
			}
		}
		
		return false;
	}

	private SnomedBrowserConcept mergeConcepts(
			ISnomedBrowserConcept sourceConcept,
			ISnomedBrowserConcept targetConcept, 
			List<ExtendedLocale> locales) {
		
		SnomedBrowserConcept mergedConcept = new SnomedBrowserConcept();
		
		/* 
		 * Selecting merge winners using non-null vs. null effective times:
		 * 
		 *                  (target)
		 * (source) | non-null |   null   |
		 * ---------+----------+----------+
		 * non-null | (source) |  target  |
		 *   null   |  source  | (source) |
		 *   
		 * Values in the main diagonal are "don't care" items, they are set to use the source concept for getting the most
		 * concise expression for testing.
		 */
		final ISnomedBrowserConcept winner;
		if (sourceConcept.getEffectiveTime() != null && targetConcept.getEffectiveTime() == null) {
			winner = targetConcept;
		} else {
			winner = sourceConcept;
		}
		
		mergedConcept.setConceptId(winner.getConceptId());
		mergedConcept.setActive(winner.isActive());
		mergedConcept.setDefinitionStatus(winner.getDefinitionStatus());
		mergedConcept.setEffectiveTime(winner.getEffectiveTime());
		mergedConcept.setModuleId(winner.getModuleId());
		mergedConcept.setIsLeafInferred(winner.getIsLeafInferred());
		mergedConcept.setIsLeafStated(winner.getIsLeafStated());
		
		// Merge descriptions - take all the descriptions from source, and add in from target if they're unpublished
		Map<String, ISnomedBrowserDescription> mergedDescriptions = Maps.newHashMap(Maps.uniqueIndex(sourceConcept.getDescriptions(), new Function<ISnomedBrowserDescription, String>() {
			@Override public String apply(ISnomedBrowserDescription input) { return input.getId(); }
		}));
		
		for (ISnomedBrowserDescription targetDescription : targetConcept.getDescriptions()) {
			if (targetDescription.getEffectiveTime() == null) {
				mergedDescriptions.put(targetDescription.getId(), targetDescription);
			}
		}
		
		mergedConcept.setDescriptions(Lists.newArrayList(mergedDescriptions.values()));
		
		// Merge relationships as well
		Map<String, ISnomedBrowserRelationship> mergedRelationships = Maps.newHashMap(Maps.uniqueIndex(sourceConcept.getRelationships(), new Function<ISnomedBrowserRelationship, String>() {
			@Override public String apply(ISnomedBrowserRelationship input) { return input.getId(); }
		}));
		
		for (ISnomedBrowserRelationship targetRelationship : targetConcept.getRelationships()) {
			if (targetRelationship.getEffectiveTime() == null) {
				mergedRelationships.put(targetRelationship.getId(), targetRelationship);
			}
		}
		
		mergedConcept.setRelationships(Lists.newArrayList(mergedRelationships.values()));
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
