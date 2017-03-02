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
package com.b2international.snowowl.snomed.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.apache.commons.collections.BeanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.exceptions.InvalidStateException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.review.ConceptChanges;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.datastore.review.ReviewManager;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.eventbus.IHandler;
import com.b2international.snowowl.eventbus.IMessage;
import com.b2international.snowowl.snomed.api.ISnomedMergeReviewService;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.mergereview.ISnomedBrowserMergeReviewDetail;
import com.b2international.snowowl.snomed.api.impl.domain.SnomedBrowserMergeReviewDetail;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.SnomedComponent;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * The implementation of the manual concept merge review service.
 * <p>
 * The service relies on {@link ReviewManager} to compute two-way reviews (returning a set of new, changed and deleted
 * components for each side of a fork), and uses the intersection set of changed component IDs to check if there are
 * any non-classification related changes. 
 * <p>
 * If the component is eligible for merge review, the full representation is retrieved on each side and an 
 * automatically merged representation is created using the two sides as sources.
 * 
 * @since 4.5
 */
public class SnomedMergeReviewServiceImpl implements ISnomedMergeReviewService {

	protected abstract static class MergeReviewCompletionHandler implements IHandler<IMessage> {

		private final String address;
		private final IEventBus bus;
		
		protected MergeReviewCompletionHandler(final String address, final IEventBus bus) {
			this.address = address;
			this.bus = bus;
		}
		
		public void register() {
			bus.registerHandler(address, this);
		}
		
		@Override
		public void handle(IMessage message) {
			Merge merge = message.body(Merge.class);
			
			try {
				
				if (Merge.Status.COMPLETED.equals(merge.getStatus())) {
					onCompleted(merge);
				}
				
			} finally {
				bus.unregisterHandler(address, this);
			}
		}
			
		protected abstract void onCompleted(Merge merge);
	}
	
	protected static class ConceptUpdateHandler extends MergeReviewCompletionHandler {
		
		private final List<ISnomedBrowserConcept> updates;
		private final String userId;
		private final List<ExtendedLocale> extendedLocales;
		private final ISnomedBrowserService browserService;
		
		private ConceptUpdateHandler(String address, IEventBus bus, 
				List<ISnomedBrowserConcept> updates,
				String userId, 
				List<ExtendedLocale> extendedLocales, 
				ISnomedBrowserService browserService) {
				
			super(address, bus);
			this.updates = updates;
			this.userId = userId;
			this.extendedLocales = extendedLocales;
			this.browserService = browserService;
		}
		
		@Override
		protected void onCompleted(Merge merge) {
			browserService.update(merge.getTarget(), updates, userId, extendedLocales);
		}
	}
	
	protected static class ManualMergeDeleteHandler extends MergeReviewCompletionHandler {
		
		private final SnomedManualConceptMergeServiceImpl manualMergeService;
		private final String mergeReviewId;
		
		private ManualMergeDeleteHandler(String address, IEventBus bus, SnomedManualConceptMergeServiceImpl manualMergeService, String mergeReviewId) {
			super(address, bus);
			this.manualMergeService = manualMergeService;
			this.mergeReviewId = mergeReviewId;
		}
		
		@Override
		protected void onCompleted(Merge merge) {
			manualMergeService.deleteAll(merge.getTarget(), mergeReviewId);
		}
	}
	
	protected static class MergeReviewDeleteHandler extends MergeReviewCompletionHandler {
		
		private final MergeReview mergeReview;
		
		private MergeReviewDeleteHandler(String address, IEventBus bus, MergeReview mergeReview) {
			super(address, bus);
			this.mergeReview = mergeReview;
		}
		
