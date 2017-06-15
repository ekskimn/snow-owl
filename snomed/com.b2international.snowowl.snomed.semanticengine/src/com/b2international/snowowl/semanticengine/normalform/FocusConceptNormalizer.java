/*
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.semanticengine.normalform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.dsl.scg.Concept;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.semanticengine.subsumption.SubsumptionTester;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * <b>5.3.3 Normalize focus concepts</b><br/>
 * The set of focus concepts is normalized to generated two separate outputs:<br/><br/>
 * <b>5.3.3.1 The set of normalized definitions of each focus concept</b><br/>
 * The set of normalized definitions includes a separate normalized definition for each focus concept,
 * The normalized definition includes
 * <ul>
 * <li>All ungrouped relationships</li>
 * <li>All relationship groups complete with contained relationships</li>
 * </ul>
 * All relationship values are normalized by recursively following the full set of rules described 
 * in section 5.3.<br/>
 * Note: Storage of pre-computed normalized form of concept definitions simplifies this process as 
 * it removes the requirement for recursive processing of definitions at run time.
 * The set of normalized definitions is passed to the "Merge definitions" process (5.3.4).<br/><br/>
 * <b>5.3.3.2 The non-redundant proximal primitive supertypes of the focus concepts</b><br/>
 * The non-redundant proximal primitive supertypes of the focus concepts is the set of all primitive 
 * supertypes of all the focus concepts with redundant concepts removed.<br/>
 * A concept is redundant if it is: 
 * <ul>
 * <li>A duplicate of another member of the set</li>
 * <li>A super type of another concept in the set.</li>
 * </ul>
 * The set of proximal primitive supertypes generated by this process is passed to the 
 * "Create expression" process (5.3.6) as the focus concepts for the output expression.
 * 
 */
public class FocusConceptNormalizer {
	
	private final String branchPath;
	private final SubsumptionTester subsumptionTester;
	
	public FocusConceptNormalizer(String branchPath) {
		this.branchPath = branchPath;
		this.subsumptionTester = new SubsumptionTester(branchPath);
	}

	/** 
	 * @param focusConcepts
	 * @return the normalized focus concepts
	 */
	public FocusConceptNormalizationResult normalizeFocusConcepts(Collection<Concept> focusConcepts) {
		Collection<SnomedConceptDocument> proximalPrimitiveSuperTypes = collectProximalPrimitiveSupertypes(toIdSet(focusConcepts));
		Collection<SnomedConceptDocument> filteredPrimitiveSuperTypes = filterRedundantSuperTypes(proximalPrimitiveSuperTypes);
		ConceptDefinitionNormalizer conceptDefinitionNormalizer = new ConceptDefinitionNormalizer(branchPath);
		Map<Concept, ConceptDefinition> conceptDefinitions = conceptDefinitionNormalizer.getNormalizedConceptDefinitions(focusConcepts);
		ConceptDefinitionMerger conceptDefinitionMerger = new ConceptDefinitionMerger(subsumptionTester);
		ConceptDefinition mergedFocusConceptDefinitions = conceptDefinitionMerger.mergeDefinitions(conceptDefinitions);
		return new FocusConceptNormalizationResult(filteredPrimitiveSuperTypes, mergedFocusConceptDefinitions);
	}
	
	private Collection<SnomedConceptDocument> collectProximalPrimitiveSupertypes(Collection<String> focusConceptIds) {
		Set<SnomedConceptDocument> proximatePrimitiveSuperTypes = new HashSet<>();

		if (focusConceptIds.isEmpty()) {
			return proximatePrimitiveSuperTypes;
		}
		
		Collection<SnomedConceptDocument> focusConcepts = SnomedRequests.prepareSearchConcept()
				.filterByIds(focusConceptIds)
				.setLimit(focusConceptIds.size())
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(ApplicationContext.getServiceForClass(IEventBus.class))
				.then(new Function<SnomedConcepts, Collection<SnomedConceptDocument>>() {
					@Override
					public Collection<SnomedConceptDocument> apply(SnomedConcepts input) {
						return SnomedConceptDocument.fromConcepts(input);
					}
				})
				.getSync();
				
		
		for (SnomedConceptDocument focusConcept : focusConcepts) {
			proximatePrimitiveSuperTypes.addAll(getProximatePrimitiveSuperTypes(focusConcept));
		}
		
		return proximatePrimitiveSuperTypes;
	}

