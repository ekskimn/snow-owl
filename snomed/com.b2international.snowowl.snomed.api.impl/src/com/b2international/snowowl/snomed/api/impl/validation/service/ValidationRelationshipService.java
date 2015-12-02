package com.b2international.snowowl.snomed.api.impl.validation.service;

import org.ihtsdo.drools.service.RelationshipService;

import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;

public class ValidationRelationshipService implements RelationshipService {

	private String branchPath;
	private IEventBus bus;

	public ValidationRelationshipService(String branchPath, IEventBus bus) {
		this.branchPath = branchPath;
		this.bus = bus;
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId) {
		return hasActiveInboundStatedRelationship(conceptId, null);
	}

	@Override
	public boolean hasActiveInboundStatedRelationship(String conceptId, String relationshipTypeId) {
		int totalInbound = SnomedRequests
				.prepareRelationshipSearch()
				.filterByDestination(conceptId)
				.filterByActive(true)
				.filterByType(relationshipTypeId)
				.setOffset(0)
				.setLimit(0)
				.build(branchPath)
				.executeSync(bus)
				.getTotal();
		
		int totalInboundInferred = SnomedRequests
				.prepareRelationshipSearch()
				.filterByDestination(conceptId)
				.filterByActive(true)
				.filterByType(relationshipTypeId)
				.filterByCharacteristicType(CharacteristicType.INFERRED_RELATIONSHIP.getConceptId())
				.setOffset(0)
				.setLimit(0)
				.build(branchPath)
				.executeSync(bus)
				.getTotal();
		
		// This covers all types of relationship other than inferred.
		return totalInbound - totalInboundInferred > 0;
	}

}
