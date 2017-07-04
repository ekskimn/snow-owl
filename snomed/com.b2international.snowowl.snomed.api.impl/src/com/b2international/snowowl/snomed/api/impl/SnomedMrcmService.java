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
import com.b2international.snowowl.datastore.request.SearchResourceRequest;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.impl.domain.Predicate;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedConstraint;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedConstraints;
import com.b2international.snowowl.snomed.core.domain.constraint.SnomedRelationshipConstraint;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
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
		final Set<String> parentIds = SnomedRequests.prepareGetConcept(conceptId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branch)
				.execute(ApplicationContext.getServiceForClass(IEventBus.class))
				.then(SnomedConcept.GET_ANCESTORS) // XXX: includes inferred and stated concepts
				.getSync();
		
		Collection<SnomedConstraint> constraintDocuments = SnomedRequests.prepareGetApplicablePredicates(branch, selfIds, parentIds, refSetIds).getSync();
		List<Predicate> predicates = new ArrayList<>();
		
		for (SnomedConstraint predicateIndexEntry : constraintDocuments) {
			if (predicateIndexEntry instanceof SnomedRelationshipConstraint) {
				Predicate predicate = new Predicate();
				predicate.setType(PredicateType.RELATIONSHIP);
				SnomedRelationshipConstraint relationshipConstraint = ((SnomedRelationshipConstraint) predicateIndexEntry);
				
				predicate.setRelationshipTypeExpression(relationshipConstraint.getType());
				predicate.setRelationshipValueExpression(relationshipConstraint.getDestinationExpression());
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
			Collection<SnomedConstraint> constraints = SnomedRequests.prepareGetApplicablePredicates(branchPath, selfIds, ruleParentIds, refSetIds).getSync();
			Set<String> typeExpressions = new HashSet<>();
			
			for (SnomedConstraint constraint : constraints) {
				if (constraint instanceof SnomedRelationshipConstraint) {
					typeExpressions.add(((SnomedRelationshipConstraint) constraint).getType());
				}
			}
			if (typeExpressions.isEmpty()) {
				return new SnomedConcepts(offset, limit, 0);
			}
			for (String typeExpression : typeExpressions) {
				if (builder.length() > 0) {
					builder.append(" OR ");
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
			.filterByEcl(builder.toString())
			.filterByActive(true)
			.setExpand(expand)
			.setLocales(locales)
			.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
			.execute(bus)
			.getSync();
	}

	public SnomedConcepts getAttributeValues(String branchPath, String attributeId, String termPrefix, 
			int offset, int limit, List<ExtendedLocale> locales, String expand) {
		
		final Collection<String> ancestorIds = SnomedRequests.prepareGetConcept(attributeId)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus)
				.then(new Function<SnomedConcept, Collection<String>>() {
					@Override
					public Collection<String> apply(SnomedConcept input) {
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
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus)
				.getSync();
		
		for (SnomedConstraint constraint : constraints) {
			
			if (!(constraint instanceof SnomedRelationshipConstraint))
				continue;
			
			SnomedRelationshipConstraint relationshipConstraint = (SnomedRelationshipConstraint) constraint;
			
			relationshipTypeExpression = relationshipConstraint.getType();
			if (relationshipTypeExpression.startsWith("<")) {
				String relationshipTypeId = relationshipTypeExpression.replace("<", "");
				if ((relationshipTypeExpression.startsWith("<<") && 
						(relationshipTypeId.equals(attributeId) || ancestorIds.contains(relationshipTypeId)))
						|| ancestorIds.contains(relationshipTypeId)) {
					relationshipValueExpression = relationshipConstraint.getDestinationExpression();
					break;
				}
			} else if (relationshipTypeExpression.equals(attributeId)) {
				relationshipValueExpression = relationshipConstraint.getDestinationExpression();
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
				.filterByEcl(relationshipValueExpression)
				.filterByTerm(termPrefix)
				.filterByActive(true)
				.setExpand(expand)
				.setLocales(locales)
				.sortBy(SearchResourceRequest.SCORE)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus)
				.getSync();
	}
}
