package com.b2international.snowowl.snomed.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.b2international.snowowl.api.domain.IComponentList;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.api.ISnomedConceptService;
import com.b2international.snowowl.snomed.api.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.api.domain.SearchKind;
import com.b2international.snowowl.snomed.api.impl.domain.Predicate;
import com.b2international.snowowl.snomed.api.impl.domain.SnomedConceptList;
import com.b2international.snowowl.snomed.datastore.SnomedPredicateBrowser;
import com.b2international.snowowl.snomed.datastore.snor.PredicateIndexEntry;
import com.b2international.snowowl.snomed.datastore.snor.PredicateIndexEntry.PredicateType;

public class SnomedMrcmService {
	
	@Resource
	protected ISnomedConceptService conceptService;

	public List<Predicate> getPredicates(String conceptId) {
		IBranchPath mainPath = BranchPathUtils.createMainPath();
		Collection<PredicateIndexEntry> indexEntries = getPredicateBrowser().getPredicates(mainPath, conceptId, null);
		List<Predicate> predicates = new ArrayList<>();
		for (PredicateIndexEntry predicateIndexEntry : indexEntries) {
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

	public IComponentList<ISnomedConcept> getDomainAttributes(String branchPath, List<String> parentIds, int offset, int limit) {
		IBranchPath path = BranchPathUtils.createPath(branchPath);
		Collection<PredicateIndexEntry> predicates = getPredicateBrowser().getPredicates(path, parentIds, null);
		Set<String> typeExpressions = new HashSet<>();
		for (PredicateIndexEntry predicateIndexEntry : predicates) {
			if (predicateIndexEntry.getType() == PredicateType.RELATIONSHIP) {
				typeExpressions.add(predicateIndexEntry.getRelationshipTypeExpression());
			}
		}
		if (typeExpressions.isEmpty()) {
			return new SnomedConceptList();
		}
		StringBuilder builder = new StringBuilder();
		for (String typeExpression : typeExpressions) {
			if (builder.length() > 0) {
				builder.append(" UNION ");
			}
			builder.append(typeExpression);
		}
		Map<SearchKind, String> queryParams = new HashMap<>();
		queryParams.put(SearchKind.ESCG, builder.toString());
		return conceptService.search(branchPath, queryParams, offset, limit);
	}

	private SnomedPredicateBrowser getPredicateBrowser() {
		SnomedPredicateBrowser predicateBrowser = ApplicationContext.getInstance().getService(SnomedPredicateBrowser.class);
		return predicateBrowser;
	}

}
