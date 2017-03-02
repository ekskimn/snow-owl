package com.b2international.snowowl.snomed.api.impl;

import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;

import java.util.List;

import org.eclipse.emf.common.util.EList;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.dsl.scg.Attribute;
import com.b2international.snowowl.dsl.scg.AttributeValue;
import com.b2international.snowowl.dsl.scg.Concept;
import com.b2international.snowowl.dsl.scg.Expression;
import com.b2international.snowowl.dsl.scg.Group;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttributeValue;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpression;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.impl.domain.expression.SnomedExpressionGroup;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;

public class SnomedExpresssionConverter {

	private final String branch;
	private final List<ExtendedLocale> extendedLocales;

	public SnomedExpresssionConverter(String branch, List<ExtendedLocale> extendedLocales) {
		this.branch = branch;
		this.extendedLocales = extendedLocales;
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
		// TODO: Collect all concepts in advance by traversing the expression 
		return SnomedRequests.prepareGetConcept()
			.setComponentId(concept.getId())
			.setLocales(extendedLocales)
			.setExpand("fsn()")
			.build(SnomedDatastoreActivator.REPOSITORY_UUID, branch)
			.execute(getServiceForClass(IEventBus.class))
			.then(new Function<SnomedConcept, SnomedExpressionConcept>() {
				@Override public SnomedExpressionConcept apply(SnomedConcept input) {
					final SnomedExpressionConcept expressionConcept = new SnomedExpressionConcept(input.getId(), input.getDefinitionStatus().isPrimitive());
					expressionConcept.setTerm(input.getFsn().getTerm());
					return expressionConcept;
				}
			})
			.getSync();
	}
}