		@Override
		protected void onCompleted(Merge merge) {
			mergeReview.delete();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(SnomedMergeReviewServiceImpl.class);
	
	/**
	 * Special value indicating that the concept should not be added to the review, because it did not change (ignoring
	 * any changes related to classification).
	 */
	private static final ISnomedBrowserMergeReviewDetail SKIP_DETAIL = new ISnomedBrowserMergeReviewDetail() {
		
		@Override
		public ISnomedBrowserConcept getTargetConcept() {
			throw new UnsupportedOperationException("getTargetConcept should not be called on empty merge review element.");
		}
		
		@Override
		public ISnomedBrowserConcept getSourceConcept() {
			throw new UnsupportedOperationException("getSourceConcept should not be called on empty merge review element.");
		}
		
		@Override
		public ISnomedBrowserConcept getManuallyMergedConcept() {
			throw new UnsupportedOperationException("getManuallyMergedConcept should not be called on empty merge review element.");
		}
		
		@Override
		public ISnomedBrowserConcept getAutoMergedConcept() {
			throw new UnsupportedOperationException("getAutoMergedConcept should not be called on empty merge review element.");
		}
	};

	/**
	 * Special value indicating that the concept ID should not be added to the intersection set, because it did not change (ignoring
	 * any changes related to classification).
	 */
	private static final String SKIP_ID = "";
	
	private static class MergeReviewParameters {
		private final String sourcePath;
		private final String targetPath;
		private final List<ExtendedLocale> extendedLocales;
		private final String mergeReviewId;
		
		private MergeReviewParameters(final String sourcePath, final String targetPath, final List<ExtendedLocale> extendedLocales, final String mergeReviewId) {
			this.sourcePath = sourcePath;
			this.targetPath = targetPath;
			this.extendedLocales = extendedLocales;
			this.mergeReviewId = mergeReviewId;
		}

		public String getSourcePath() {
			return sourcePath;
		}

		public String getTargetPath() {
			return targetPath;
		}

		public List<ExtendedLocale> getExtendedLocales() {
			return extendedLocales;
		}

		public String getMergeReviewId() {
			return mergeReviewId;
		}
	}
	
	private abstract class MergeReviewCallable<T> implements Callable<T> {
		protected final String conceptId;
		protected final MergeReviewParameters parameters;

		private MergeReviewCallable(final String conceptId, final MergeReviewParameters parameters) {
			this.conceptId = conceptId;
			this.parameters = parameters;
		}
		
		@Override
		public T call() throws Exception {

			final SnomedConcept sourceConcept = getConcept(parameters.getSourcePath(), conceptId);
			final SnomedConcept targetConcept = getConcept(parameters.getTargetPath(), conceptId);
			
			if (hasConceptChanges(sourceConcept, targetConcept)) {
				return onSuccess();
			}
			
			final SnomedDescriptions sourceDescriptions = getDescriptions(parameters.getSourcePath(), conceptId);
			final SnomedDescriptions targetDescriptions = getDescriptions(parameters.getTargetPath(), conceptId);
			
			if (hasDescriptionChanges(sourceDescriptions.getItems(), targetDescriptions.getItems())) {
				return onSuccess();
			}
			
			final SnomedRelationships sourceRelationships = getUnpublishedRelationships(parameters.getSourcePath(), conceptId);
			final SnomedRelationships targetRelationships = getUnpublishedRelationships(parameters.getTargetPath(), conceptId);
			
			if (hasNonInferredRelationshipChanges(sourceRelationships.getItems(), targetRelationships.getItems())) {
				return onSuccess();
			}
			
			return onSkip();
		}

		private SnomedConcept getConcept(final String path, final String conceptId) {
			return SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, path)
				.execute(bus)
				.getSync();
		}
		
		private boolean hasConceptChanges(final SnomedConcept sourceConcept, final SnomedConcept targetConcept) {
			return hasSinglePropertyChanges(sourceConcept, targetConcept, "iconId", "score");
		}
		
		private SnomedDescriptions getDescriptions(final String path, final String conceptId) {
			return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByConcept(conceptId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, path)
				.execute(bus)
				.getSync();
		}
		
		private boolean hasDescriptionChanges(final List<SnomedDescription> sourceDescriptions, final List<SnomedDescription> targetDescriptions) {

			if (sourceDescriptions.size() != targetDescriptions.size()) {
				return true;
			}
				
			final Map<String, SnomedDescription> sourceMap = Maps.uniqueIndex(sourceDescriptions, new Function<SnomedDescription, String>() {
				@Override public String apply(final SnomedDescription input) { return input.getId(); }
			});
			
			final Map<String, SnomedDescription> targetMap = Maps.uniqueIndex(targetDescriptions, new Function<SnomedDescription, String>() {
				@Override public String apply(final SnomedDescription input) { return input.getId(); }
			});
			
			for (final String descriptionId : sourceMap.keySet()) {
				
				if (!targetMap.containsKey(descriptionId)) {
					return true;
				}
				
				final SnomedDescription sourceDescription = sourceMap.get(descriptionId);
				final SnomedDescription targetDescription = targetMap.get(descriptionId);
				
				if (hasSinglePropertyChanges(sourceDescription, targetDescription, "iconId", "score")) {
					return true;
				}
			}
			
			return false;
		}

		private SnomedRelationships getUnpublishedRelationships(final String path, final String conceptId) {
			return SnomedRequests.prepareSearchRelationship()
				.all()
				.filterBySource(conceptId)
				.filterByEffectiveTime(EffectiveTimes.UNSET_EFFECTIVE_TIME_LABEL)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, path)
				.execute(bus)
				.getSync();
		}
		
