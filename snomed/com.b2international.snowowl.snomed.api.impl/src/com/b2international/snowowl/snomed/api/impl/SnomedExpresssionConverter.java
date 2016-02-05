package com.b2international.snowowl.snomed.api.impl;

import java.util.List;

import org.eclipse.emf.common.util.EList;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.datastore.browser.BranchSpecificClientTerminologyBrowser;
import com.b2international.snowowl.dsl.scg.Attribute;
import com.b2international.snowowl.dsl.scg.AttributeValue;
import com.b2international.snowowl.dsl.scg.Concept;
import com.b2international.snowowl.dsl.scg.Expression;
import com.b2international.snowowl.dsl.scg.Group;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttributeValue;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpression;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionGroup;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;

public class SnomedExpresssionConverter {

	private BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> browser;
	private List<ExtendedLocale> extendedLocales;
	private DescriptionService descriptionService;

	public SnomedExpresssionConverter(BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> browser, 
			List<ExtendedLocale> extendedLocales, DescriptionService descriptionService) {
		this.browser = browser;
		this.extendedLocales = extendedLocales;
		this.descriptionService = descriptionService;
	}

	public ISnomedExpression convert(Expression expression) {
		final SnomedExpression conceptExpression = new SnomedExpression();
		final EList<Concept> concepts = expression.getConcepts();
		for (Concept concept : concepts) {
			conceptExpression.addConcept(convert(concept));
		}
		
		for (Attribute attribute : expression.getAttributes()) {
			conceptExpression.addAttribute(convert(attribute));
		}
		
		for (Group group : expression.getGroups()) {
			final SnomedExpressionGroup expressionGroup = new SnomedExpressionGroup();
			for (Attribute attribute : group.getAttributes()) {
				expressionGroup.addAttribute(convert(attribute));
			}
			conceptExpression.addGroup(expressionGroup);
		}
		
		return conceptExpression;
	}

	private SnomedExpressionAttribute convert(Attribute attribute) {
		final AttributeValue value = attribute.getValue();
		final SnomedExpressionConcept attributeType = convert(attribute.getName());
		ISnomedExpressionAttributeValue attributeValue;
		if (value instanceof Concept) {
			Concept conceptValue = (Concept) value;
			attributeValue = convert(conceptValue);
		} else if (value instanceof Expression) {
			Expression expressionValue = (Expression) value;
			attributeValue = convert(expressionValue);
		} else {
			throw new RuntimeException("Unrecognised AttributeValueType " + value.getClass());
		}
		return new SnomedExpressionAttribute(attributeType, attributeValue);
	}

	private SnomedExpressionConcept convert(Concept concept) {
		final String id = concept.getId();
		final ISnomedDescription fullySpecifiedName = descriptionService.getFullySpecifiedName(id, extendedLocales);
		return new SnomedExpressionConcept(id, fullySpecifiedName.getTerm(), browser.getConcept(id).isPrimitive());
	}

}
