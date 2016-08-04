package com.b2international.snowowl.snomed.api.impl.domain;

import java.util.Map;

import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserDescription;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.core.domain.DescriptionInactivationIndicator;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedDescriptionUpdateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
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
		
		/* 
		 * XXX: Snow Owl's description update request treats null values as "no change", if the description remains inactive, 
		 * but the browser has PUT semantics, so a value of null should mean "set to default". We're also trying to 
		 * do a null-safe comparison at the same time, to see if the description update request needs to include a change at all.
		 */
		if (existingDescription.isActive() && !updatedDescription.isActive()) {
			
			// Set the updated description values when inactivating (null is OK here, they will be set to the default)
			anyDifference = true;
			builder.setActive(updatedDescription.isActive());
			builder.setInactivationIndicator(updatedDescription.getInactivationIndicator());
			builder.setAssociationTargets(updatedDescription.getAssociationTargets());
			
			// Remove acceptability values when inactivating
			builder.setAcceptability(ImmutableMap.<String, Acceptability>of());
			
		} else if (!existingDescription.isActive() && updatedDescription.isActive()) {
			
			// Re-activation will clean out these values, don't set anything
			anyDifference = true;
			builder.setActive(updatedDescription.isActive());
			builder.setInactivationIndicator(null);
			builder.setAssociationTargets(null);
			
			// If any acceptability values were specified, apply them
			builder.setAcceptability(nullToEmptyMap(updatedDescription.getAcceptabilityMap()));
			
		} else if (!existingDescription.isActive() && !updatedDescription.isActive()) {
			
			// Convert null values to the corresponding default and apply if changed
			final DescriptionInactivationIndicator existingInactivationIndicator = existingDescription.getInactivationIndicator() != null 
					? existingDescription.getInactivationIndicator() 
					: DescriptionInactivationIndicator.RETIRED;
					
			final DescriptionInactivationIndicator updatedInactivationIndicator = updatedDescription.getInactivationIndicator() != null 
					? updatedDescription.getInactivationIndicator() 
					: DescriptionInactivationIndicator.RETIRED;
			
			if (!existingInactivationIndicator.equals(updatedInactivationIndicator)) {
				anyDifference = true;
				builder.setInactivationIndicator(updatedInactivationIndicator);
			}
			
			final Multimap<AssociationType, String> existingAssociationTargets = nullToEmptyMultimap(existingDescription.getAssociationTargets());
			final Multimap<AssociationType, String> updatedAssociationTargets = nullToEmptyMultimap(updatedDescription.getAssociationTargets());
			
			if (!existingAssociationTargets.equals(updatedAssociationTargets)) {
				anyDifference = true;
				builder.setAssociationTargets(updatedAssociationTargets);
			}
			
			// No acceptability changes are to be expected
			
		} else /* if (existingDescription.isActive() && updatedDescription.isActive()) */ {
			// TODO: Support updating "concept non-current" status?
			
			// Apply acceptability changes, if there is one
			final Map<String, Acceptability> existingAcceptabilityMap = nullToEmptyMap(existingDescription.getAcceptabilityMap());
			final Map<String, Acceptability> updatedAcceptabilityMap = nullToEmptyMap(updatedDescription.getAcceptabilityMap());
			
			if (!existingAcceptabilityMap.equals(updatedAcceptabilityMap)) {
				anyDifference = true;
				builder.setAcceptability(updatedAcceptabilityMap);
			}
		}
		
		if (!existingDescription.getModuleId().equals(updatedDescription.getModuleId())) {
			anyDifference = true;
			builder.setModuleId(updatedDescription.getModuleId());
		}
		
		if (!existingDescription.getCaseSignificance().equals(updatedDescription.getCaseSignificance())) {
			anyDifference = true;
			builder.setCaseSignificance(updatedDescription.getCaseSignificance());
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