		private boolean hasNonInferredRelationshipChanges(final List<SnomedRelationship> sourceRelationships, final List<SnomedRelationship> targetRelationships) {
			
			final Map<String, SnomedRelationship> sourceMap = FluentIterable.from(sourceRelationships)
					.filter(new Predicate<SnomedRelationship>() {
						@Override public boolean apply(final SnomedRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
					})
					.uniqueIndex(new Function<SnomedRelationship, String>() {
						@Override public String apply(final SnomedRelationship input) { return input.getId(); }
					});
			
			final Map<String, SnomedRelationship> targetMap = FluentIterable.from(targetRelationships)
					.filter(new Predicate<SnomedRelationship>() {
						@Override public boolean apply(final SnomedRelationship input) { return !CharacteristicType.INFERRED_RELATIONSHIP.equals(input.getCharacteristicType()); }
					})
					.uniqueIndex(new Function<SnomedRelationship, String>() {
						@Override public String apply(final SnomedRelationship input) { return input.getId(); }
					});

			// If there are no new or changed stated relationships on one side of the comparison, no merge conflict should be indicated
			if (sourceMap.isEmpty() || targetMap.isEmpty()) {
				return false;
			}

			// XXX: Need to process the relationships first so that filtered sizes can be compared
			if (sourceMap.size() != targetMap.size()) {
				return true;
			}
			
			for (final String relationshipId : sourceMap.keySet()) {
				
				if (!targetMap.containsKey(relationshipId)) {
					return true;
				}
				
				final SnomedRelationship sourceRelationship = sourceMap.get(relationshipId);
				final SnomedRelationship targetRelationship = targetMap.get(relationshipId);
				
				if (hasSinglePropertyChanges(sourceRelationship, targetRelationship, "iconId", "score")) {
					return true;
				}
			}
			
			return false;
		}
		
		@SuppressWarnings("deprecation") // Using deprecated org.apache.commons.collections.BeanMap because of split packages
		private boolean hasSinglePropertyChanges(final Object sourceBean, final Object targetBean, final String... ignoredProperties) {
			final BeanMap sourceMap = new BeanMap(sourceBean);
			final BeanMap targetMap = new BeanMap(targetBean);
			final Set<String> ignoredPropertySet = ImmutableSet.copyOf(ignoredProperties);
			final Set<Object> checkedPropertySet = ImmutableSet.copyOf(Sets.difference(sourceMap.keySet(), ignoredPropertySet));
			
		    for (final Object key : checkedPropertySet) {
		    	
		    	Object sourceValue = sourceMap.get(key);
		    	Object targetValue = targetMap.get(key);
		        
		    	// Skip multi-valued properties
		    	if (sourceValue instanceof Iterable || sourceValue instanceof long[]) {
		    		continue;
		    	}
		    	
		    	if (targetValue instanceof Iterable) {
		    		continue;
		    	}
		    	
		    	// Skip arrays as well
		    	if (sourceValue != null && sourceValue.getClass().isArray()) {
		    		continue;
		    	}
		    	
		    	if (targetValue != null && targetValue.getClass().isArray()) {
		    		continue;
		    	}
		    	
		    	// Compare core components by identifier
		    	if (sourceValue instanceof SnomedComponent) {
		    		sourceValue = ((SnomedComponent) sourceValue).getId();
		    	}
		    	
		    	if (targetValue instanceof SnomedComponent) {
		    		targetValue = ((SnomedComponent) targetValue).getId();
		    	}
		    	
			    if (!Objects.equals(sourceValue, targetValue)) {
			    	return true;
			    }
		    }
		    
		    return false;
		}
		
		protected abstract T onSuccess() throws IOException;
		
		protected abstract T onSkip();
	}
	
	private class ComputeMergeReviewCallable extends MergeReviewCallable<ISnomedBrowserMergeReviewDetail> {

		private ComputeMergeReviewCallable(final String conceptId, final MergeReviewParameters parameters) {
			super(conceptId, parameters);
		}

