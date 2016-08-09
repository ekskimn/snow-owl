package com.b2international.snowowl.snomed.api.impl.domain;

import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationshipType;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.core.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedConceptCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedConceptCreateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedConceptUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedConceptUpdateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.google.common.collect.Multimap;

public class ConceptInputCreator extends AbstractInputCreator implements ComponentInputCreator<SnomedConceptCreateRequest, SnomedConceptUpdateRequest, SnomedBrowserConcept> {
	
	private String getParentId(final ISnomedBrowserConcept concept) {
		ISnomedBrowserRelationship parentRelationship = null;
		
		for (final ISnomedBrowserRelationship relationship : concept.getRelationships()) {
			final ISnomedBrowserRelationshipType type = relationship.getType();
			final String typeId = type.getConceptId();
			
			// FIXME: add active status, characteristic type, group checks? 
			if (Concepts.IS_A.equals(typeId)) {
				parentRelationship = relationship;
			}
		}
		
		if (parentRelationship != null) {
			return parentRelationship.getTarget().getConceptId();
		} else {
			throw new BadRequestException("At least one IS A relationship is required.");
		}
	}

	@Override
	public SnomedConceptCreateRequest createInput(final SnomedBrowserConcept newConcept, final InputFactory inputFactory) {
		final SnomedConceptCreateRequestBuilder builder = SnomedRequests.prepareNewConcept()
				.setModuleId(getModuleOrDefault(newConcept))
				.setDefinitionStatus(newConcept.getDefinitionStatus())
				.setParent(getParentId(newConcept));
		
		final String conceptId = newConcept.getConceptId();
		if (conceptId != null) {
			builder.setId(conceptId);
		}

		for (final ISnomedBrowserDescription newDescription : newConcept.getDescriptions()) {
			builder.addDescription(inputFactory.createComponentInput(newDescription, SnomedDescriptionCreateRequest.class));
		}

		// TODO remove cast, use only Request interfaces with proper type
		return (SnomedConceptCreateRequest) builder.build();
	}

	@Override
	public SnomedConceptUpdateRequest createUpdate(final SnomedBrowserConcept existingConcept, final SnomedBrowserConcept updatedConcept) {
		final SnomedConceptUpdateRequestBuilder builder = SnomedRequests.prepareUpdateConcept(existingConcept.getConceptId());
		boolean anyDifference = false;

		/* 
		 * XXX: Snow Owl's concept update request treats null values as "no change", if the concept remains inactive, 
		 * but the browser has PUT semantics, so a value of null should mean "set to default". We're also trying to 
		 * do a null-safe comparison at the same time, to see if the concept update request needs to include a change at all.
		 */
		if (existingConcept.isActive() && !updatedConcept.isActive()) {
			
			// Set the updated concept's values when inactivating (null is OK here, they will be set to the default)
			anyDifference = true;
			builder.setActive(updatedConcept.isActive());
			builder.setInactivationIndicator(updatedConcept.getInactivationIndicator());
			builder.setAssociationTargets(updatedConcept.getAssociationTargets());
			
		} else if (!existingConcept.isActive() && updatedConcept.isActive()) {
			
			// Re-activation will clean out these values, don't set anything
			anyDifference = true;
			builder.setActive(updatedConcept.isActive());
			builder.setInactivationIndicator(null);
			builder.setAssociationTargets(null);
			
		} else if (!existingConcept.isActive() && !updatedConcept.isActive()) {
			
			// Convert null values to the corresponding default and apply if changed
			final InactivationIndicator existingInactivationIndicator = existingConcept.getInactivationIndicator() != null ? existingConcept.getInactivationIndicator() : InactivationIndicator.RETIRED;
			final InactivationIndicator updatedInactivationIndicator = updatedConcept.getInactivationIndicator() != null ? updatedConcept.getInactivationIndicator() : InactivationIndicator.RETIRED;
			
			if (!existingInactivationIndicator.equals(updatedInactivationIndicator)) {
				anyDifference = true;
				builder.setInactivationIndicator(updatedInactivationIndicator);
			}
			
			final Multimap<AssociationType, String> existingAssociationTargets = nullToEmptyMultimap(existingConcept.getAssociationTargets());
			final Multimap<AssociationType, String> updatedAssociationTargets = nullToEmptyMultimap(updatedConcept.getAssociationTargets());
			
			if (!existingAssociationTargets.equals(updatedAssociationTargets)) {
				anyDifference = true;
				builder.setAssociationTargets(updatedAssociationTargets);
			}
			
		} else /* if (existingConcept.isActive() && updatedConcept.isActive()) */ {
			// Nothing to do when the concept remains active
		}

		if (!existingConcept.getModuleId().equals(updatedConcept.getModuleId())) {
			anyDifference = true;
			builder.setModuleId(updatedConcept.getModuleId());
		}
		
		if (!existingConcept.getDefinitionStatus().equals(updatedConcept.getDefinitionStatus())) {
			anyDifference = true;
			builder.setDefinitionStatus(updatedConcept.getDefinitionStatus());
		}

		if (anyDifference) {
			// TODO remove cast, use only Request interfaces with proper types
			return (SnomedConceptUpdateRequest) builder.build();
		} else {
			return null;
		}
	}

	@Override
	public boolean canCreateInput(final Class<? extends BaseSnomedComponentCreateRequest> inputType) {
		return SnomedConceptCreateRequest.class.isAssignableFrom(inputType);
	}

	@Override
	public boolean canCreateUpdate(final Class<? extends BaseSnomedComponentUpdateRequest> updateType) {
		return SnomedConceptUpdateRequest.class.isAssignableFrom(updateType);
	}
}
