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
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.apache.commons.collections.BeanMap;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.exceptions.InvalidStateException;
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
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SnomedMergeReviewServiceImpl implements ISnomedMergeReviewService {

	private abstract class ChangeRunnable implements Runnable {
		private final String sourcePath;
		private final String targetPath;
		private final String conceptId;

		protected ChangeRunnable(final String sourcePath, final String targetPath, final String conceptId) {
			this.targetPath = targetPath;
			this.conceptId = conceptId;
			this.sourcePath = sourcePath;
		}

		@Override
		public void run() {

			final ISnomedConcept sourceConcept = getConcept(sourcePath, conceptId);
			final ISnomedConcept targetConcept = getConcept(targetPath, conceptId);
			
			if (hasPropertyChanges(sourceConcept, targetConcept)) {
				register(conceptId);
				return;
			}
			
			final SnomedDescriptions sourceDescriptions = getDescriptions(sourcePath, conceptId);
			final SnomedDescriptions targetDescriptions = getDescriptions(targetPath, conceptId);
			
			if (hasDescriptionChanges(sourceDescriptions.getItems(), targetDescriptions.getItems())) {
				register(conceptId);
				return;
			}
			
			final SnomedRelationships sourceRelationships = getRelationships(sourcePath, conceptId);
			final SnomedRelationships targetRelationships = getRelationships(targetPath, conceptId);
			
			if (hasNonInferredChanges(sourceRelationships.getItems(), targetRelationships.getItems())) {
				register(conceptId);
				return;
			}
		}

		protected abstract void register(String conceptId);

		private ISnomedConcept getConcept(final String path, final String conceptId) {
			return SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.build(path)
				.executeSync(bus);
		}

		private SnomedDescriptions getDescriptions(final String path, final String conceptId) {
			return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByConceptId(conceptId)
				.build(path)
				.executeSync(bus);
		}

		private SnomedRelationships getRelationships(final String path, final String conceptId) {
			return SnomedRequests.prepareSearchRelationship()
				.all()
				.filterBySource(conceptId)
				.build(path)
				.executeSync(bus);
		}
	}

	@Resource
	private IEventBus bus;
	
	@Resource
	protected ISnomedBrowserService browserService;
	
	@Resource
	private SnomedManualConceptMergeServiceImpl manualConceptMergeService;
	
	private final ListeningExecutorService executorService;
	
	public SnomedMergeReviewServiceImpl() {
		executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));
	}

	@Override
	public Set<ISnomedBrowserMergeReviewDetail> getMergeReviewDetails(final String mergeReviewId, final List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException {
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		return getConceptDetails(mergeReview, extendedLocales);
	}
	
	private Set<ISnomedBrowserMergeReviewDetail> getConceptDetails(final MergeReview mergeReview, final List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException {
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();
		final Set<String> mergeReviewIntersection = mergeReview.mergeReviewIntersection();
		
		final Map<String, ListenableFuture<ISnomedBrowserMergeReviewDetail>> detailFutures = Maps.newConcurrentMap();
		final List<ListenableFuture<?>> changeFutures = Lists.newArrayList();
		
		for (final String conceptId : mergeReviewIntersection) {
			changeFutures.add(executorService.submit(new ChangeRunnable(sourcePath, targetPath, conceptId) {
				@Override
				protected void register(final String conceptId) {
					detailFutures.put(conceptId, executorService.submit(new Callable<ISnomedBrowserMergeReviewDetail>() {
						@Override
						public ISnomedBrowserMergeReviewDetail call() throws Exception {
							ISnomedBrowserConcept sourceConcept = browserService.getConceptDetails(SnomedServiceHelper.createComponentRef(sourcePath, conceptId), extendedLocales);
							ISnomedBrowserConcept targetConcept = browserService.getConceptDetails(SnomedServiceHelper.createComponentRef(targetPath, conceptId), extendedLocales);

							ISnomedBrowserConcept autoMergedConcept = mergeConcepts(sourceConcept, targetConcept, extendedLocales);
							ISnomedBrowserConceptUpdate manuallyMergedConcept = manualConceptMergeService.exists(targetPath, mergeReview.id(), conceptId) 
									? manualConceptMergeService.retrieve(targetPath, mergeReview.id(), conceptId) 
									: null;

							return new SnomedBrowserMergeReviewDetail(sourceConcept, targetConcept, autoMergedConcept, manuallyMergedConcept);
						}
					}));
				}
			}));
		}
		
		// Ensure that all filtering Runnables are completed
		Futures.allAsList(changeFutures).get();
		
		// Collect all relevant detail objects
		return Sets.newHashSet(Futures.allAsList(detailFutures.values()).get());
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

	private boolean hasDescriptionChanges(List<ISnomedDescription> sourceDescriptions, List<ISnomedDescription> targetDescriptions) {

		if (sourceDescriptions.size() != targetDescriptions.size()) {
			return true;
		}
			
		Map<String, ISnomedDescription> sourceMap = Maps.uniqueIndex(sourceDescriptions, new Function<ISnomedDescription, String>() {
			@Override public String apply(ISnomedDescription input) { return input.getId(); }
		});
		
		Map<String, ISnomedDescription> targetMap = Maps.uniqueIndex(targetDescriptions, new Function<ISnomedDescription, String>() {
			@Override public String apply(ISnomedDescription input) { return input.getId(); }
		});
		
		for (String descriptionId : sourceMap.keySet()) {
			
			if (!targetMap.containsKey(descriptionId)) {
				return true;
			}
			
			ISnomedDescription sourceDescription = sourceMap.get(descriptionId);
			ISnomedDescription targetDescription = targetMap.get(descriptionId);
			
			if (hasPropertyChanges(sourceDescription, targetDescription)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean hasNonInferredChanges(List<ISnomedRelationship> sourceRelationships, List<ISnomedRelationship> targetRelationships) {
		
		Map<String, ISnomedRelationship> sourceMap = FluentIterable.from(sourceRelationships)
				.filter(new Predicate<ISnomedRelationship>() {
					@Override public boolean apply(ISnomedRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
				})
				.uniqueIndex(new Function<ISnomedRelationship, String>() {
					@Override public String apply(ISnomedRelationship input) { return input.getId(); }
				});
		
		Map<String, ISnomedRelationship> targetMap = FluentIterable.from(targetRelationships)
				.filter(new Predicate<ISnomedRelationship>() {
					@Override public boolean apply(ISnomedRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
				})
				.uniqueIndex(new Function<ISnomedRelationship, String>() {
					@Override public String apply(ISnomedRelationship input) { return input.getId(); }
				});

		// XXX: Need to process the relationships first so that filtered sizes can be compared
		if (sourceMap.size() != targetMap.size()) {
			return true;
		}
		
		for (String relationshipId : sourceMap.keySet()) {
			
			if (!targetMap.containsKey(relationshipId)) {
				return true;
			}
			
			ISnomedRelationship sourceRelationship = sourceMap.get(relationshipId);
			ISnomedRelationship targetRelationship = targetMap.get(relationshipId);
			
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
	public void mergeAndReplayConceptUpdates(String mergeReviewId, String userId, List<ExtendedLocale> extendedLocales) throws IOException, InterruptedException, ExecutionException {
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();

		// Check we have a full set of manually merged concepts 
		final Set<String> mergeReviewIntersection = mergeReview.mergeReviewIntersection();
		List<ISnomedBrowserConceptUpdate> conceptUpdates = new ArrayList<ISnomedBrowserConceptUpdate>();
		for (String conceptId : mergeReviewIntersection) {
			if (!manualConceptMergeService.exists(targetPath, mergeReviewId, conceptId)) {
				throw new InvalidStateException("Manually merged concept " + conceptId + " does not exist for merge review " + mergeReviewId);
			} else {
				conceptUpdates.add(manualConceptMergeService.retrieve(targetPath, mergeReviewId, conceptId));
			}
		}

		// Auto merge branches
		SnomedRequests
			.branching()
			.prepareMerge()
			.setSource(sourcePath)
			.setTarget(targetPath)
			.setReviewId(mergeReview.sourceToTargetReviewId())
			.setCommitComment("Auto merging branches before applying manually merged concepts. " + sourcePath + " > " + targetPath)
			.build()
			.executeSync(bus);
		
		// Apply manually merged concepts
		browserService.update(targetPath, conceptUpdates, userId, extendedLocales);

		// Clean up
		mergeReview.delete();
		manualConceptMergeService.deleteAll(targetPath, mergeReviewId);
	}

	@Override
	public void persistManualConceptMerge(MergeReview mergeReview, ISnomedBrowserConceptUpdate concept) {
		manualConceptMergeService.storeConceptChanges(mergeReview.targetPath(), mergeReview.id(), concept);
	}
	
	private MergeReview getMergeReview(String mergeReviewId) {
		return SnomedRequests.mergeReview().prepareGet(mergeReviewId).executeSync(bus);
	}
}
