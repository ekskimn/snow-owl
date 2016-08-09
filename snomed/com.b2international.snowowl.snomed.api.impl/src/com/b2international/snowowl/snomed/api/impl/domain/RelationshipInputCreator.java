package com.b2international.snowowl.snomed.api.impl.domain;

import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationship;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRelationshipCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRelationshipUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRelationshipUpdateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;

public class RelationshipInputCreator extends AbstractInputCreator implements ComponentInputCreator<SnomedRelationshipCreateRequest, SnomedRelationshipUpdateRequest, SnomedBrowserRelationship> {
	
	@Override
	public SnomedRelationshipCreateRequest createInput(SnomedBrowserRelationship newRelationship, InputFactory inputFactory) {
		return (SnomedRelationshipCreateRequest) SnomedRequests.prepareNewRelationship()
				.setActive(newRelationship.isActive())
				.setModuleId(getModuleOrDefault(newRelationship))
				.setTypeId(newRelationship.getType().getConceptId())
				.setCharacteristicType(newRelationship.getCharacteristicType())
				.setSourceId(newRelationship.getSourceId()) // XXX: for a new concept, this value might not be known
				.setDestinationId(newRelationship.getTarget().getConceptId())
				.setGroup(newRelationship.getGroupId())
				.setModifier(newRelationship.getModifier())
				.build();
	}

	@Override
	public SnomedRelationshipUpdateRequest createUpdate(SnomedBrowserRelationship existingRelationship, SnomedBrowserRelationship updatedRelationship) {
		final SnomedRelationshipUpdateRequestBuilder builder = SnomedRequests.prepareUpdateRelationship(existingRelationship.getRelationshipId());
		boolean anyDifference = false;
		
		if (existingRelationship.isActive() != updatedRelationship.isActive()) {
			anyDifference = true;
			builder.setActive(updatedRelationship.isActive());
		}
		
		if (!existingRelationship.getModuleId().equals(updatedRelationship.getModuleId())) {
			anyDifference = true;
			builder.setModuleId(updatedRelationship.getModuleId());
		}
		
		if (existingRelationship.getGroupId() != updatedRelationship.getGroupId()) {
			anyDifference = true;
			builder.setGroup(updatedRelationship.getGroupId());
		}
		
		if (!existingRelationship.getCharacteristicType().equals(updatedRelationship.getCharacteristicType())) {
			anyDifference = true;
			builder.setCharacteristicType(updatedRelationship.getCharacteristicType());
		}
		
		if (!existingRelationship.getModifier().equals(updatedRelationship.getModifier())) {
			anyDifference = true;
			builder.setModifier(updatedRelationship.getModifier());
		}
		
		if (anyDifference) {
			// TODO remove cast, use only Request interfaces with proper types
			return (SnomedRelationshipUpdateRequest) builder.build();
		} else {
			return null;
		}
	}

	@Override
	public boolean canCreateInput(Class<? extends BaseSnomedComponentCreateRequest> inputType) {
		return SnomedRelationshipCreateRequest.class.isAssignableFrom(inputType);
	}

	@Override
	public boolean canCreateUpdate(Class<? extends BaseSnomedComponentUpdateRequest> updateType) {
		return SnomedRelationshipUpdateRequest.class.isAssignableFrom(updateType);
	}
}