		@Override
		protected ISnomedBrowserMergeReviewDetail onSuccess() throws IOException {
			final ISnomedBrowserConcept sourceConcept = browserService.getConceptDetails(parameters.getSourcePath(), conceptId, parameters.getExtendedLocales());
			final ISnomedBrowserConcept targetConcept = browserService.getConceptDetails(parameters.getTargetPath(), conceptId, parameters.getExtendedLocales());

			final ISnomedBrowserConcept autoMergedConcept = mergeConcepts(sourceConcept, targetConcept, parameters.getExtendedLocales());
			final ISnomedBrowserConcept manuallyMergedConcept = manualConceptMergeService.exists(parameters.getTargetPath(), parameters.getMergeReviewId(), conceptId) 
					? manualConceptMergeService.retrieve(parameters.getTargetPath(), parameters.getMergeReviewId(), conceptId) 
					: null;

			return new SnomedBrowserMergeReviewDetail(sourceConcept, targetConcept, autoMergedConcept, manuallyMergedConcept);
		}

		private SnomedBrowserConcept mergeConcepts(
				final ISnomedBrowserConcept sourceConcept,
				final ISnomedBrowserConcept targetConcept, 
				final List<ExtendedLocale> locales) {

			final SnomedBrowserConcept mergedConcept = new SnomedBrowserConcept();
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
			
			mergedConcept.setInactivationIndicator(winner.getInactivationIndicator());
			mergedConcept.setAssociationTargets(winner.getAssociationTargets());
			
			// Merge Descriptions - take all the descriptions from source, and add in from target
			// if they're unpublished, which will cause an overwrite in the Set if the Description Id matches
			// TODO UNLESS the source description is also unpublished (Change to use map?)
			final Set<ISnomedBrowserDescription> mergedDescriptions = new HashSet<ISnomedBrowserDescription>(sourceConcept.getDescriptions());
			for (final ISnomedBrowserDescription thisDescription : targetConcept.getDescriptions()) {
				if (thisDescription.getEffectiveTime() == null) {
					mergedDescriptions.add(thisDescription);
				}
			}
			mergedConcept.setDescriptions(new ArrayList<ISnomedBrowserDescription>(mergedDescriptions));
			
			// Merge Relationships  - same process using Set to remove duplicated
			final Set<ISnomedBrowserRelationship> mergedRelationships = new HashSet<ISnomedBrowserRelationship>(sourceConcept.getRelationships());
			for (final ISnomedBrowserRelationship thisRelationship : targetConcept.getRelationships()) {
				if (thisRelationship.getEffectiveTime() == null) {
					mergedRelationships.add(thisRelationship);
				}
			}
			mergedConcept.setRelationships(new ArrayList<ISnomedBrowserRelationship>(mergedRelationships));
			
			return mergedConcept;
		}

		@Override
		protected ISnomedBrowserMergeReviewDetail onSkip() {
			return SKIP_DETAIL;
		}
	}
	
	private class ComputeIntersectionIdsCallable extends MergeReviewCallable<String> {

		private ComputeIntersectionIdsCallable(final String conceptId, final MergeReviewParameters parameters) {
			super(conceptId, parameters);
		}
		
		@Override
		protected String onSuccess() throws IOException {
			return conceptId;
		}
		
		@Override
		protected String onSkip() {
			return SKIP_ID;
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
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		
		try {
			return getConceptDetails(mergeReview, extendedLocales);
		} finally {
			LOG.debug("Merge review details for {} (source: {}, target: {}) computed in {}.", mergeReview.id(), mergeReview.sourcePath(), mergeReview.targetPath(), stopwatch);
		}
	}
	
	private Set<ISnomedBrowserMergeReviewDetail> getConceptDetails(final MergeReview mergeReview, final List<ExtendedLocale> extendedLocales) throws InterruptedException, ExecutionException {
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();
		
		final Set<String> mergeReviewIntersection = getFilteredMergeReviewIntersection(mergeReview);
		
		final List<ListenableFuture<ISnomedBrowserMergeReviewDetail>> changeFutures = Lists.newArrayList();
		final MergeReviewParameters parameters = new MergeReviewParameters(sourcePath, targetPath, extendedLocales, mergeReview.id());
		
		for (final String conceptId : mergeReviewIntersection) {
			changeFutures.add(executorService.submit(new ComputeMergeReviewCallable(conceptId, parameters)));
		}
		
		// Filter out all irrelevant detail objects
		final List<ISnomedBrowserMergeReviewDetail> changes = Futures.allAsList(changeFutures).get();
		final Set<ISnomedBrowserMergeReviewDetail> relevantChanges = Sets.newHashSet(Collections2.filter(changes, new Predicate<ISnomedBrowserMergeReviewDetail>() {
			@Override public boolean apply(final ISnomedBrowserMergeReviewDetail input) { return SKIP_DETAIL != input; }
		}));
		
		LOG.debug("Merge review {} count: {} initial, {} filtered", mergeReview.id(), changes.size(), relevantChanges.size());
		return relevantChanges;
	}

