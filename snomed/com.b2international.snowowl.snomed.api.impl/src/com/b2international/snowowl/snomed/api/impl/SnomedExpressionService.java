package com.b2international.snowowl.snomed.api.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.browser.BranchSpecificClientTerminologyBrowser;
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
import com.b2international.snowowl.snomed.datastore.BranchSpecificSnomedClientStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class SnomedExpressionService implements ISnomedExpressionService {

	@Resource
	private IEventBus bus;

	@Override
	public ISnomedExpression getConceptAuthoringForm(String conceptId, String branchPath, List<ExtendedLocale> extendedLocales) {
		
		IBranchPath iBranchPath = BranchPathUtils.createPath(branchPath);
		BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> browser = 
				new BranchSpecificClientTerminologyBrowser<>(getTerminologyBrowser(), iBranchPath);
		FocusConceptNormalizer focusConceptNormalizer = new FocusConceptNormalizer(
				browser, new BranchSpecificSnomedClientStatementBrowser(getStatementBrowser(), iBranchPath));
		final DescriptionService descriptionService = new DescriptionService(bus, branchPath);
		
		final SnomedExpression expression = new SnomedExpression();
		final Map<Integer, SnomedExpressionGroup> groups = new HashMap<>();
		final Map<String, SnomedExpressionConcept> concepts = new HashMap<>();
		final Collection<SnomedRelationshipIndexEntry> relationships = getStatementBrowser().getActiveOutboundStatementsById(iBranchPath, conceptId);
		final Set<String> parents = new HashSet<>();
		for (SnomedRelationshipIndexEntry relationship : relationships) {
			final String attributeId = relationship.getAttributeId();
			if (Concepts.INFERRED_RELATIONSHIP.equals(relationship.getCharacteristicTypeId())) {
				if (Concepts.IS_A.equals(attributeId)) {
					parents.add(relationship.getValueId());
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
							getCreateConcept(relationship.getAttributeId(), concepts, browser), 
							getCreateConcept(relationship.getValueId(), concepts, browser)));
				}
			}
		}
		
		final Collection<SnomedConceptIndexEntry> superTypes = focusConceptNormalizer.collectNonRedundantProximalPrimitiveSuperTypes(parents);
		for (SnomedConceptIndexEntry superType : superTypes) {
			expression.addConcept(getCreateConcept(superType.getId(), concepts, browser));
		}
		
		SnomedServiceHelper.populateConceptTerms(Collections2.transform(concepts.values(), expressionToConceptMinFunction), extendedLocales, descriptionService);
		
		return expression;
	}
	
	private ISnomedExpressionConcept getCreateConcept(String conceptId, Map<String, SnomedExpressionConcept> concepts, 
			BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> browser) {
		
		if (!concepts.containsKey(conceptId)) {
			final SnomedConceptIndexEntry concept = browser.getConcept(conceptId);
			concepts.put(conceptId, new SnomedExpressionConcept(conceptId, concept.isPrimitive()));
		}
		return concepts.get(conceptId);
	}

	private static SnomedTerminologyBrowser getTerminologyBrowser() {
		return ApplicationContext.getServiceForClass(SnomedTerminologyBrowser.class);
	}

	private static SnomedStatementBrowser getStatementBrowser() {
		return ApplicationContext.getServiceForClass(SnomedStatementBrowser.class);
	}
	
	private static final Function<SnomedExpressionConcept, ISnomedConceptMin> expressionToConceptMinFunction = new Function<SnomedExpressionConcept, ISnomedConceptMin>() {
		@Override
		public ISnomedConceptMin apply(SnomedExpressionConcept input) {
			return input;
		}
	};

}
