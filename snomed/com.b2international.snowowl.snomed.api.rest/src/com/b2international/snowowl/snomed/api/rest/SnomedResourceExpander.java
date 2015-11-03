package com.b2international.snowowl.snomed.api.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.b2international.snowowl.api.domain.IComponentList;
import com.b2international.snowowl.api.domain.IComponentRef;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.snomed.api.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.api.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.api.domain.classification.IRelationshipChange;
import com.b2international.snowowl.snomed.api.impl.FsnService;
import com.b2international.snowowl.snomed.api.rest.domain.ExpandableRelationshipChange;
import com.b2international.snowowl.snomed.api.rest.domain.ExpandableSnomedConcept;
import com.b2international.snowowl.snomed.api.rest.domain.ExpandableSnomedRelationship;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedConceptMini;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class SnomedResourceExpander {

	@Autowired
	private FsnService fsnService;

	private static final String SOURCE_FSN = "source.fsn";
	private static final String TYPE_FSN = "type.fsn";
	private static final String DESTINATION_FSN = "destination.fsn";

	public static final String FSN = "fsn";

	public List<ISnomedRelationship> expandRelationships(IComponentRef conceptRef, List<ISnomedRelationship> members, 
			final List<Locale> locales, List<String> expantions) {
		
		if (expantions.isEmpty()) {
			return members;			
		}
		final List<ExpandableSnomedRelationship> expandedMembers = new ArrayList<>();
		for (ISnomedRelationship relationship : members) {
			expandedMembers.add(new ExpandableSnomedRelationship(relationship));
		}
		for (String expantion : expantions) {
			
			if (expantion.equals(SOURCE_FSN)) {
				List<String> conceptIds = Lists.transform(expandedMembers, new Function<ExpandableSnomedRelationship, String>() {
					@Override
					public String apply(ExpandableSnomedRelationship input) {
						return input.getSourceId();
					}
				});
				Map<String, String> conceptIdFsnMap = fsnService.getConceptIdFsnMap(conceptRef, conceptIds, locales);
				for (ExpandableSnomedRelationship relationship : expandedMembers) {
					relationship.setSource(createConceptMini(relationship.getSourceId(), conceptIdFsnMap));
				}
			} else if (expantion.equals(TYPE_FSN)) {
				List<String> conceptIds = Lists.transform(expandedMembers, new Function<ExpandableSnomedRelationship, String>() {
					@Override
					public String apply(ExpandableSnomedRelationship input) {
						return input.getTypeId();
					}
				});
				Map<String, String> conceptIdFsnMap = fsnService.getConceptIdFsnMap(conceptRef, conceptIds, locales);
				for (ExpandableSnomedRelationship relationship : expandedMembers) {
					relationship.setType(createConceptMini(relationship.getTypeId(), conceptIdFsnMap));
				}
			} else {
				throw new BadRequestException("Unrecognised expand parameter '%s'.", expantion);
			}
		}
		return new ArrayList<ISnomedRelationship>(expandedMembers);
	}

	public IComponentList<ISnomedConcept> expandConcepts(IComponentRef conceptRef, IComponentList<ISnomedConcept> concepts, 
				List<Locale> locales, List<String> expantions) {

		if (expantions.isEmpty()) {
			return concepts;
		}
		final List<ISnomedConcept> expandedConcepts = new ArrayList<>();
		for (ISnomedConcept concept : concepts.getMembers()) {
			expandedConcepts.add(new ExpandableSnomedConcept(concept));
		}
		for (String expantion : expantions) {
			if (expantion.equals(FSN)) {
				List<String> conceptIds = Lists.transform(expandedConcepts, new Function<ISnomedConcept, String>() {
					@Override
					public String apply(ISnomedConcept input) {
						return input.getId();
					}
				});
				Map<String, String> conceptIdFsnMap = fsnService.getConceptIdFsnMap(conceptRef, conceptIds, locales);
				for (ISnomedConcept concept : expandedConcepts) {
					((ExpandableSnomedConcept)concept).setFsn(conceptIdFsnMap.get(concept.getId()));
				}
			} else {
				throw new BadRequestException("Unrecognised expand parameter '%s'.", expantion);
			}
		}
		return new ConceptList(concepts.getTotalMembers(), expandedConcepts);
	}
	
	public List<IRelationshipChange> expandRelationshipChanges(IComponentRef componentRef, List<IRelationshipChange> changes, 
			List<Locale> locales, List<String> expantions) {
		
		if (expantions.isEmpty() || changes.isEmpty()) {
			return changes;
		}
		List<IRelationshipChange> changesExtended = new ArrayList<IRelationshipChange>();
		for (IRelationshipChange iRelationshipChange : changes) {
			changesExtended.add(new ExpandableRelationshipChange(iRelationshipChange));
		}

		boolean expandSource = false;
		boolean expandType = false;
		boolean expandDestination = false;
		for (String expantion : expantions) {
			if (expantion.equals(SOURCE_FSN)) {
				expandSource = true;
			} else if (expantion.equals(TYPE_FSN)) {
				expandType = true;
			} else if (expantion.equals(DESTINATION_FSN)) {
				expandDestination = true;
			} else {
				throw new BadRequestException("Unrecognised expand parameter '%s'.", expantion);
			}
		}
		
		Set<String> conceptIds = new HashSet<>();
		for (IRelationshipChange change : changes) {
			if (expandSource) {
				conceptIds.add(change.getSourceId());
			}
			if (expandType) {
				conceptIds.add(change.getTypeId());
			}
			if (expandDestination) {
				conceptIds.add(change.getDestinationId());
			}			
		}
		
		Map<String, String> conceptIdFsnMap = fsnService.getConceptIdFsnMap(componentRef, conceptIds, locales);
		for (IRelationshipChange iChange : changesExtended) {
			ExpandableRelationshipChange change = (ExpandableRelationshipChange) iChange;
			if (expandSource) {
				change.setSource(createConceptMini(change.getSourceId(), conceptIdFsnMap));
			}
			if (expandType) {
				change.setType(createConceptMini(change.getTypeId(), conceptIdFsnMap));
			}
			if (expandDestination) {
				change.setDestination(createConceptMini(change.getDestinationId(), conceptIdFsnMap));
			}			
		}
		
		return changesExtended;
	}
	
	private SnomedConceptMini createConceptMini(String conceptId, Map<String, String> conceptIdFsnMap) {
		return new SnomedConceptMini(conceptId, conceptIdFsnMap.get(conceptId));
	}

	private static final class ConceptList implements IComponentList<ISnomedConcept> {

		private int total;
		private List<ISnomedConcept> members;
		
		public ConceptList(int total, List<ISnomedConcept> members) {
			super();
			this.total = total;
			this.members = members;
		}

		@Override
		public int getTotalMembers() {
			return total;
		}

		@Override
		public List<ISnomedConcept> getMembers() {
			return members;
		}
		
	}

}