	private Set<String> getFilteredMergeReviewIntersection(final MergeReview mergeReview) {
		
		final Set<String> sourceConceptIds = Sets.newHashSet();
		final Set<String> sourceDescriptionIds = Sets.newHashSet();
		final Set<String> sourceRelationshipIds = Sets.newHashSet();
		
		ConceptChanges sourceChanges = SnomedRequests.review()
				.prepareGetConceptChanges(mergeReview.sourceToTargetReviewId())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID)
				.execute(bus)
				.getSync();
		
		for (String id : sourceChanges.changedConcepts()) {
			ComponentCategory componentCategory = SnomedIdentifiers.getComponentCategory(id);
			if (componentCategory == ComponentCategory.CONCEPT) {
				sourceConceptIds.add(id);
			} else if (componentCategory == ComponentCategory.DESCRIPTION) {
				sourceDescriptionIds.add(id);
			} else if (componentCategory == ComponentCategory.RELATIONSHIP) {
				sourceRelationshipIds.add(id);
			} else {
				LOG.warn("Changed concept set contained invalid component id: {}", id);
			}
		}
		
		if (!sourceDescriptionIds.isEmpty()) {
			
			sourceConceptIds.addAll(SnomedRequests.prepareSearchDescription()
				.setComponentIds(sourceDescriptionIds)
				.setLimit(sourceDescriptionIds.size())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, mergeReview.sourcePath())
				.execute(bus)
				.then(new Function<SnomedDescriptions, Set<String>>() {
					@Override
					public Set<String> apply(SnomedDescriptions input) {
						return FluentIterable.from(input).transform(new Function<SnomedDescription, String>() {
							@Override
							public String apply(SnomedDescription input) {
								return input.getConceptId();
							}
						}).toSet();
					}
				}).getSync());

			
		}
		
		if (!sourceRelationshipIds.isEmpty()) {
			
			sourceConceptIds.addAll(SnomedRequests.prepareSearchRelationship()
				.setComponentIds(sourceRelationshipIds)
				.setLimit(sourceRelationshipIds.size())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, mergeReview.sourcePath())
				.execute(bus)
				.then(new Function<SnomedRelationships, Set<String>>() {
					@Override
					public Set<String> apply(SnomedRelationships input) {
						return FluentIterable.from(input).transform(new Function<SnomedRelationship, String>() {
							@Override
							public String apply(SnomedRelationship input) {
								return input.getSourceId();
							}
						}).toSet();
					}
				}).getSync());
			
		}
		
		sourceConceptIds.removeAll(sourceChanges.deletedConcepts());
		
