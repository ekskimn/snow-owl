package com.b2international.snowowl.snomed.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.functions.LongToStringFunction;
import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.exceptions.ComponentNotFoundException;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.impl.domain.Predicate;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedConstraints;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.datastore.snor.SnomedConstraintDocument;
import com.b2international.snowowl.snomed.datastore.snor.SnomedConstraintDocument.PredicateType;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

public class SnomedMrcmService {
	
	@Resource
	private IEventBus bus;
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public List<Predicate> getPredicates(String conceptId) {
		
		final String branch = Branch.MAIN_PATH;
		
		final Set<String> selfIds = Collections.singleton(conceptId);
		final Set<String> refSetIds = Collections.emptySet();
		final Set<String> parentIds = SnomedRequests.prepareGetConcept()
				.setComponentId(conceptId)
				.build(branch)
				.execute(ApplicationContext.getServiceForClass(IEventBus.class))
				.then(ISnomedConcept.GET_ANCESTORS) // XXX: includes inferred and stated concepts
				.getSync();
		
		Collection<SnomedConstraintDocument> constraintDocuments = SnomedRequests.prepareGetApplicablePredicates(branch, selfIds, parentIds, refSetIds).getSync();
		List<Predicate> predicates = new ArrayList<>();
		
		for (SnomedConstraintDocument predicateIndexEntry : constraintDocuments) {
			PredicateType type = predicateIndexEntry.getType();
			if (type == PredicateType.RELATIONSHIP) {
				Predicate predicate = new Predicate();
				predicate.setType(type);
				predicate.setRelationshipTypeExpression(predicateIndexEntry.getRelationshipTypeExpression());
				predicate.setRelationshipValueExpression(predicateIndexEntry.getRelationshipValueExpression());
				predicates.add(predicate);
			}
		}
		return predicates;
	}

	public SnomedConcepts getDomainAttributes(String branchPath, List<String> parentIds, 
			int offset, int limit, final List<ExtendedLocale> locales, final String expand) {

		final Set<String> selfIds = Collections.emptySet();
		final Set<String> refSetIds = Collections.emptySet();
		final Set<String> ruleParentIds = ImmutableSet.copyOf(parentIds);

		StringBuilder builder = new StringBuilder();
		
		if (!ruleParentIds.isEmpty()) {
			Collection<SnomedConstraintDocument> constraintDocuments = SnomedRequests.prepareGetApplicablePredicates(branchPath, selfIds, ruleParentIds, refSetIds).getSync();
			Set<String> typeExpressions = new HashSet<>();
			
			for (SnomedConstraintDocument predicateIndexEntry : constraintDocuments) {
				if (predicateIndexEntry.getType() == PredicateType.RELATIONSHIP) {
					typeExpressions.add(predicateIndexEntry.getRelationshipTypeExpression());
				}
			}
			if (typeExpressions.isEmpty()) {
				return new SnomedConcepts(offset, limit, 0);
			}
			for (String typeExpression : typeExpressions) {
				if (builder.length() > 0) {
					builder.append(" UNION ");
				}
				builder.append(typeExpression);
			}
		} else {
			builder.append(Concepts.IS_A);
		}
		
		return SnomedRequests
			.prepareSearchConcept()
			.setLimit(limit)
			.setOffset(offset)
			.filterByEscg(builder.toString())
			.filterByActive(true)
			.setExpand(expand)
			.setLocales(locales)
			.build(branchPath)
			.executeSync(bus);
	}

	public SnomedConcepts getAttributeValues(String branchPath, String attributeId, String termPrefix, 
			int offset, int limit, List<ExtendedLocale> locales, String expand) {
		
		final Collection<String> ancestorIds = SnomedRequests.prepareGetConcept()
				.setComponentId(attributeId)
				.build(branchPath)
				.execute(bus)
				.then(new Function<ISnomedConcept, Collection<String>>() {
					@Override
					public Collection<String> apply(ISnomedConcept input) {
						Set<String> result = Sets.newHashSet();
						result.addAll(LongToStringFunction.copyOf(Longs.asList(input.getParentIds())));
						result.addAll(LongToStringFunction.copyOf(Longs.asList(input.getAncestorIds())));
						return result;
					}
				})
				.getSync();
		
		String relationshipValueExpression = null;
		String relationshipTypeExpression = null;
		
		SnomedConstraints constraints = SnomedRequests.prepareSearchConstraint()
				.all()
				.filterByType(PredicateType.RELATIONSHIP)
				.build(branchPath)
				.execute(bus)
				.getSync();
		
		for (SnomedConstraintDocument constraint : constraints) {
			relationshipTypeExpression = constraint.getRelationshipTypeExpression();
			if (relationshipTypeExpression.startsWith("<")) {
				String relationshipTypeId = relationshipTypeExpression.replace("<", "");
				if ((relationshipTypeExpression.startsWith("<<") && 
						(relationshipTypeId.equals(attributeId) || ancestorIds.contains(relationshipTypeId)))
						|| ancestorIds.contains(relationshipTypeId)) {
					relationshipValueExpression = constraint.getRelationshipValueExpression();
					break;
				}
			} else if (relationshipTypeExpression.equals(attributeId)) {
				relationshipValueExpression = constraint.getRelationshipValueExpression();
				break;
			}
		}
		if (relationshipValueExpression == null) {
			logger.error("No MRCM predicate found for attribute {}", attributeId);
			throw new ComponentNotFoundException("MRCM predicate for attribute", attributeId);
		}
		logger.info("Matched attribute predicate for attribute {}, type expression '{}', value expression '{}'", attributeId, relationshipTypeExpression, relationshipValueExpression);
		
		return SnomedRequests
				.prepareSearchConcept()
				.setLimit(limit)
				.setOffset(offset)
				.filterByEscg(relationshipValueExpression)
				.filterByTerm(termPrefix)
				.filterByActive(true)
				.setExpand(expand)
				.setLocales(locales)
				.build(branchPath)
				.executeSync(bus);
	}
}
