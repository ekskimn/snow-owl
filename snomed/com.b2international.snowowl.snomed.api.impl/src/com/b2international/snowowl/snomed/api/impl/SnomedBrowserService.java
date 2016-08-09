/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.b2international.snowowl.snomed.api.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.ClassUtils;
import com.b2international.commons.collections.Procedure;
import com.b2international.commons.http.ExtendedLocale;
import com.b2international.commons.options.Options;
import com.b2international.commons.options.OptionsBuilder;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.domain.IComponentRef;
import com.b2international.snowowl.core.domain.IStorageRef;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.bulk.BulkRequest;
import com.b2international.snowowl.core.events.bulk.BulkRequestBuilder;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.index.AbstractIndexQueryAdapter;
import com.b2international.snowowl.datastore.request.CommitInfo;
import com.b2international.snowowl.datastore.request.RepositoryCommitRequestBuilder;
import com.b2international.snowowl.datastore.server.domain.InternalComponentRef;
import com.b2international.snowowl.datastore.server.domain.InternalStorageRef;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserBulkChangeRun;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserChildConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConstant;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescriptionResult;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserParentConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserBulkChangeStatus;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;
import com.b2international.snowowl.snomed.api.domain.browser.TaxonomyNode;
import com.b2international.snowowl.snomed.api.impl.domain.InputFactory;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserBulkChangeRun;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserChildConcept;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConstant;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserDescriptionResult;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserDescriptionResultDetails;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserParentConcept;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationshipTarget;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationshipType;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.ConceptEnum;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.core.tree.Trees;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;
import com.b2international.snowowl.snomed.datastore.index.SnomedRelationshipIndexQueryAdapter;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptGetRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptSearchRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionSearchRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRelationshipCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRelationshipUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SnomedBrowserService implements ISnomedBrowserService {

	private static final class ChildLeafQueryAdapter extends SnomedRelationshipIndexQueryAdapter {
		
		private static final long serialVersionUID = 1L;
		
		private final String characteristicTypeId;

		private ChildLeafQueryAdapter(String queryString, String characteristicTypeId) {
			super(queryString, AbstractIndexQueryAdapter.SEARCH_DEFAULT);
			this.characteristicTypeId = characteristicTypeId;
		}

		@Override
		public Query createQuery() {
			return SnomedMappings.newQuery()
					.relationship()
					.active()
					.relationshipCharacteristicType(characteristicTypeId)
					.relationshipDestination(Long.valueOf(searchString))
					.relationshipType(Concepts.IS_A)
					.matchAll();
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedBrowserService.class);
	private static final Set<SnomedBrowserDescriptionType> SUPPORTED_DESCRIPTION_TYPES = ImmutableSet.of(SnomedBrowserDescriptionType.FSN, SnomedBrowserDescriptionType.SYNONYM); 
	private static final List<ConceptEnum> CONCEPT_ENUMS;

	static {
		final ImmutableList.Builder<ConceptEnum> conceptEnumsBuilder = ImmutableList.builder();
		
		conceptEnumsBuilder.add(DefinitionStatus.values());
		conceptEnumsBuilder.add(CharacteristicType.values());
		conceptEnumsBuilder.add(CaseSignificance.values());
		conceptEnumsBuilder.add(SnomedBrowserDescriptionType.values());
		conceptEnumsBuilder.add(RelationshipModifier.values());
		
		CONCEPT_ENUMS = conceptEnumsBuilder.build();
	}

	private final InputFactory inputFactory;
	
	private final Cache<String, SnomedBrowserBulkChangeRun> bulkChangeRuns = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build();
	
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Resource
	private IEventBus bus;

	public SnomedBrowserService() {
		inputFactory = new InputFactory();
	}

	@Override
	public ISnomedBrowserConcept getConceptDetails(final String branch, final String conceptId, final List<ExtendedLocale> locales) {

		final ISnomedConcept concept = SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.setLocales(locales)
				.setExpand("fsn(),pt(),descriptions(expand(inactivationProperties())),relationships(expand(type(expand(fsn())),destination(expand(fsn()))))")
				.build(branch)
				.executeSync(bus);
		
		final ISnomedDescription fullySpecifiedName = concept.getFsn();
		final ISnomedDescription preferredSynonym = concept.getPt();
		final SnomedDescriptions descriptions = concept.getDescriptions();
		final SnomedRelationships relationships = concept.getRelationships();

		final SnomedBrowserConcept result = convertConcept(concept);

		populateInactivationFields(concept, result);
		populateLeafFields(branch, concept.getId(), result);
		populateFsn(fullySpecifiedName, result);
		populatePreferredSynonym(preferredSynonym, result);

		result.setDescriptions(convertDescriptions(descriptions));
		result.setRelationships(convertRelationships(relationships));
		
		return result;
	}

	private SnomedBrowserConcept convertConcept(ISnomedConcept concept) {
		final SnomedBrowserConcept result = new SnomedBrowserConcept();
		
		result.setActive(concept.isActive());
		result.setReleased(concept.isReleased());
		result.setConceptId(concept.getId());
		result.setDefinitionStatus(concept.getDefinitionStatus());
		result.setEffectiveTime(concept.getEffectiveTime());
		result.setModuleId(concept.getModuleId());
		
		return result;
	}

	private void populateInactivationFields(ISnomedConcept concept, SnomedBrowserConcept result) {
		result.setInactivationIndicator(concept.getInactivationIndicator());
		result.setAssociationTargets(concept.getAssociationTargets());
	}

	private void populateLeafFields(String branch, String conceptId, TaxonomyNode result) {
		final IBranchPath branchPath = BranchPathUtils.createPath(branch);
		final ChildLeafQueryAdapter statedAdapter = new ChildLeafQueryAdapter(conceptId, Concepts.STATED_RELATIONSHIP);
		final ChildLeafQueryAdapter inferredAdapter = new ChildLeafQueryAdapter(conceptId, Concepts.INFERRED_RELATIONSHIP);
		
		result.setIsLeafStated(getIndexService().getHitCount(branchPath, statedAdapter) < 1);
		result.setIsLeafInferred(getIndexService().getHitCount(branchPath, inferredAdapter) < 1);
	}

	private void populatePreferredSynonym(final ISnomedDescription preferredSynonym, final SnomedBrowserConcept result) {
		if (preferredSynonym != null) {
			result.setPreferredSynonym(preferredSynonym.getTerm());
		}
	}

	private void populateFsn(final ISnomedDescription fullySpecifiedName, final SnomedBrowserConcept result) {
		if (fullySpecifiedName != null) {
			result.setFsn(fullySpecifiedName.getTerm());
		} else {
			result.setFsn(result.getConceptId());
		}
	}

	private List<ISnomedBrowserDescription> convertDescriptions(final Iterable<ISnomedDescription> descriptions) {
		final ImmutableList.Builder<ISnomedBrowserDescription> convertedDescriptionBuilder = ImmutableList.builder();
	
		for (final ISnomedDescription description : descriptions) {
			final SnomedBrowserDescription convertedDescription = new SnomedBrowserDescription();
	
			final SnomedBrowserDescriptionType descriptionType = SnomedBrowserDescriptionType.getByConceptId(description.getTypeId());
			if (null == descriptionType) {
				LOGGER.warn("Unsupported description type ID {} on description {}, ignoring.", description.getTypeId(), description.getId());
				continue;
			}
			
			convertedDescription.setActive(description.isActive());
			convertedDescription.setReleased(description.isReleased());
			convertedDescription.setCaseSignificance(description.getCaseSignificance());
			convertedDescription.setConceptId(description.getConceptId());
			convertedDescription.setDescriptionId(description.getId());
			convertedDescription.setEffectiveTime(description.getEffectiveTime());
			convertedDescription.setLang(description.getLanguageCode());
			convertedDescription.setModuleId(description.getModuleId());
			convertedDescription.setTerm(description.getTerm());
			convertedDescription.setType(descriptionType);
			convertedDescription.setAcceptabilityMap(description.getAcceptabilityMap());
			
			convertedDescription.setInactivationIndicator(description.getInactivationIndicator());
			convertedDescription.setAssociationTargets(description.getAssociationTargets());
			
			convertedDescriptionBuilder.add(convertedDescription);
		}
	
		return convertedDescriptionBuilder.build();
	}

	private List<ISnomedBrowserRelationship> convertRelationships(final Iterable<ISnomedRelationship> relationships) {

		final LoadingCache<ISnomedConcept, SnomedBrowserRelationshipType> types = CacheBuilder.newBuilder().build(new CacheLoader<ISnomedConcept, SnomedBrowserRelationshipType>() {
			@Override
			public SnomedBrowserRelationshipType load(ISnomedConcept key) throws Exception {
				return convertBrowserRelationshipType(key);
			}
		});
		
		final LoadingCache<ISnomedConcept, SnomedBrowserRelationshipTarget> targets = CacheBuilder.newBuilder().build(new CacheLoader<ISnomedConcept, SnomedBrowserRelationshipTarget>() {
			@Override
			public SnomedBrowserRelationshipTarget load(ISnomedConcept key) throws Exception {
				return convertBrowserRelationshipTarget(key);
			}
		});
		
		final ImmutableList.Builder<ISnomedBrowserRelationship> convertedRelationshipBuilder = ImmutableList.builder();
		
		for (final ISnomedRelationship relationship : relationships) {
			final SnomedBrowserRelationship convertedRelationship = new SnomedBrowserRelationship(relationship.getId());
			
			convertedRelationship.setActive(relationship.isActive());
			convertedRelationship.setCharacteristicType(relationship.getCharacteristicType());
			convertedRelationship.setEffectiveTime(relationship.getEffectiveTime());
			convertedRelationship.setGroupId(relationship.getGroup());
			convertedRelationship.setModifier(relationship.getModifier());
			convertedRelationship.setModuleId(relationship.getModuleId());
			convertedRelationship.setRelationshipId(relationship.getId());
			convertedRelationship.setReleased(relationship.isReleased());
			convertedRelationship.setSourceId(relationship.getSourceId());
			
			convertedRelationship.setTarget(targets.getUnchecked(relationship.getDestinationConcept()));
			convertedRelationship.setType(types.getUnchecked(relationship.getTypeConcept()));
			
			convertedRelationshipBuilder.add(convertedRelationship);
		}
		
		return convertedRelationshipBuilder.build();
	}
	
	/* package */ SnomedBrowserRelationshipType convertBrowserRelationshipType(ISnomedConcept concept) {
		final SnomedBrowserRelationshipType result = new SnomedBrowserRelationshipType();
		
		result.setConceptId(concept.getId());
		
		if (concept.getFsn() != null) {
			result.setFsn(concept.getFsn().getTerm());
		} else {
			result.setFsn(concept.getId());
		}
		
		return result;
	}

	/* package */ SnomedBrowserRelationshipTarget convertBrowserRelationshipTarget(ISnomedConcept concept) {
		final SnomedBrowserRelationshipTarget result = new SnomedBrowserRelationshipTarget();
		
		result.setActive(concept.isActive());
		result.setConceptId(concept.getId());
		result.setDefinitionStatus(concept.getDefinitionStatus());
		result.setEffectiveTime(concept.getEffectiveTime());
		
		if (concept.getFsn() != null) {
			result.setFsn(concept.getFsn().getTerm());
		} else {
			result.setFsn(concept.getId());
		}
		
		result.setModuleId(concept.getModuleId());
		result.setReleased(concept.isReleased());
		
		return result;
	}

	@Override
	public ISnomedBrowserConcept create(String branch, ISnomedBrowserConcept newConcept, String userId, List<ExtendedLocale> locales) {
		final SnomedConceptCreateRequest conceptCreateRequest = inputFactory.createComponentInput(newConcept, SnomedConceptCreateRequest.class);
		final String commitComment = getCommitComment(userId, newConcept, "creating");
		
		final String createdConceptId = SnomedRequests
				.prepareCommit()
				.setCommitComment(commitComment)
				.setBody(conceptCreateRequest)
				.setUserId(userId)
				.setBranch(branch)
				.build()
				.executeSync(bus)
				.getResultAs(String.class);
		
		final List<ISnomedBrowserRelationship> newRelationships = Lists.newArrayList();
		for (ISnomedBrowserRelationship newRelationship : newConcept.getRelationships()) {
			
			// Skip first IS A relationship that has already been added as the concept's parent
			// FIXME: add active status, characteristic type, group checks? 
			if (Concepts.IS_A.equals(newRelationship.getType().getConceptId()) && conceptCreateRequest.getParentId().equals(newRelationship.getTarget().getConceptId())) {
				continue;
			}
				
			((SnomedBrowserRelationship) newRelationship).setSourceId(createdConceptId);
			newRelationships.add(newRelationship);
		}
		
		if (!newRelationships.isEmpty()) {
			LOGGER.info("Persisting {} additional relationships.", newRelationships.size());
			
			final BulkRequestBuilder<TransactionContext> relationshipCreateBulkRequest = BulkRequest.create();
			final List<SnomedRelationshipCreateRequest> relationshipCreateRequests = inputFactory.createComponentInputs(newRelationships, SnomedRelationshipCreateRequest.class);
			for (SnomedRelationshipCreateRequest relationshipCreateRequest : relationshipCreateRequests) {
				relationshipCreateBulkRequest.add(relationshipCreateRequest);
			}
			
			final String additionalCommitComment = getCommitComment(userId, newConcept, "adding additional relationships to");
			
			SnomedRequests
				.prepareCommit()
				.setCommitComment(additionalCommitComment)
				.setBody(relationshipCreateBulkRequest)
				.setUserId(userId)
				.setBranch(branch)
				.build()
				.executeSync(bus);
		}
		
		return getConceptDetails(branch, createdConceptId, locales);
	}

	@Override
	public ISnomedBrowserConcept update(String branch, ISnomedBrowserConcept updatedConcept, String userId, List<ExtendedLocale> locales) {
		final BulkRequestBuilder<TransactionContext> conceptUpdateBulkRequest = BulkRequest.create();
		update(branch, updatedConcept, userId, locales, conceptUpdateBulkRequest);
		final String commitComment = getCommitComment(userId, updatedConcept, "updating");
		
		SnomedRequests
			.prepareCommit()
			.setUserId(userId)
			.setBranch(branch)
			.setCommitComment(commitComment)
			.setBody(conceptUpdateBulkRequest)
			.build()
			.executeSync(bus);
		
		LOGGER.info("Committed changes for concept {}", getFsn(updatedConcept));
		return getConceptDetails(branch, updatedConcept.getConceptId(), locales);
	}
	
	@Override
	public SnomedBrowserBulkChangeRun beginBulkChange(final String branch, final List<? extends ISnomedBrowserConcept> updatedConcepts, final String userId, final List<ExtendedLocale> locales) {
		final SnomedBrowserBulkChangeRun run = new SnomedBrowserBulkChangeRun();
		run.start();

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
				
					final String commitComment = userId + " Bulk update.";
					createBulkCommit(branch, updatedConcepts, userId, locales, commitComment)
						.build()
						.execute(bus)
						.then(new Function<CommitInfo, Void>() {
							@Override public Void apply(CommitInfo input) { return onSuccess(); }
						})
						.fail(new Procedure<Throwable>() {
							@Override protected void doApply(Throwable throwable) { onFailure(throwable, "during commit"); }
						});
					
				} catch(Exception e) {
					onFailure(e, "while building commit");
				}
			}
			
			private Void onSuccess() {
				run.end(SnomedBrowserBulkChangeStatus.COMPLETED);
				LOGGER.info("Committed bulk concept changes on {}", branch);
				return null;
			}

			private void onFailure(final Throwable throwable, final String phase) {
				run.end(SnomedBrowserBulkChangeStatus.FAILED);
				LOGGER.error("Bulk concept changes failed {} on {}", phase, branch, throwable);
			}
		});
		
		bulkChangeRuns.put(run.getId(), run);
		return run;
	}
	
	@Override
	public ISnomedBrowserBulkChangeRun getBulkChange(String bulkChangeId) {
		return bulkChangeRuns.getIfPresent(bulkChangeId);
	}
	
	@Override
	public void update(String branch, List<? extends ISnomedBrowserConcept> updatedConcepts, String userId, List<ExtendedLocale> locales) {
		final String commitComment = userId + " Bulk update.";
		createBulkCommit(branch, updatedConcepts, userId, locales, commitComment)
			.build()
			.executeSync(bus);
		
		LOGGER.info("Committed bulk concept changes on {}", branch);
	}

	private RepositoryCommitRequestBuilder createBulkCommit(String branch, List<? extends ISnomedBrowserConcept> updatedConcepts, String userId, List<ExtendedLocale> locales, final String commitComment) {
		final BulkRequestBuilder<TransactionContext> bulkRequest = BulkRequest.create();
		
		for (ISnomedBrowserConcept concept : updatedConcepts) {
			update(branch, concept, userId, locales, bulkRequest);
		}
		
		final RepositoryCommitRequestBuilder commit = SnomedRequests
			.prepareCommit()
			.setUserId(userId)
			.setBranch(branch)
			.setCommitComment(commitComment)
			.setBody(bulkRequest);

		return commit;
	}

	private void update(String branch, ISnomedBrowserConcept updatedConcept, String userId, List<ExtendedLocale> locales, final BulkRequestBuilder<TransactionContext> bulkRequest) {
		LOGGER.info("Update concept start {}", getFsn(updatedConcept));

		assertHasAnIsARelationship(updatedConcept);
		final List<ISnomedBrowserDescription> updatedDescriptions = updatedConcept.getDescriptions();
		final List<ISnomedBrowserRelationship> updatedRelationships = updatedConcept.getRelationships();

		final ISnomedBrowserConcept existingConcept = getConceptDetails(branch, updatedConcept.getConceptId(), locales);
		final List<ISnomedBrowserDescription> existingDescriptions = existingConcept.getDescriptions();
		final List<ISnomedBrowserRelationship> existingRelationships = existingConcept.getRelationships();

		// Description updates
		final List<SnomedDescriptionCreateRequest> descriptionCreateRequests = inputFactory.createComponentInputs(updatedDescriptions, SnomedDescriptionCreateRequest.class);
		final Map<String, SnomedDescriptionUpdateRequest> descriptionUpdateRequests = inputFactory.createComponentUpdates(existingDescriptions, updatedDescriptions, SnomedDescriptionUpdateRequest.class);
		final Set<String> deletedDescriptionIds = inputFactory.getDeletedComponentIds(existingDescriptions, updatedDescriptions);
		
		LOGGER.info("Got description changes +{} -{} m{}, {}", descriptionCreateRequests.size(), deletedDescriptionIds.size(), descriptionUpdateRequests.size(), getFsn(updatedConcept));

		// Relationship updates
		List<SnomedRelationshipCreateRequest> relationshipCreateRequests = inputFactory.createComponentInputs(updatedRelationships, SnomedRelationshipCreateRequest.class);
		Map<String, SnomedRelationshipUpdateRequest> relationshipUpdateRequests = inputFactory.createComponentUpdates(existingRelationships, updatedRelationships, SnomedRelationshipUpdateRequest.class);
		final Set<String> deletedRelationshipIds = inputFactory.getDeletedComponentIds(existingRelationships, updatedRelationships);
		
		LOGGER.info("Got relationship changes +{} -{} m{}, {}", relationshipCreateRequests.size(), deletedRelationshipIds.size(), relationshipUpdateRequests.size(), getFsn(updatedConcept));

		// Concept update
		final SnomedConceptUpdateRequest conceptUpdateRequest = inputFactory.createComponentUpdate(existingConcept, updatedConcept, SnomedConceptUpdateRequest.class);
		final boolean conceptInactivation = conceptUpdateRequest != null && Boolean.FALSE.equals(conceptUpdateRequest.isActive());
		
		if (conceptUpdateRequest != null && !conceptInactivation) {
			bulkRequest.add(conceptUpdateRequest);
		}
		
		for (final SnomedDescriptionUpdateRequest descriptionUpdateRequest : descriptionUpdateRequests.values()) {
			bulkRequest.add(descriptionUpdateRequest);
		}
		
		for (final SnomedDescriptionCreateRequest descriptionCreateRequest : descriptionCreateRequests) {
			descriptionCreateRequest.setConceptId(updatedConcept.getConceptId());
			bulkRequest.add(descriptionCreateRequest);
		}
		
		for (final String deletedDescriptionId : deletedDescriptionIds) {
			bulkRequest.add(SnomedRequests.prepareDeleteDescription().setComponentId(deletedDescriptionId).build());
		}

		for (final SnomedRelationshipUpdateRequest relationshipUpdateRequest : relationshipUpdateRequests.values()) {
			bulkRequest.add(relationshipUpdateRequest);
		}
		
		for (final SnomedRelationshipCreateRequest relationshipCreateRequest : relationshipCreateRequests) {
			// XXX: Since an existing concept is being updated, sourceId should be set in the creation request correctly
			bulkRequest.add(relationshipCreateRequest);
		}
		
		for (final String deletedRelationshipId : deletedRelationshipIds) {
			bulkRequest.add(SnomedRequests.prepareDeleteRelationship().setComponentId(deletedRelationshipId).build());
		}

		if (conceptInactivation) {
			bulkRequest.add(conceptUpdateRequest);
		}

		// TODO - Add MRCM checks here
	}

	private void assertHasAnIsARelationship(ISnomedBrowserConcept concept) {
		if (!concept.isActive()) {
			return;
		}
		
		List<ISnomedBrowserRelationship> relationships = concept.getRelationships();
		if (relationships == null) {
			throw new BadRequestException("At least one IS A relationship is required.");
		}
		
		for (ISnomedBrowserRelationship relationship : relationships) {
			if (!relationship.isActive()) { continue; }
			if (relationship.getGroupId() != 0) { continue; }
			if (!CharacteristicType.STATED_RELATIONSHIP.equals(relationship.getCharacteristicType())) { continue; }
			if (!Concepts.IS_A.equals(relationship.getType().getConceptId())) { continue; }
			
			// Found an active, ungrouped, stated IS A relationship, good to go
			return;
		}
			
		throw new BadRequestException("At least one IS A relationship is required.");
	}

	private String getCommitComment(String userId, ISnomedBrowserConcept snomedConceptInput, String action) {
		String fsn = getFsn(snomedConceptInput);
		return userId + " " + action + " concept " + fsn;
	}

	private String getFsn(ISnomedBrowserConcept concept) {
		if (concept.getFsn() != null) {
			return concept.getFsn();
		}
		
		List<ISnomedBrowserDescription> descriptions = concept.getDescriptions();
		if (descriptions == null) {
			return concept.getConceptId();
		}
		
		for (ISnomedBrowserDescription description : descriptions) {
			if (!description.isActive()) { continue; }
			if (!Concepts.FULLY_SPECIFIED_NAME.equals(description.getType().getConceptId())) { continue; }

			// Found an active FSN, good to go
			return description.getTerm();	
		}
		
		return concept.getConceptId();
	}

	@Override
	public List<ISnomedBrowserParentConcept> getConceptParents(IComponentRef conceptRef, List<ExtendedLocale> locales) {
		return getConceptParents(conceptRef, locales, SnomedBrowserDescriptionType.FSN);
	}
	
	@Override
	public List<ISnomedBrowserParentConcept> getConceptParents(final IComponentRef conceptRef, final List<ExtendedLocale> locales,
			final SnomedBrowserDescriptionType preferredDescriptionType) {
		
		final InternalComponentRef internalConceptRef = ClassUtils.checkAndCast(conceptRef, InternalComponentRef.class);
		final String path = internalConceptRef.getBranchPath();
		
		if (!SUPPORTED_DESCRIPTION_TYPES.contains(preferredDescriptionType)) {
			throw new IllegalArgumentException("Only FSN or preferred synonym expansion is supported.");
		}
		
		final OptionsBuilder ancestorsBuilder = OptionsBuilder.newBuilder()
				.put("direct", true)
				.put("form", Trees.INFERRED_FORM)
				.put("limit", Integer.MAX_VALUE);

		switch (preferredDescriptionType) {
			case FSN:
				ancestorsBuilder.put("expand", OptionsBuilder.newBuilder().put("fsn", "").build());
				break;
			
			case SYNONYM:
				ancestorsBuilder.put("expand", OptionsBuilder.newBuilder().put("pt", "").build());
				break;
				
			default:
				break;
		}

		final OptionsBuilder expandBuilder = OptionsBuilder.newBuilder()
				.put("ancestors", ancestorsBuilder.build());
		
		final SnomedConceptGetRequestBuilder requestBuilder = SnomedRequests.prepareGetConcept()
				.setComponentId(conceptRef.getComponentId())
				.setLocales(locales)
				.setExpand(expandBuilder.build());

		final Promise<ISnomedConcept> concept = requestBuilder
				.build(path)
				.execute(bus);
		
		final Promise<List<ISnomedBrowserParentConcept>> transformedParentConcepts = concept.then(new Function<ISnomedConcept, List<ISnomedBrowserParentConcept>>() {
			@Override
			public List<ISnomedBrowserParentConcept> apply(ISnomedConcept input) {
				final List<ISnomedBrowserParentConcept> convertedInput = Lists.newArrayList();
				
				for (final ISnomedConcept concept : input.getAncestors()) {

					final SnomedBrowserParentConcept convertedConcept = new SnomedBrowserParentConcept(); 

					convertedConcept.setConceptId(concept.getId());
					convertedConcept.setDefinitionStatus(concept.getDefinitionStatus());
										
					if (concept.getFsn() != null) {
						convertedConcept.setFsn(concept.getFsn().getTerm());
					} else if (SnomedBrowserDescriptionType.FSN.equals(preferredDescriptionType)) {
						convertedConcept.setFsn(concept.getId());
					}
					
					if (concept.getPt() != null) {
						convertedConcept.setPreferredSynonym(concept.getPt().getTerm());
					} else if (SnomedBrowserDescriptionType.SYNONYM.equals(preferredDescriptionType)) {
						convertedConcept.setPreferredSynonym(concept.getId());
					}
					
					convertedInput.add(convertedConcept);
				}
				
				return convertedInput;
			}
		});
		
		return transformedParentConcepts.getSync();
	}
	
	@Override
	public List<ISnomedBrowserChildConcept> getConceptChildren(final IComponentRef conceptRef, final List<ExtendedLocale> locales, final boolean stated) {
		return getConceptChildren(conceptRef, locales, stated, SnomedBrowserDescriptionType.FSN);
	}
	
	@Override
	public List<ISnomedBrowserChildConcept> getConceptChildren(final IComponentRef conceptRef, final List<ExtendedLocale> locales, final boolean stated, 
			final SnomedBrowserDescriptionType preferredDescriptionType) {
		
		final InternalComponentRef internalConceptRef = ClassUtils.checkAndCast(conceptRef, InternalComponentRef.class);
		final String path = internalConceptRef.getBranchPath();
		
		if (!SUPPORTED_DESCRIPTION_TYPES.contains(preferredDescriptionType)) {
			throw new IllegalArgumentException("Only FSN or preferred synonym expansion is supported.");
		}
		
		final OptionsBuilder descendantsBuilder = OptionsBuilder.newBuilder()
				.put("direct", true)
				.put("limit", 0);
		
		if (stated) {
			descendantsBuilder.put("form", Trees.STATED_FORM);
		} else {
			descendantsBuilder.put("form", Trees.INFERRED_FORM);
		}
		
		final OptionsBuilder expandBuilder = OptionsBuilder.newBuilder()
				.put("descendants", descendantsBuilder.build());
		
		switch (preferredDescriptionType) {
			case FSN:
				expandBuilder.put("fsn", ImmutableMap.of());
				break;
			
			case SYNONYM:
				expandBuilder.put("pt", ImmutableMap.of());
				break;
				
			default:
				break;
		}
				
		final SnomedConceptSearchRequestBuilder requestBuilder = SnomedRequests.prepareSearchConcept()
				.all()
				.filterByActive(true)
				.setLocales(locales)
				.setExpand(expandBuilder.build());
				
		if (stated) {
			requestBuilder.filterByStatedParent(conceptRef.getComponentId());
		} else {
			requestBuilder.filterByParent(conceptRef.getComponentId());
		}

		final Promise<SnomedConcepts> childConcepts = requestBuilder
				.build(path)
				.execute(bus);
		
		final Promise<List<ISnomedBrowserChildConcept>> transformedChildConcepts = childConcepts.then(new Function<SnomedConcepts, List<ISnomedBrowserChildConcept>>() {
			@Override
			public List<ISnomedBrowserChildConcept> apply(SnomedConcepts input) {
				final List<ISnomedBrowserChildConcept> convertedInput = Lists.newArrayList();
				
				for (final ISnomedConcept concept : input) {

					final SnomedBrowserChildConcept convertedConcept = new SnomedBrowserChildConcept(); 

					convertedConcept.setConceptId(concept.getId());
					convertedConcept.setActive(concept.isActive());
					convertedConcept.setDefinitionStatus(concept.getDefinitionStatus());
					convertedConcept.setModuleId(concept.getModuleId());
					
					if (concept.getFsn() != null) {
						convertedConcept.setFsn(concept.getFsn().getTerm());
					} else if (SnomedBrowserDescriptionType.FSN.equals(preferredDescriptionType)) {
						convertedConcept.setFsn(concept.getId());
					}
					
					if (concept.getPt() != null) {
						convertedConcept.setPreferredSynonym(concept.getPt().getTerm());
					} else if (SnomedBrowserDescriptionType.SYNONYM.equals(preferredDescriptionType)) {
						convertedConcept.setPreferredSynonym(concept.getId());
					}
					
					// XXX: The returned concept will not know about the leaf property that was *not* requested here
					if (stated) {
						convertedConcept.setIsLeafStated(concept.getDescendants().getTotal() < 1);
					} else {
						convertedConcept.setIsLeafInferred(concept.getDescendants().getTotal() < 1);
					}

					convertedInput.add(convertedConcept);
				}
				
				return convertedInput;
			}
		});
		
		return transformedChildConcepts.getSync();
	}

	private static SnomedTerminologyBrowser getTerminologyBrowser() {
		return ApplicationContext.getServiceForClass(SnomedTerminologyBrowser.class);
	}

	@Override
	public List<ISnomedBrowserDescriptionResult> getDescriptions(final IStorageRef storageRef, final String query, final List<ExtendedLocale> locales, final int offset, final int limit) {
		return getDescriptions(storageRef, query, locales, offset, limit, SnomedBrowserDescriptionType.FSN);
	}
	
	@Override
	public List<ISnomedBrowserDescriptionResult> getDescriptions(final IStorageRef storageRef, final String query, final List<ExtendedLocale> locales, final int offset, final int limit, 
			final SnomedBrowserDescriptionType preferredDescriptionType) {
		
		checkNotNull(storageRef, "Storage reference may not be null.");
		checkNotNull(query, "Query may not be null.");
		checkArgument(query.length() >= 3, "Query must be at least 3 characters long.");

		if (!SUPPORTED_DESCRIPTION_TYPES.contains(preferredDescriptionType)) {
			throw new IllegalArgumentException("Only FSN or preferred synonym expansion is supported.");
		}
		
		final InternalStorageRef internalStorageRef = ClassUtils.checkAndCast(storageRef, InternalStorageRef.class);
		final String path = internalStorageRef.getBranchPath();
		
		final OptionsBuilder conceptExpandBuilder = OptionsBuilder.newBuilder();
		switch (preferredDescriptionType) {
			case FSN:
				conceptExpandBuilder.put("fsn", OptionsBuilder.newBuilder().build());
				break;
			
			case SYNONYM:
				conceptExpandBuilder.put("pt", OptionsBuilder.newBuilder().build());
				break;
				
			default:
				break;
		}
		
		final Options conceptExpandOptions = OptionsBuilder.newBuilder()
				.put("expand", conceptExpandBuilder.build())
				.build();
		
		final Options descriptionExpandOptions = OptionsBuilder.newBuilder()
				.put("concept", conceptExpandOptions)
				.build();
		
		final SnomedDescriptionSearchRequestBuilder requestBuilder = SnomedRequests.prepareSearchDescription()
			.setOffset(offset)
			.setLimit(limit)
			.setLocales(locales)
			.setExpand(descriptionExpandOptions)
			.filterByType(Concepts.FULLY_SPECIFIED_NAME + " UNION " + Concepts.SYNONYM)
			.filterByTerm(query);
		
		final Promise<SnomedDescriptions> descriptions = requestBuilder
				.build(path)
				.execute(bus);
		
		final Promise<List<ISnomedBrowserDescriptionResult>> transformedDescriptions = descriptions.then(new Function<SnomedDescriptions, List<ISnomedBrowserDescriptionResult>>() {
			public List<ISnomedBrowserDescriptionResult> apply(SnomedDescriptions input) {
				final List<ISnomedBrowserDescriptionResult> convertedInput = Lists.newArrayList();
				final Cache<String, SnomedBrowserDescriptionResultDetails> detailCache = CacheBuilder.newBuilder().build();
				
				for (final ISnomedDescription description : input) {
					
					final SnomedBrowserDescriptionResult convertedDescription = new SnomedBrowserDescriptionResult();
					convertedDescription.setActive(description.isActive());
					convertedDescription.setTerm(description.getTerm());
					
					try {
						
						final SnomedBrowserDescriptionResultDetails details = detailCache.get(description.getConceptId(), 
								new Callable<SnomedBrowserDescriptionResultDetails>() {
							
							@Override
							public SnomedBrowserDescriptionResultDetails call() throws Exception {
								final ISnomedConcept concept = description.getConcept();
								final SnomedBrowserDescriptionResultDetails details = new SnomedBrowserDescriptionResultDetails();
								
								if (concept != null) {
									details.setActive(concept.isActive());
									details.setConceptId(description.getConceptId());
									details.setDefinitionStatus(concept.getDefinitionStatus());
									details.setModuleId(concept.getModuleId());
									
									if (concept.getFsn() != null) {
										details.setFsn(concept.getFsn().getTerm());
									} else if (SnomedBrowserDescriptionType.FSN.equals(preferredDescriptionType)) {
										details.setFsn(description.getConceptId());
									}
									
									if (concept.getPt() != null) {
										details.setPreferredSynonym(concept.getPt().getTerm());
									} else if (SnomedBrowserDescriptionType.SYNONYM.equals(preferredDescriptionType)) {
										details.setPreferredSynonym(concept.getId());
									}
									
								} else {
									LOGGER.warn("Concept {} not expanded, properties will not be set.", description.getConceptId());
								}
								
								return details;
							}
						});
						
						convertedDescription.setConcept(details);
						
					} catch (ExecutionException e) {
						LOGGER.error("Exception thrown during computing details for concept {}, properties will not be set.", description.getConceptId(), e);
					}
					
					convertedInput.add(convertedDescription);
				}
				
				return convertedInput;
			}
		});
		
		return transformedDescriptions.getSync();
	}

	@Override
	public Map<String, ISnomedBrowserConstant> getConstants(final String branch, final List<ExtendedLocale> locales) {
		final ImmutableMap.Builder<String, ISnomedBrowserConstant> resultBuilder = ImmutableMap.builder();
		final DescriptionService descriptionService = new DescriptionService(bus, branch);
		
		for (final ConceptEnum conceptEnum : CONCEPT_ENUMS) {
			try {
				final String conceptId = conceptEnum.getConceptId();

				// Check if the corresponding concept exists
				SnomedRequests.prepareGetConcept()
						.setComponentId(conceptId)
						.build(branch)
						.executeSync(bus);
				
				final SnomedBrowserConstant constant = new SnomedBrowserConstant();
				constant.setConceptId(conceptId);
				
				final ISnomedDescription fullySpecifiedName = descriptionService.getFullySpecifiedName(conceptId, locales);
				if (fullySpecifiedName != null) {
					constant.setFsn(fullySpecifiedName.getTerm());
				} else {
					constant.setFsn(conceptId);
				}
				
				resultBuilder.put(conceptEnum.name(), constant);
			} catch (ComponentNotFoundException e) {
				// ignore
			}
		}
		
		return resultBuilder.build();
	}
	
	private static SnomedIndexService getIndexService() {
		return ApplicationContext.getServiceForClass(SnomedIndexService.class);
	}
}
