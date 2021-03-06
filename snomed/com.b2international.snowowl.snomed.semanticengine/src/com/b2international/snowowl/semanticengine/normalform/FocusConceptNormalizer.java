/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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

import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.b2international.collections.PrimitiveSets;
import com.b2international.collections.longs.LongSet;
import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.dsl.scg.Concept;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.semanticengine.subsumption.SubsumptionTester;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;

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
	
	private final class ParentCacheLoader extends CacheLoader<String, ISnomedConcept> {
		
		@Override
		public ISnomedConcept load(String conceptId) throws Exception {
			ISnomedConcept conceptWithParents = SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.setExpand("ancestors(direct:true,form:\"inferred\")")
				.build(branchPath)
				.execute(getServiceForClass(IEventBus.class))
				.getSync();
			
			return conceptWithParents;
		}

		@Override
		public Map<String, ISnomedConcept> loadAll(Iterable<? extends String> conceptIds) throws Exception {
			Collection<String> conceptIdSet = ImmutableSet.copyOf(conceptIds);
			SnomedConcepts conceptsWithParents = SnomedRequests.prepareSearchConcept()
					.setComponentIds(conceptIdSet)
					.setLimit(conceptIdSet.size())
					.setExpand("ancestors(direct:true,form:\"inferred\")")
					.build(branchPath)
					.execute(getServiceForClass(IEventBus.class))
					.getSync();

			return Maps.uniqueIndex(conceptsWithParents, IComponent.ID_FUNCTION);
		}
	}

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
		return normalizeFocusConcepts(focusConcepts, true);
	}

	public FocusConceptNormalizationResult normalizeFocusConcepts(Collection<Concept> focusConcepts, boolean normaliseAttributeValues) {
		Map<String, ISnomedConcept> proximalPrimitiveSuperTypes = collectProximalPrimitiveSupertypes(toIdSet(focusConcepts));
		Collection<ISnomedConcept> filteredPrimitiveSuperTypes = filterRedundantSuperTypes(proximalPrimitiveSuperTypes).values();
		ConceptDefinitionNormalizer conceptDefinitionNormalizer = new ConceptDefinitionNormalizer(branchPath);
		Map<Concept, ConceptDefinition> conceptDefinitions = conceptDefinitionNormalizer.getNormalizedConceptDefinitions(focusConcepts);
		ConceptDefinitionMerger conceptDefinitionMerger = new ConceptDefinitionMerger(subsumptionTester);
		ConceptDefinition mergedFocusConceptDefinitions = conceptDefinitionMerger.mergeDefinitions(conceptDefinitions);
		return new FocusConceptNormalizationResult(filteredPrimitiveSuperTypes, mergedFocusConceptDefinitions);
	}
	
	public Collection<ISnomedConcept> collectNonRedundantProximalPrimitiveSuperTypes(Collection<String> focusConceptIds) {
		final Map<String, ISnomedConcept> proximalPrimitiveSupertypes = collectProximalPrimitiveSupertypes(focusConceptIds);
		return filterRedundantSuperTypes(proximalPrimitiveSupertypes).values();
	}

	private Map<String, ISnomedConcept> collectProximalPrimitiveSupertypes(Collection<String> focusConceptIds) {
		Map<String, ISnomedConcept> proximatePrimitiveSuperTypes = newHashMap();

		if (focusConceptIds.isEmpty()) {
			return proximatePrimitiveSuperTypes;
		}
		
		LoadingCache<String, ISnomedConcept> parentCache = CacheBuilder.newBuilder().build(new ParentCacheLoader());
		
		try {
			
			Iterable<ISnomedConcept> focusConcepts = parentCache.getAll(focusConceptIds).values();
			for (ISnomedConcept focusConcept : focusConcepts) {
				proximatePrimitiveSuperTypes.putAll(getProximatePrimitiveSuperTypes(focusConcept, parentCache));
			}

		} catch (ExecutionException e) {
			throw new UncheckedExecutionException(e.getCause());
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
	 * @param proximalPrimitiveSupertypes
	 * @return
	 */
	private Map<String, ISnomedConcept> filterRedundantSuperTypes(Map<String, ISnomedConcept> proximalPrimitiveSupertypes) {
		Map<String, ISnomedConcept> filteredSuperTypes = newHashMap(proximalPrimitiveSupertypes);
		
		for (Entry<String, ISnomedConcept> superType: proximalPrimitiveSupertypes.entrySet()) {
			for (Entry<String, ISnomedConcept> otherSuperType: proximalPrimitiveSupertypes.entrySet()) {
				if (!otherSuperType.getKey().equals(superType.getKey()) && filteredSuperTypes.containsKey(otherSuperType.getKey()) && isSuperTypeOf(superType.getValue(), otherSuperType.getValue())) {
					filteredSuperTypes.remove(superType.getKey());
				}
			}
		}
		
		return filteredSuperTypes;
	}

	private Map<String, ISnomedConcept> getProximatePrimitiveSuperTypes(ISnomedConcept focusConceptWithParents, LoadingCache<String, ISnomedConcept> parentCache) {
		Map<String, ISnomedConcept> proximatePrimitiveSuperTypes = newHashMap();
		
		if (focusConceptWithParents.getDefinitionStatus().isPrimitive()) {
			proximatePrimitiveSuperTypes.put(focusConceptWithParents.getId(), focusConceptWithParents);
			return proximatePrimitiveSuperTypes;
		}
		
		for (ISnomedConcept parent : focusConceptWithParents.getAncestors()) {
			if (parent.getDefinitionStatus().isPrimitive()) {
				proximatePrimitiveSuperTypes.put(parent.getId(), parent);
			} else {
				final ISnomedConcept parentWithParents = parentCache.getUnchecked(parent.getId());
				proximatePrimitiveSuperTypes.putAll(getProximatePrimitiveSuperTypes(parentWithParents, parentCache));
			}
		}
		
		return filterSuperTypesToProximate(proximatePrimitiveSuperTypes);
	}

	private Map<String, ISnomedConcept> filterSuperTypesToProximate(Map<String, ISnomedConcept> proximatePrimitiveSuperTypes) {
			Map<String, ISnomedConcept> filteredProximateSuperTypes = newHashMap();
			
			for (Entry<String, ISnomedConcept> superType : proximatePrimitiveSuperTypes.entrySet()) {
				if (filteredProximateSuperTypes.isEmpty()) {
					filteredProximateSuperTypes.put(superType.getKey(), superType.getValue());
				} else {
					// remove types from proximateSuperTypes, if there is a more specific type among the superTypes
					boolean toBeAdded = false;
					Set<ISnomedConcept> removedProximateSuperTypes = new HashSet<ISnomedConcept>();
					for (ISnomedConcept proximateSuperType : filteredProximateSuperTypes.values()) {
						/*
						 * If the super type is a super type of a type already in the proximate super type set, then 
						 * it shouldn't be added, no further checks necessary.
						 */
						if (isSuperTypeOf(superType.getValue(), proximateSuperType)) {
							toBeAdded = false;
							break;
						}
						
						/* 
						 * Remove super type and add more specific type. In case of multiple super types we get here several times, 
						 * but since we are using Set<Integer>, adding the same concept multiple times is not an issue. 
						 */
						if (isSuperTypeOf(proximateSuperType, superType.getValue())) {
							removedProximateSuperTypes.add(proximateSuperType);
						}
						
						toBeAdded = true;
					}
					
					// process differences
					filteredProximateSuperTypes.values().removeAll(removedProximateSuperTypes);
					if (toBeAdded) {
						filteredProximateSuperTypes.put(superType.getKey(), superType.getValue());
					}
				}
			}
			
			return filteredProximateSuperTypes;
		}

	private boolean isSuperTypeOf(ISnomedConcept superType, ISnomedConcept subType) {
		long superTypeId = Long.parseLong(superType.getId());
		LongSet ancestorIds = PrimitiveSets.newLongOpenHashSet(subType.getAncestorIds());
		LongSet parentIds = PrimitiveSets.newLongOpenHashSet(subType.getParentIds());
		
		return ancestorIds.contains(superTypeId) || parentIds.contains(superTypeId);
	}
	
	private Collection<String> toIdSet(Collection<Concept> focusConcepts) {
		return Collections2.transform(focusConcepts, new Function<Concept, String>() {
			@Override
			public String apply(Concept input) {
				return input.getId();
			}
		});
	}
}