	/**
	 * The non-redundant proximal primitive supertypes of the focus concepts is the set of 
	 * all primitive supertypes of all the focus concepts with redundant concepts removed.<br/>
	 * A concept is redundant if it is:
	 * <ul>
	 * <li>A duplicate of another member of the set</li>
	 * <li>A super type of another concept in the set.</li>
	 * </ul>
	 * @param proximalPrimitiveSuperTypes
	 * @return
	 */
	private Collection<SnomedConceptDocument> filterRedundantSuperTypes(Collection<SnomedConceptDocument> proximalPrimitiveSuperTypes) {
		List<SnomedConceptDocument> filteredSuperTypes = new ArrayList<SnomedConceptDocument>(proximalPrimitiveSuperTypes);
		
		for(SnomedConceptDocument superType: proximalPrimitiveSuperTypes) {
			for(SnomedConceptDocument otherSuperType: proximalPrimitiveSuperTypes) {
				if (!otherSuperType.equals(superType) && filteredSuperTypes.contains(otherSuperType) && isSuperTypeOf(superType, otherSuperType)) {
					filteredSuperTypes.remove(superType);
				}
			}
		}
		
		return filteredSuperTypes;
	}

	private Set<SnomedConceptDocument> getProximatePrimitiveSuperTypes(SnomedConceptDocument focusConcept) {
		Set<SnomedConceptDocument> proximatePrimitiveSuperTypes = new HashSet<SnomedConceptDocument>();
		
		if (focusConcept.isPrimitive()) {
			proximatePrimitiveSuperTypes.add(focusConcept);
			return proximatePrimitiveSuperTypes;
		}
		
		final SnomedRelationships outboundRelationships = SnomedRequests.prepareSearchRelationship()
				.all()
				.filterByActive(true)
				.filterByType(Concepts.IS_A)
				.filterByCharacteristicType(Concepts.INFERRED_RELATIONSHIP)
				.filterBySource(focusConcept.getId())
				.setExpand("destination()")
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(ApplicationContext.getServiceForClass(IEventBus.class))
				.getSync();
		//for (int i = 0; i < outgoingRelationships.length; i++) {
		for (SnomedRelationship relationship : outboundRelationships) {
			SnomedConcept destinationConcept = relationship.getDestination();
			SnomedConceptDocument destinationConceptDocument = SnomedConceptDocument.builder(destinationConcept).build();
			
			if (destinationConcept.getDefinitionStatus().isPrimitive()) {
				proximatePrimitiveSuperTypes.add(destinationConceptDocument);
			} else {
				proximatePrimitiveSuperTypes.addAll(getProximatePrimitiveSuperTypes(destinationConceptDocument));
			}
		}
		return filterSuperTypesToProximate(proximatePrimitiveSuperTypes);
	}

	private Set<SnomedConceptDocument> filterSuperTypesToProximate(Set<SnomedConceptDocument> superTypes) {
			Set<SnomedConceptDocument> filteredProximateSuperTypes = new HashSet<SnomedConceptDocument>();
			
			for (SnomedConceptDocument superType : superTypes) {
				if (filteredProximateSuperTypes.isEmpty()) {
					filteredProximateSuperTypes.add(superType);
				} else {
					// remove types from proximateSuperTypes, if there is a more specific type among the superTypes
					boolean toBeAdded = false;
					Set<SnomedConceptDocument> removedProximateSuperTypes = new HashSet<SnomedConceptDocument>();
					for (SnomedConceptDocument proximateSuperType : filteredProximateSuperTypes) {
						/*
						 * If the super type is a super type of a type already in the proximate super type set, then 
						 * it shouldn't be added, no further checks necessary.
						 */
						if (isSuperTypeOf(superType, proximateSuperType)) {
							toBeAdded = false;
							break;
						}
						
						/* 
						 * Remove super type and add more specific type. In case of multiple super types we get here several times, 
						 * but since we are using Set<Integer>, adding the same concept multiple times is not an issue. 
						 */
						if (isSuperTypeOf(proximateSuperType, superType)) {
							removedProximateSuperTypes.add(proximateSuperType);
						}
						
						toBeAdded = true;
					}
					
					// process differences
					filteredProximateSuperTypes.removeAll(removedProximateSuperTypes);
					if (toBeAdded) {
						filteredProximateSuperTypes.add(superType);
					}
				}
			}
			
			return filteredProximateSuperTypes;
		}

	private boolean isSuperTypeOf(SnomedConceptDocument superType, SnomedConceptDocument subType) {
		long superTypeId = Long.parseLong(superType.getId());
		return subType.getAncestors().contains(superTypeId) || subType.getParents().contains(superTypeId);
	}
	
	private Collection<String> toIdSet(Collection<Concept> focusConcepts) {
		return Collections2.transform(focusConcepts, new Function<Concept, String>() {
			@Override
			public String apply(Concept input) {
				return input.getId();
			}
		});
	}

	public Collection<SnomedConceptDocument> collectNonRedundantProximalPrimitiveSuperTypes(Set<String> parents) {
		Collection<SnomedConceptDocument> proximalPrimitiveSupertypes = collectProximalPrimitiveSupertypes(parents);
		return filterRedundantSuperTypes(proximalPrimitiveSupertypes);
	}
	
}