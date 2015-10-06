package com.b2international.snowowl.snomed.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.api.impl.domain.Predicate;
import com.b2international.snowowl.snomed.datastore.SnomedPredicateBrowser;
import com.b2international.snowowl.snomed.datastore.snor.PredicateIndexEntry;
import com.b2international.snowowl.snomed.datastore.snor.PredicateIndexEntry.PredicateType;

public class SnomedMrcmService {

	public List<Predicate> getPredicates(String conceptId) {
		SnomedPredicateBrowser predicateBrowser = ApplicationContext.getInstance().getService(SnomedPredicateBrowser.class);
		IBranchPath mainPath = BranchPathUtils.createMainPath();
		Collection<PredicateIndexEntry> indexEntries = predicateBrowser.getPredicates(mainPath, conceptId, null);
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

}
