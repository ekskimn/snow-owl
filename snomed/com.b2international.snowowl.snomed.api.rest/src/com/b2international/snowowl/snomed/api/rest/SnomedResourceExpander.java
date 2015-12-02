package com.b2international.snowowl.snomed.api.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.domain.classification.IRelationshipChange;
import com.b2international.snowowl.snomed.api.impl.DescriptionService;
import com.b2international.snowowl.snomed.api.rest.domain.ExpandableRelationshipChange;
import com.b2international.snowowl.snomed.api.rest.domain.ExpandableSnomedRelationship;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedConceptMini;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

public class SnomedResourceExpander {

	@Resource
	protected IEventBus bus;

	private static final String SOURCE_FSN = "source.fsn";
	private static final String TYPE_FSN = "type.fsn";
	private static final String DESTINATION_FSN = "destination.fsn";

	public static final String FSN = "fsn";

	public List<ISnomedRelationship> expandRelationships(String branchPath, List<ISnomedRelationship> members, 
			final List<ExtendedLocale> extendedLocales, List<String> expantions) {
		
		final DescriptionService descriptionService = new DescriptionService(bus, branchPath);
		
		if (expantions.isEmpty()) {
			return members;			
		}
		
		final List<ExpandableSnomedRelationship> expandedMembers = new ArrayList<>();
		for (ISnomedRelationship relationship : members) {
			expandedMembers.add(new ExpandableSnomedRelationship(relationship));
		}
		
		for (String expand : expantions) {
			if (expand.equals(SOURCE_FSN)) {
				
				Set<String> conceptIds = FluentIterable.from(expandedMembers)
						.transform(new Function<ExpandableSnomedRelationship, String>() {
							@Override public String apply(ExpandableSnomedRelationship input) {
								return input.getSourceId();
							}})
						.toSet();
				
				Map<String, ISnomedDescription> fsnMap = descriptionService.getFullySpecifiedNames(conceptIds, extendedLocales);
				for (ExpandableSnomedRelationship relationship : expandedMembers) {
					String sourceId = relationship.getSourceId();
					relationship.setSource(new SnomedConceptMini(sourceId, getFsn(fsnMap, sourceId)));
				}
				
			} else if (expand.equals(TYPE_FSN)) {
				
				Set<String> conceptIds = FluentIterable.from(expandedMembers)
						.transform(new Function<ExpandableSnomedRelationship, String>() {
							@Override public String apply(ExpandableSnomedRelationship input) {
								return input.getTypeId();
							}})
						.toSet();
				
				Map<String, ISnomedDescription> fsnMap = descriptionService.getFullySpecifiedNames(conceptIds, extendedLocales);
				for (ExpandableSnomedRelationship relationship : expandedMembers) {
					String typeId = relationship.getTypeId();
					relationship.setType(new SnomedConceptMini(typeId, getFsn(fsnMap, typeId)));
				}
				
			} else {
				throw new BadRequestException("Unrecognised expand parameter '%s'.", expand);
			}
		}
		
		return new ArrayList<ISnomedRelationship>(expandedMembers);
	}

	public List<IRelationshipChange> expandRelationshipChanges(String branchPath, List<IRelationshipChange> changes, 
			List<ExtendedLocale> locales, List<String> expantions) {

		final DescriptionService descriptionService = new DescriptionService(bus, branchPath);

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
		
		Map<String, ISnomedDescription> fullySpecifiedNames = descriptionService.getFullySpecifiedNames(conceptIds, locales);
		for (IRelationshipChange iChange : changesExtended) {
			ExpandableRelationshipChange change = (ExpandableRelationshipChange) iChange;
			if (expandSource) {
				change.setSource(createConceptMini(change.getSourceId(), fullySpecifiedNames));
			}
			if (expandType) {
				change.setType(createConceptMini(change.getTypeId(), fullySpecifiedNames));
			}
			if (expandDestination) {
				change.setDestination(createConceptMini(change.getDestinationId(), fullySpecifiedNames));
			}			
		}
		
		return changesExtended;
	}
	
	public SnomedConcepts expandConcepts(String branchPath, SnomedConcepts concepts, 
			List<ExtendedLocale> locales, List<String> expantions) {
		
		if (expantions.isEmpty() || concepts.getItems().isEmpty()) {
			return null;
		}

		for (String expantion : expantions) {
			if (expantion.equals(FSN)) {
				final DescriptionService descriptionService = new DescriptionService(bus, branchPath);

				Set<String> conceptIds = FluentIterable.from(concepts.getItems())
						.transform(new Function<ISnomedConcept, String>() {
							@Override public String apply(ISnomedConcept input) {
								return input.getId();
							}})
						.toSet();
				
				Map<String, ISnomedDescription> fullySpecifiedNames = descriptionService.getFullySpecifiedNames(conceptIds, locales);
				for (ISnomedConcept iSnomedConcept : concepts) {
					SnomedConcept concept = (SnomedConcept) iSnomedConcept;
					concept.setFsn(fullySpecifiedNames.get(concept.getId()));
				}
			} else {
				throw new BadRequestException("Unrecognised expand parameter '%s'.", expantion);
			}
		}
		
		return concepts;
	}
	
	private SnomedConceptMini createConceptMini(String conceptId, Map<String, ISnomedDescription> fullySpecifiedNames) {
		return new SnomedConceptMini(conceptId, getFsn(fullySpecifiedNames, conceptId));
	}

	private String getFsn(Map<String, ISnomedDescription> fsnMap, String conceptId) {
		if (fsnMap.containsKey(conceptId)) {
			return fsnMap.get(conceptId).getTerm();
		} else {
			return conceptId;
		}
	}

}