		ConceptChanges targetChanges = SnomedRequests.review()
				.prepareGetConceptChanges(mergeReview.targetToSourceReviewId())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID)
				.execute(bus)
				.getSync();
		
		final Set<String> targetConceptIds = Sets.newHashSet();
		final Set<String> targetDescriptionIds = Sets.newHashSet();
		final Set<String> targetRelationshipIds = Sets.newHashSet();
		
		for (String id : targetChanges.changedConcepts()) {
			ComponentCategory componentCategory = SnomedIdentifiers.getComponentCategory(id);
			if (componentCategory == ComponentCategory.CONCEPT) {
				targetConceptIds.add(id);
			} else if (componentCategory == ComponentCategory.DESCRIPTION) {
				targetDescriptionIds.add(id);
			} else if (componentCategory == ComponentCategory.RELATIONSHIP) {
				targetRelationshipIds.add(id);
			} else {
				LOG.warn("Changed concept set contained invalid component id: {}", id);
			}
		}
		
		if (!targetDescriptionIds.isEmpty()) {
			
			targetConceptIds.addAll(SnomedRequests.prepareSearchDescription()
				.setComponentIds(targetDescriptionIds)
				.setLimit(targetDescriptionIds.size())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, mergeReview.targetPath())
				.execute(bus)
				.then(new Function<SnomedDescriptions, Set<String>>() {
					@Override
					public Set<String> apply(SnomedDescriptions input) {
						return FluentIterable.from(input).transform(new Function<SnomedDescription, String>() {
							@Override
							public String apply(SnomedDescription input) {
								return input.getConceptId();
							}
						}).toSet();
					}
				}).getSync());
			
			
		}
		
		if (!targetRelationshipIds.isEmpty()) {
			
			targetConceptIds.addAll(SnomedRequests.prepareSearchRelationship()
				.setComponentIds(targetRelationshipIds)
				.setLimit(targetRelationshipIds.size())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, mergeReview.targetPath())
				.execute(bus)
				.then(new Function<SnomedRelationships, Set<String>>() {
					@Override
					public Set<String> apply(SnomedRelationships input) {
						return FluentIterable.from(input).transform(new Function<SnomedRelationship, String>() {
							@Override
							public String apply(SnomedRelationship input) {
								return input.getSourceId();
							}
						}).toSet();
					}
				}).getSync());
		}
		
		targetConceptIds.removeAll(targetChanges.deletedConcepts());
		
		sourceConceptIds.retainAll(targetConceptIds);
		
		return sourceConceptIds;
	}
	
	@Override
	public Merge mergeAndReplayConceptUpdates(final String mergeReviewId, final String userId, final List<ExtendedLocale> extendedLocales) throws IOException, InterruptedException, ExecutionException {
		final MergeReview mergeReview = getMergeReview(mergeReviewId);
		final String sourcePath = mergeReview.sourcePath();
		final String targetPath = mergeReview.targetPath();

		// Check we have a full set of manually merged concepts 
		final Set<String> mergeReviewIntersection = getFilteredMergeReviewIntersection(mergeReview);
		final List<ListenableFuture<String>> changeFutures = Lists.newArrayList();
		final MergeReviewParameters parameters = new MergeReviewParameters(sourcePath, targetPath, extendedLocales, mergeReview.id());

		for (final String conceptId : mergeReviewIntersection) {
			changeFutures.add(executorService.submit(new ComputeIntersectionIdsCallable(conceptId, parameters)));
		}
		
		final List<String> changes = Futures.allAsList(changeFutures).get();
		final Set<String> relevantIntersection = Sets.newHashSet(Collections2.filter(changes, new Predicate<String>() {
			@Override public boolean apply(final String input) { return SKIP_ID != input; }
		}));

		final List<ISnomedBrowserConcept> conceptUpdates = new ArrayList<ISnomedBrowserConcept>();
		for (final String conceptId : relevantIntersection) {
			if (!manualConceptMergeService.exists(targetPath, mergeReviewId, conceptId)) {
				throw new InvalidStateException("Manually merged concept " + conceptId + " does not exist for merge review " + mergeReviewId);
			} else {
				conceptUpdates.add(manualConceptMergeService.retrieve(targetPath, mergeReviewId, conceptId));
			}
		}

		final UUID mergeId = UUID.randomUUID();
		final String address = String.format(Merge.ADDRESS_TEMPLATE, SnomedDatastoreActivator.REPOSITORY_UUID, mergeId);

		// Set up one-shot handlers that will be notified when the merge completes successfully
		new ConceptUpdateHandler(address, bus, conceptUpdates, userId, extendedLocales, browserService).register();
		new MergeReviewDeleteHandler(address, bus, mergeReview).register();
		new ManualMergeDeleteHandler(address, bus, manualConceptMergeService, mergeReviewId).register();
		
		return SnomedRequests
			.merging()
			.prepareCreate()
			.setId(mergeId)
			.setSource(sourcePath)
			.setTarget(targetPath)
			.setReviewId(mergeReview.sourceToTargetReviewId())
			.setCommitComment("Auto merging branches before applying manually merged concepts. " + sourcePath + " > " + targetPath)
			.build(SnomedDatastoreActivator.REPOSITORY_UUID)
			.execute(bus)
			.getSync();
	}

	@Override
	public void persistManualConceptMerge(final MergeReview mergeReview, final ISnomedBrowserConcept concept) {
		manualConceptMergeService.storeConceptChanges(mergeReview.targetPath(), mergeReview.id(), concept);
	}
	
	private MergeReview getMergeReview(final String mergeReviewId) {
		return SnomedRequests.mergeReview()
				.prepareGet(mergeReviewId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID)
				.execute(bus)
				.getSync();
	}
}
