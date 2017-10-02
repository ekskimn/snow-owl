package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.service.ConceptService;

import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.impl.validation.domain.ValidationSnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.collect.Sets;

public class ValidationConceptService implements ConceptService {

	private final String branchPath;
	private final IEventBus bus;

	public ValidationConceptService(String branchPath, IEventBus bus) {
		this.branchPath = branchPath;
		this.bus = bus;
	}

	@Override
	public boolean isActive(String conceptId) {
		return SnomedRequests.prepareGetConcept(conceptId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus)
				.getSync()
				.isActive();
	}

	@Override
	public Set<Concept> getAllTopLevelHierachies(){
		SnomedConcepts concepts = SnomedRequests.prepareSearchConcept().all().filterByActive(true).filterByParent(Concepts.ROOT_CONCEPT)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus)
				.getSync();
		
		Set<Concept> matches = new HashSet<>();
		for (SnomedConcept iSnomedConcept : concepts) {
			matches.add(new ValidationSnomedConcept(iSnomedConcept));
		}
		
		return matches;
	}
	
	@Override
	public Set<Concept> findAllStatedAncestorsOfConcept(Concept concept){
		
		List<? extends Relationship> statedParents = concept.getRelationships().stream().filter(r -> 
		   r.getCharacteristicTypeId().equals(Concepts.STATED_RELATIONSHIP) && r.getTypeId().equals(Concepts.IS_A)).collect(Collectors.toList());
		
		Set<Concept> matches = new HashSet<>();
		int limit = Integer.MAX_VALUE;
		for(Relationship statedParentRelationship : statedParents) {
			SnomedConcepts concepts = SnomedRequests.prepareGetConcept(statedParentRelationship.getDestinationId()).setExpand("ancestors(limit:"+limit+",direct:false,form:\"stated\")")
					.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
					.execute(bus)
					.then(new Function<SnomedConcept, SnomedConcepts>() {
						public SnomedConcepts apply(SnomedConcept input) {
							SnomedConcepts ancestors = input.getAncestors();
							List<SnomedConcept> items = new ArrayList<>(input.getAncestors().getItems());
							items.add(input);							
							return new SnomedConcepts(items, 0, limit, ancestors.getTotal());
						}
					})
					.getSync();
			
			for (SnomedConcept iSnomedConcept : concepts) {
				matches.add(new ValidationSnomedConcept(iSnomedConcept));
			}
		}
		
		return matches;
	}
	
	@Override
	public Set<Concept> findAllTopLevelHierachiesOfConcept(Concept concept){
		Set<Concept> allTopLevelHierachies = this.getAllTopLevelHierachies();
		Set<Concept> allStatedAncestorsOfConcept = this.findAllStatedAncestorsOfConcept(concept);
		Set<Concept> matchResults = new HashSet<Concept>();
		for (Concept conceptStatedAncestorsOfConcept : allStatedAncestorsOfConcept) {
			for (Concept conceptTopLevelHierachies : allTopLevelHierachies) {
				if (conceptStatedAncestorsOfConcept.getId().equals(conceptTopLevelHierachies.getId())) {
					matchResults.add(conceptStatedAncestorsOfConcept);
				}
			}
			
		}
		return matchResults;
	}
}
