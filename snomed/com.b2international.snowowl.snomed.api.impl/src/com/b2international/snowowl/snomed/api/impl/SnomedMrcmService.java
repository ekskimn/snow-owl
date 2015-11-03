package com.b2international.snowowl.snomed.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private Logger logger = LoggerFactory.getLogger(getClass());

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
		Collection<PredicateIndexEntry> predicates = getPredicateBrowser().getPredicates(getBranch(branchPath), parentIds, null);
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

	public IComponentList<ISnomedConcept> getAttributeValues(String branchPath,
			String attributeId, String termPrefix, int offset, int limit) {
		
		String relationshipValueExpression = null;
		Collection<PredicateIndexEntry> predicates = getPredicateBrowser().getAllPredicates(getBranch(branchPath));
		for (PredicateIndexEntry predicateIndexEntry : predicates) {
			if (predicateIndexEntry.getType() == PredicateType.RELATIONSHIP 
					&& predicateIndexEntry.getRelationshipTypeExpression().equals(attributeId)) {
				relationshipValueExpression = predicateIndexEntry.getRelationshipValueExpression();
			}
		}
		
		Map<SearchKind, String> queryParams = new HashMap<>();
		if (relationshipValueExpression == null) {
			queryParams.put(SearchKind.ESCG, relationshipValueExpression);			
		} else {
			logger.warn("No MRCM predicate matched for attribute {}", attributeId);
		}
		
		if (termPrefix != null && !termPrefix.isEmpty()) {
			queryParams.put(SearchKind.LABEL, termPrefix);
		}
		
		return conceptService.search(branchPath, queryParams, offset, limit);
	}
	
	private IBranchPath getBranch(String branchPath) {
		IBranchPath path = BranchPathUtils.createPath(branchPath);
		return path;
	}

	private SnomedPredicateBrowser getPredicateBrowser() {
		SnomedPredicateBrowser predicateBrowser = ApplicationContext.getInstance().getService(SnomedPredicateBrowser.class);
		return predicateBrowser;
	}

}
