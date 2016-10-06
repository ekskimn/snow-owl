package com.b2international.snowowl.snomed.api.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.semanticengine.normalform.FocusConceptNormalizer;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.ISnomedExpressionService;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.impl.domain.ISnomedConceptMin;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpression;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionGroup;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class SnomedExpressionService implements ISnomedExpressionService {

	@Resource
	private IEventBus bus;

	@Override
	public ISnomedExpression getConceptAuthoringForm(String conceptId, String branchPath, List<ExtendedLocale> extendedLocales) {
		
		FocusConceptNormalizer focusConceptNormalizer = new FocusConceptNormalizer(branchPath);
		final DescriptionService descriptionService = new DescriptionService(bus, branchPath);
		
		final SnomedExpression expression = new SnomedExpression();
		final Map<Integer, SnomedExpressionGroup> groups = new HashMap<>();
		final Map<String, SnomedExpressionConcept> concepts = new HashMap<>();
		final SnomedRelationships relationships = getActiveInferredRelationships(branchPath, conceptId);
		final Set<String> parents = new HashSet<>();
		for (ISnomedRelationship relationship : relationships) {
			final String attributeId = relationship.getTypeId();
			if (Concepts.IS_A.equals(attributeId)) {
				parents.add(relationship.getDestinationId());
			} else {
				final int groupNum = relationship.getGroup();
				List<ISnomedExpressionAttribute> attributes;
				if (groupNum == 0) {
					attributes = expression.getAttributes();
				} else {
					if (!groups.containsKey(groupNum)) {
						final SnomedExpressionGroup group = new SnomedExpressionGroup();
						groups.put(groupNum, group);
						expression.addGroup(group);
					}
					attributes = groups.get(groupNum).getAttributes();
				}
				attributes.add(new SnomedExpressionAttribute(
						getCreateConcept(branchPath, relationship.getTypeConcept(), concepts), 
						getCreateConcept(branchPath, relationship.getDestinationConcept(), concepts)));
			}
		}
		
		final Collection<ISnomedConcept> superTypes = focusConceptNormalizer.collectNonRedundantProximalPrimitiveSuperTypes(parents);
		for (ISnomedConcept superType : superTypes) {
			expression.addConcept(getCreateConcept(branchPath, superType, concepts));
		}
		
		SnomedServiceHelper.populateConceptTerms(Collections2.transform(concepts.values(), expressionToConceptMinFunction), extendedLocales, descriptionService);
		
		return expression;
	}
	
	private SnomedRelationships getActiveInferredRelationships(String branchPath, String conceptId) {
		return SnomedRequests.prepareSearchRelationship()
				.filterByActive(true)
				.filterBySource(conceptId)
				.filterByCharacteristicType(Concepts.INFERRED_RELATIONSHIP)
				.setExpand("destination(),type()")
				.build(branchPath)
				.execute(bus)
				.getSync();
	}

	private ISnomedExpressionConcept getCreateConcept(String branchPath, ISnomedConcept concept, Map<String, SnomedExpressionConcept> concepts) {
		final String conceptId = concept.getId();
		
		if (!concepts.containsKey(conceptId)) {
			boolean primitive = concept.getDefinitionStatus().isPrimitive();
			concepts.put(conceptId, new SnomedExpressionConcept(conceptId, primitive));
		}
		
		return concepts.get(concept);
	}

	private static final Function<SnomedExpressionConcept, ISnomedConceptMin> expressionToConceptMinFunction = new Function<SnomedExpressionConcept, ISnomedConceptMin>() {
		@Override
		public ISnomedConceptMin apply(SnomedExpressionConcept input) {
			return input;
		}
	};

}
