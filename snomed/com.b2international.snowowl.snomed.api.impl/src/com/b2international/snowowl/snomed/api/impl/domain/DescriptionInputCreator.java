package com.b2international.snowowl.snomed.api.impl.domain;

import java.util.Map;

import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserDescription;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionUpdateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

public class DescriptionInputCreator extends AbstractInputCreator implements ComponentInputCreator<SnomedDescriptionCreateRequest, SnomedDescriptionUpdateRequest, SnomedBrowserDescription> {

	@Override
	public SnomedDescriptionCreateRequest createInput(final SnomedBrowserDescription newDescription, final InputFactory inputFactory) {
		return (SnomedDescriptionCreateRequest) SnomedRequests.prepareNewDescription()
				.setModuleId(getModuleOrDefault(newDescription))
				.setLanguageCode(newDescription.getLang())
				.setTypeId(newDescription.getType().getConceptId())
				.setTerm(newDescription.getTerm())
				.setAcceptability(newDescription.getAcceptabilityMap())
				.setCaseSignificance(newDescription.getCaseSignificance())
				.build();
	}

	@Override
	public SnomedDescriptionUpdateRequest createUpdate(final SnomedBrowserDescription existingDescription, final SnomedBrowserDescription updatedDescription) {
		final SnomedDescriptionUpdateRequestBuilder builder = SnomedRequests.prepareUpdateDescription(existingDescription.getDescriptionId());
		boolean anyDifference = false;
		
		if (existingDescription.isActive() != updatedDescription.isActive()) {
			anyDifference = true;
			builder.setActive(updatedDescription.isActive());
		}
		
		if (!existingDescription.getModuleId().equals(updatedDescription.getModuleId())) {
			anyDifference = true;
			builder.setModuleId(updatedDescription.getModuleId());
		}
		
		final Map<String, Acceptability> existingAcceptabilityMap = nullToEmptyMap(existingDescription.getAcceptabilityMap());
		final Map<String, Acceptability> updatedAcceptabilityMap;
		
		if (!updatedDescription.isActive()) {
			// If the description is inactive, make sure the acceptability map is empty to make the language reference set entries inactive
			updatedAcceptabilityMap = ImmutableMap.of();
		} else {
			// Otherwise use the incoming map, handling null input
			updatedAcceptabilityMap = nullToEmptyMap(updatedDescription.getAcceptabilityMap());
		}
		
		if (!existingAcceptabilityMap.equals(updatedAcceptabilityMap)) {
			anyDifference = true;
			builder.setAcceptability(updatedAcceptabilityMap);
		}
		
		if (!existingDescription.getCaseSignificance().equals(updatedDescription.getCaseSignificance())) {
			anyDifference = true;
			builder.setCaseSignificance(updatedDescription.getCaseSignificance());
		}
		
		if (!Objects.equal(existingDescription.getInactivationIndicator(), updatedDescription.getInactivationIndicator())) {
			anyDifference = true;
			builder.setInactivationIndicator(updatedDescription.getInactivationIndicator());
		}
		
		final Multimap<AssociationType, String> existingAssociationTargets = nullToEmptyMultimap(existingDescription.getAssociationTargets());
		final Multimap<AssociationType, String> updatedAssociationTargets = nullToEmptyMultimap(updatedDescription.getAssociationTargets());
		
		if (!existingAssociationTargets.equals(updatedAssociationTargets)) {
			anyDifference = true;
			builder.setAssociationTargets(updatedAssociationTargets);
		}
		
		if (anyDifference) {
			// TODO remove cast, use only Request interfaces with proper types
			return (SnomedDescriptionUpdateRequest) builder.build();
		} else {
			return null;
		}
	}

	@Override
	public boolean canCreateInput(final Class<? extends BaseSnomedComponentCreateRequest> inputType) {
		return SnomedDescriptionCreateRequest.class.isAssignableFrom(inputType);
	}

	@Override
	public boolean canCreateUpdate(final Class<? extends BaseSnomedComponentUpdateRequest> updateType) {
		return SnomedDescriptionUpdateRequest.class.isAssignableFrom(updateType);
	}
}
