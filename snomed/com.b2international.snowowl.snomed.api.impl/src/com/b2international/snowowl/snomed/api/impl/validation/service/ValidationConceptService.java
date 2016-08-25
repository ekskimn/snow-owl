package com.b2international.snowowl.snomed.api.impl.validation.service;

import org.ihtsdo.drools.service.ConceptService;

import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;

public class ValidationConceptService implements ConceptService {

	private final String branchPath;
	private final IEventBus bus;

	public ValidationConceptService(String branchPath, IEventBus bus) {
		this.branchPath = branchPath;
		this.bus = bus;
	}

	@Override
	public boolean isActive(String conceptId) {
		return SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.build(branchPath)
				.execute(bus)
				.getSync()
				.isActive();
	}

}
