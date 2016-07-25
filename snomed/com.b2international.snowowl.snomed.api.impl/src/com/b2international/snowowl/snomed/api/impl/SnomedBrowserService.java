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
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.ClassUtils;
import com.b2international.commons.http.ExtendedLocale;
import com.b2international.commons.options.Options;
import com.b2international.commons.options.OptionsBuilder;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.IComponentRef;
import com.b2international.snowowl.core.domain.IStorageRef;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.bulk.BulkRequest;
import com.b2international.snowowl.core.events.bulk.BulkRequestBuilder;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.index.AbstractIndexQueryAdapter;
import com.b2international.snowowl.datastore.server.domain.InternalComponentRef;
import com.b2international.snowowl.datastore.server.domain.InternalStorageRef;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserChildConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdate;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConstant;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescriptionResult;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserParentConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;
import com.b2international.snowowl.snomed.api.domain.browser.TaxonomyNode;
import com.b2international.snowowl.snomed.api.impl.domain.InputFactory;
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
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.core.tree.Trees;
import com.b2international.snowowl.snomed.datastore.SnomedStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;
import com.b2international.snowowl.snomed.datastore.index.SnomedRelationshipIndexQueryAdapter;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptSearchRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedConceptUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionSearchRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRelationshipCreateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRelationshipUpdateRequest;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

	@Resource
	private IEventBus bus;

	public SnomedBrowserService() {
		inputFactory = new InputFactory();
	}

	@Override
	public ISnomedBrowserConcept getConceptDetails(final IComponentRef conceptRef, final List<ExtendedLocale> locales) {

		final InternalComponentRef internalConceptRef = ClassUtils.checkAndCast(conceptRef, InternalComponentRef.class);
		internalConceptRef.checkStorageExists();
		
		final IBranchPath branchPath = internalConceptRef.getBranch().branchPath();
		final String conceptId = conceptRef.getComponentId();
		final SnomedConceptIndexEntry concept = getTerminologyBrowser().getConcept(branchPath, conceptId);
		
		if (null == concept) {
			throw new ComponentNotFoundException(ComponentCategory.CONCEPT, conceptId);
		}
		
		final List<ISnomedDescription> descriptions = ImmutableList.copyOf(SnomedRequests.prepareSearchDescription()
				.all()
				.filterByConceptId(conceptId)
				.build(conceptRef.getBranchPath())
				.executeSync(bus)
				.getItems());

		final DescriptionService descriptionService = new DescriptionService(bus, conceptRef.getBranchPath());
		
		final ISnomedDescription fullySpecifiedName = descriptionService.getFullySpecifiedName(conceptId, locales);
		final ISnomedDescription preferredSynonym = descriptionService.getPreferredTerm(conceptId, locales);
		final List<SnomedRelationshipIndexEntry> relationships = getStatementBrowser().getOutboundStatements(branchPath, concept);

		final SnomedBrowserConcept result = new SnomedBrowserConcept();

		result.setActive(concept.isActive());
		result.setReleased(concept.isReleased());
		result.setConceptId(concept.getId());
		result.setDefinitionStatus(concept.isPrimitive() ? DefinitionStatus.PRIMITIVE : DefinitionStatus.FULLY_DEFINED);
		result.setEffectiveTime(EffectiveTimes.toDate(concept.getEffectiveTimeAsLong()));
		result.setModuleId(concept.getModuleId());
		
		populateLeafFields(branchPath, conceptId, result);
		
		result.setDescriptions(convertDescriptions(newArrayList(descriptions)));
		
		if (fullySpecifiedName != null) {
			result.setFsn(fullySpecifiedName.getTerm());
		} else {
			result.setFsn(conceptId);
		}
		
		if (preferredSynonym != null) {
			result.setPreferredSynonym(preferredSynonym.getTerm());
		}

		result.setRelationships(convertRelationships(relationships, conceptRef, locales));
		
		return result;
	}

	@Override
	public ISnomedBrowserConcept create(String branchPath, ISnomedBrowserConcept concept, String userId, List<ExtendedLocale> locales) {
		//If calling from an Autowired context, bus might not have been set
		if (bus == null) {
			bus = com.b2international.snowowl.core.ApplicationContext.getInstance().getServiceChecked(IEventBus.class);
		}
		
		final SnomedConceptCreateRequest req = inputFactory.createComponentInput(branchPath, concept, SnomedConceptCreateRequest.class);
		String commitComment = getCommitComment(userId, concept, "creating");
		final String createdConceptId = SnomedRequests
				.prepareCommit()
				.setCommitComment(commitComment)
				.setBody(req)
				.setUserId(userId)
				.setBranch(branchPath)
				.build()
				.executeSync(bus)
				.getResultAs(String.class);
		final IComponentRef componentRef = SnomedServiceHelper.createComponentRef(branchPath, createdConceptId);
		
		List<ISnomedBrowserRelationship> additionalRelationships = new ArrayList<ISnomedBrowserRelationship>();
		for (ISnomedBrowserRelationship iSnomedBrowserRelationship : concept.getRelationships()) {
			if (!(req.getParentId().equals(iSnomedBrowserRelationship.getTarget().getConceptId()) && SnomedConstants.Concepts.IS_A.equals(iSnomedBrowserRelationship.getType().getConceptId()))) {
				SnomedBrowserRelationship relationship = (SnomedBrowserRelationship) iSnomedBrowserRelationship;
				relationship.setSourceId(createdConceptId);
				additionalRelationships.add(iSnomedBrowserRelationship);
			}
		}
		if (!additionalRelationships.isEmpty()) {
			LOGGER.info("Persisting {} additional relationships.", additionalRelationships.size());
			final BulkRequestBuilder<TransactionContext> commitReq = BulkRequest.create();
			List<SnomedRelationshipCreateRequest> relationshipInputs = inputFactory.createComponentInputs(branchPath, additionalRelationships, SnomedRelationshipCreateRequest.class);
		
			for (SnomedRelationshipCreateRequest relationshipCreateRequest : relationshipInputs) {
				commitReq.add(relationshipCreateRequest);
			}
			final String additionalCommitComment = getCommitComment(userId, concept, "adding addiontional relationships of");
			SnomedRequests
				.prepareCommit()
				.setCommitComment(additionalCommitComment)
				.setBody(commitReq)
				.setUserId(userId)
				.setBranch(branchPath)
				.build()
				.executeSync(bus);
		}
		return getConceptDetails(componentRef, locales);
	}

	@Override
	public ISnomedBrowserConcept update(String branchPath, ISnomedBrowserConceptUpdate newVersionConcept, String userId, List<ExtendedLocale> locales) {
		final BulkRequestBuilder<TransactionContext> commitReq = BulkRequest.create();
		IComponentRef componentRef = update(branchPath, newVersionConcept, userId, locales, commitReq);
		
		// Commit
		final String commitComment = getCommitComment(userId, newVersionConcept, "updating");
		SnomedRequests
			.prepareCommit()
			.setUserId(userId)
			.setBranch(branchPath)
			.setCommitComment(commitComment)
			.setBody(commitReq)
			.build()
			.executeSync(bus);
		LOGGER.info("Committed changes for concept {}", newVersionConcept.getFsn());
		
		return getConceptDetails(componentRef, locales);
	}
	
	@Override
	public void update(String branchPath, List<? extends ISnomedBrowserConceptUpdate> newVersionConcepts, String userId, List<ExtendedLocale> locales) {
		final BulkRequestBuilder<TransactionContext> commitReq = BulkRequest.create();
		
		for (ISnomedBrowserConceptUpdate newVersionConcept : newVersionConcepts) {
			update(branchPath, newVersionConcept, userId, locales, commitReq);
		}
		
		// Commit
		final String commitComment = userId + " Bulk update.";
		SnomedRequests
			.prepareCommit()
			.setUserId(userId)
			.setBranch(branchPath)
			.setCommitComment(commitComment)
			.setBody(commitReq)
			.build()
			.executeSync(bus);
		
		LOGGER.info("Committed bulk concept changes on {}", branchPath);
	}

	private IComponentRef update(String branchPath, ISnomedBrowserConceptUpdate newVersionConcept, String userId, List<ExtendedLocale> locales, final BulkRequestBuilder<TransactionContext> commitReq) {
		
		LOGGER.info("Update concept start {}", newVersionConcept.getFsn());

		assertHasAnIsARelationship(newVersionConcept);
		final IComponentRef componentRef = SnomedServiceHelper.createComponentRef(branchPath, newVersionConcept.getConceptId());
		final ISnomedBrowserConcept existingVersionConcept = getConceptDetails(componentRef, locales);

		// Concept update
		final SnomedConceptUpdateRequest conceptUpdate = inputFactory.createComponentUpdate(existingVersionConcept, newVersionConcept, SnomedConceptUpdateRequest.class);
		
		// Description updates
		final List<ISnomedBrowserDescription> existingVersionDescriptions = existingVersionConcept.getDescriptions();
		final List<ISnomedBrowserDescription> newVersionDescriptions = newVersionConcept.getDescriptions();
		Set<String> descriptionDeletionIds = inputFactory.getComponentDeletions(existingVersionDescriptions, newVersionDescriptions);
		Map<String, SnomedDescriptionUpdateRequest> descriptionUpdates = inputFactory.createComponentUpdates(existingVersionDescriptions, newVersionDescriptions, SnomedDescriptionUpdateRequest.class);
		List<SnomedDescriptionCreateRequest> descriptionInputs = inputFactory.createComponentInputs(branchPath, newVersionDescriptions, SnomedDescriptionCreateRequest.class);
		LOGGER.info("Got description changes +{} -{} m{}, {}", descriptionInputs.size(), descriptionDeletionIds.size(), descriptionUpdates.size(), newVersionConcept.getFsn());

		// Relationship updates
		final List<ISnomedBrowserRelationship> existingVersionRelationships = existingVersionConcept.getRelationships();
		final List<ISnomedBrowserRelationship> newVersionRelationships = newVersionConcept.getRelationships();
		Set<String> relationshipDeletionIds = inputFactory.getComponentDeletions(existingVersionRelationships, newVersionRelationships);
		Map<String, SnomedRelationshipUpdateRequest> relationshipUpdates = inputFactory.createComponentUpdates(existingVersionRelationships, newVersionRelationships, SnomedRelationshipUpdateRequest.class);
		List<SnomedRelationshipCreateRequest> relationshipInputs = inputFactory.createComponentInputs(branchPath, newVersionRelationships, SnomedRelationshipCreateRequest.class);
		LOGGER.info("Got relationship changes +{} -{} m{}, {}", relationshipInputs.size(), relationshipDeletionIds.size(), relationshipUpdates.size(), newVersionConcept.getFsn());

		
		// In the case of inactivation, other updates seem to go more smoothly if this is done later
		boolean conceptInactivation = conceptUpdate != null && conceptUpdate.isActive() != null && Boolean.FALSE.equals(conceptUpdate.isActive());
		if (conceptUpdate != null && !conceptInactivation) {
			commitReq.add(conceptUpdate);
		}
		
		for (String descriptionId : descriptionUpdates.keySet()) {
			commitReq.add(descriptionUpdates.get(descriptionId));
		}
		for (SnomedDescriptionCreateRequest descriptionReq : descriptionInputs) {
			descriptionReq.setConceptId(existingVersionConcept.getConceptId());
			commitReq.add(descriptionReq);
		}
		for (String descriptionDeletionId : descriptionDeletionIds) {
			commitReq.add(SnomedRequests.prepareDeleteDescription().setComponentId(descriptionDeletionId).build());
		}

		for (String relationshipId : relationshipUpdates.keySet()) {
			commitReq.add(relationshipUpdates.get(relationshipId));
		}
		for (SnomedRelationshipCreateRequest relationshipReq : relationshipInputs) {
			commitReq.add(relationshipReq);
		}
		for (String relationshipDeletionId : relationshipDeletionIds) {
			commitReq.add(SnomedRequests.prepareDeleteRelationship().setComponentId(relationshipDeletionId).build());
		}

		// Inactivate concept last
		if (conceptUpdate != null && conceptInactivation) {
			commitReq.add(conceptUpdate);
		}

		// TODO - Add MRCM checks here
		
		return componentRef;
	}
	
	private void assertHasAnIsARelationship(ISnomedBrowserConceptUpdate concept) {
		if (concept.isActive()) {
			List<ISnomedBrowserRelationship> relationships = concept.getRelationships();
			if (relationships != null) {
				for (ISnomedBrowserRelationship iSnomedBrowserRelationship : relationships) {
					if (iSnomedBrowserRelationship.isActive()
							&& iSnomedBrowserRelationship.getCharacteristicType() == CharacteristicType.STATED_RELATIONSHIP
							&& Concepts.IS_A.equals(iSnomedBrowserRelationship.getType().getConceptId())
							&& iSnomedBrowserRelationship.getGroupId() == 0) {
						return;
					}
				}
			}
			throw new BadRequestException("At least one is-A relationships is required.");
		}
	}

	private String getCommitComment(String userId, ISnomedBrowserConcept snomedConceptInput, String action) {
		String fsn = getFsn(snomedConceptInput);
		return userId + " " + action + " concept " + fsn;
	}

	private String getFsn(ISnomedBrowserConcept snomedConceptInput) {
		for (ISnomedBrowserDescription descriptionInput : snomedConceptInput.getDescriptions()) {
			if (Concepts.FULLY_SPECIFIED_NAME.equals(descriptionInput.getType().getConceptId())) {
				return descriptionInput.getTerm();
			}
		}
		return null;
	}

	private List<ISnomedBrowserDescription> convertDescriptions(final List<ISnomedDescription> descriptions) {
		final ImmutableList.Builder<ISnomedBrowserDescription> convertedDescriptionBuilder = ImmutableList.builder();

		for (final ISnomedDescription description : descriptions) {
			final SnomedBrowserDescription convertedDescription = new SnomedBrowserDescription();

			final SnomedBrowserDescriptionType descriptionType = convertDescriptionType(description.getTypeId());
			final String descriptionId = description.getId();
			if (null == descriptionType) {
				LOGGER.warn("Unsupported description type ID {} on description {}, ignoring.", description.getTypeId(), descriptionId);
				continue;
			}
			convertedDescription.setActive(description.isActive());
			convertedDescription.setReleased(description.isReleased());
			convertedDescription.setCaseSignificance(description.getCaseSignificance());
			convertedDescription.setConceptId(description.getConceptId());
			convertedDescription.setDescriptionId(descriptionId);
			convertedDescription.setEffectiveTime(description.getEffectiveTime());
			convertedDescription.setLang(description.getLanguageCode());
			convertedDescription.setModuleId(description.getModuleId());
			convertedDescription.setTerm(description.getTerm());
			convertedDescription.setType(descriptionType);
			convertedDescription.setAcceptabilityMap(description.getAcceptabilityMap());
			convertedDescriptionBuilder.add(convertedDescription);
		}

		return convertedDescriptionBuilder.build();
	}

	private SnomedBrowserDescriptionType convertDescriptionType(final String typeId) {
		return SnomedBrowserDescriptionType.getByConceptId(typeId);
	}

	private List<ISnomedBrowserRelationship> convertRelationships(final List<SnomedRelationshipIndexEntry> relationships, final IComponentRef sourceConceptRef, final List<ExtendedLocale> locales) {
		final InternalComponentRef internalConceptRef = ClassUtils.checkAndCast(sourceConceptRef, InternalComponentRef.class);
		final IBranchPath branchPath = internalConceptRef.getBranch().branchPath();
		final DescriptionService descriptionService = new DescriptionService(bus, sourceConceptRef.getBranchPath());

		final ImmutableMap.Builder<String, ISnomedBrowserRelationship> convertedRelationshipBuilder = ImmutableMap.builder();
		for (final SnomedRelationshipIndexEntry relationship : relationships) {
			final SnomedBrowserRelationship convertedRelationship = new SnomedBrowserRelationship(relationship.getId());
			convertedRelationship.setActive(relationship.isActive());
			convertedRelationship.setReleased(relationship.isReleased());
			convertedRelationship.setCharacteristicType(CharacteristicType.getByConceptId(relationship.getCharacteristicTypeId()));
			convertedRelationship.setEffectiveTime(EffectiveTimes.toDate(relationship.getEffectiveTimeAsLong()));
			convertedRelationship.setGroupId(relationship.getGroup());
			convertedRelationship.setModifier(relationship.isUniversal() ? RelationshipModifier.UNIVERSAL : RelationshipModifier.EXISTENTIAL);
			convertedRelationship.setModuleId(relationship.getModuleId());
			convertedRelationship.setSourceId(relationship.getObjectId());
			convertedRelationshipBuilder.put(relationship.getId(), convertedRelationship);
		}
		
		final Map<String, ISnomedBrowserRelationship> convertedRelationships = convertedRelationshipBuilder.build();
		
		final List<SnomedBrowserRelationshipType> types = new FsnJoinerOperation<SnomedBrowserRelationshipType>(sourceConceptRef.getComponentId(), 
				locales, 
				descriptionService) {
			
			@Override
			protected Collection<SnomedConceptIndexEntry> getConceptEntries(String conceptId) {
				final Set<String> typeIds = newHashSet();
				for (final SnomedRelationshipIndexEntry relationship : relationships) {
					typeIds.add(relationship.getAttributeId());
				}
				return getTerminologyBrowser().getConcepts(branchPath, typeIds);
			}

			@Override
			protected SnomedBrowserRelationshipType convertConceptEntry(SnomedConceptIndexEntry conceptEntry, Optional<String> optionalFsn) {
				final SnomedBrowserRelationshipType type = new SnomedBrowserRelationshipType();
				type.setConceptId(conceptEntry.getId());
				type.setFsn(optionalFsn.or(conceptEntry.getId()));
				return type;
			}
		}.run();
		
		final Map<String, SnomedBrowserRelationshipType> typesById = Maps.uniqueIndex(types, new Function<SnomedBrowserRelationshipType, String>() {
			@Override
			public String apply(SnomedBrowserRelationshipType input) {
				return input.getConceptId();
			}
		});
		
		final List<SnomedBrowserRelationshipTarget> targets = new FsnJoinerOperation<SnomedBrowserRelationshipTarget>(sourceConceptRef.getComponentId(), 
				locales, 
				descriptionService) {
			
			@Override
			protected Collection<SnomedConceptIndexEntry> getConceptEntries(String conceptId) {
				final Set<String> destinationConceptIds = newHashSet();
				for (final SnomedRelationshipIndexEntry relationship : relationships) {
					destinationConceptIds.add(relationship.getValueId());
				}
				return getTerminologyBrowser().getConcepts(branchPath, destinationConceptIds);
			}

			@Override
			protected SnomedBrowserRelationshipTarget convertConceptEntry(SnomedConceptIndexEntry destinationConcept, Optional<String> optionalFsn) {
				final SnomedBrowserRelationshipTarget target = new SnomedBrowserRelationshipTarget();
				target.setActive(destinationConcept.isActive());
				target.setConceptId(destinationConcept.getId());
				target.setDefinitionStatus(destinationConcept.isPrimitive() ? DefinitionStatus.PRIMITIVE : DefinitionStatus.FULLY_DEFINED);
				target.setEffectiveTime(EffectiveTimes.toDate(destinationConcept.getEffectiveTimeAsLong()));
				target.setModuleId(destinationConcept.getModuleId());
				target.setFsn(optionalFsn.or(destinationConcept.getId()));
				return target;
			}
		}.run();
		
		final Map<String, SnomedBrowserRelationshipTarget> targetsById = Maps.uniqueIndex(targets, new Function<SnomedBrowserRelationshipTarget, String>() {
			@Override
			public String apply(SnomedBrowserRelationshipTarget input) {
				return input.getConceptId();
			}
		});
		
		for (SnomedRelationshipIndexEntry entry : relationships) {
			SnomedBrowserRelationship rel = (SnomedBrowserRelationship) convertedRelationships.get(entry.getId());
			SnomedBrowserRelationshipType type = typesById.get(entry.getAttributeId());
			SnomedBrowserRelationshipTarget target = targetsById.get(entry.getValueId());
			rel.setType(type);
			rel.setTarget(target);
		}
		
		return ImmutableList.copyOf(convertedRelationships.values());
	}

	protected SnomedBrowserRelationshipTarget getSnomedBrowserRelationshipTarget(SnomedConceptIndexEntry destinationConcept, String branch, List<ExtendedLocale> locales) {
		final DescriptionService descriptionService = new DescriptionService(bus, branch);
		final SnomedBrowserRelationshipTarget target = new SnomedBrowserRelationshipTarget();

		target.setActive(destinationConcept.isActive());
		target.setConceptId(destinationConcept.getId());
		target.setDefinitionStatus(destinationConcept.isPrimitive() ? DefinitionStatus.PRIMITIVE : DefinitionStatus.FULLY_DEFINED);
		target.setEffectiveTime(EffectiveTimes.toDate(destinationConcept.getEffectiveTimeAsLong()));
		target.setModuleId(destinationConcept.getModuleId());

		ISnomedDescription fullySpecifiedName = descriptionService.getFullySpecifiedName(destinationConcept.getId(), locales);
		if (fullySpecifiedName != null) {
			target.setFsn(fullySpecifiedName.getTerm());
		} else {
			target.setFsn(destinationConcept.getId());
		}
		
		return target;
	}

	@Override
	public List<ISnomedBrowserParentConcept> getConceptParents(IComponentRef conceptRef, List<ExtendedLocale> locales) {
		final InternalComponentRef internalConceptRef = ClassUtils.checkAndCast(conceptRef, InternalComponentRef.class);
		final IBranchPath branchPath = internalConceptRef.getBranch().branchPath();
		final DescriptionService descriptionService = new DescriptionService(bus, conceptRef.getBranchPath());

		return new FsnJoinerOperation<ISnomedBrowserParentConcept>(conceptRef.getComponentId(), locales, descriptionService) {
			
			@Override
			protected Collection<SnomedConceptIndexEntry> getConceptEntries(String conceptId) {
				return getTerminologyBrowser().getSuperTypesById(branchPath, conceptId);
			}

			@Override
			protected ISnomedBrowserParentConcept convertConceptEntry(SnomedConceptIndexEntry conceptEntry, Optional<String> optionalFsn) {
				final String childConceptId = conceptEntry.getId();
				final SnomedBrowserParentConcept convertedConcept = new SnomedBrowserParentConcept(); 

				convertedConcept.setConceptId(childConceptId);
				convertedConcept.setDefinitionStatus(conceptEntry.isPrimitive() ? DefinitionStatus.PRIMITIVE : DefinitionStatus.FULLY_DEFINED);
				convertedConcept.setFsn(optionalFsn.or(childConceptId));

				return convertedConcept;
			}
			
		}.run();
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

	private void populateLeafFields(final IBranchPath branchPath, final String conceptId, final TaxonomyNode node) {
		ChildLeafQueryAdapter queryAdapter = new ChildLeafQueryAdapter(conceptId, Concepts.STATED_RELATIONSHIP);
		node.setIsLeafStated(getIndexService().getHitCount(branchPath, queryAdapter) < 1);

		queryAdapter = new ChildLeafQueryAdapter(conceptId, Concepts.INFERRED_RELATIONSHIP);
		node.setIsLeafInferred(getIndexService().getHitCount(branchPath, queryAdapter) < 1);
	}

	private static SnomedTerminologyBrowser getTerminologyBrowser() {
		return ApplicationContext.getServiceForClass(SnomedTerminologyBrowser.class);
	}

	private static SnomedStatementBrowser getStatementBrowser() {
		return ApplicationContext.getServiceForClass(SnomedStatementBrowser.class);
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
