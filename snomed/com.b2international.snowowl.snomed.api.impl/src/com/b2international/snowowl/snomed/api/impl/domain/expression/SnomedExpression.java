package com.b2international.snowowl.snomed.api.impl.domain.expression;

import java.util.ArrayList;
import java.util.List;

import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionGroup;

public class SnomedExpression implements ISnomedExpression {

	private List<ISnomedExpressionConcept> concepts;
	private List<ISnomedExpressionAttribute> attributes;
	private List<ISnomedExpressionGroup> groups;
	
	public SnomedExpression() {
		concepts = new ArrayList<>();
		attributes = new ArrayList<>();
		groups = new ArrayList<>();
	}

	public void addConcept(ISnomedExpressionConcept concept) {
		concepts.add(concept);
	}

	public void addAttribute(ISnomedExpressionAttribute attribute) {
		attributes.add(attribute);
	}

	public void addGroup(ISnomedExpressionGroup group) {
		groups.add(group);
	}
	
	@Override
	public List<ISnomedExpressionConcept> getConcepts() {
		return concepts;
	}
	
	@Override
	public List<ISnomedExpressionAttribute> getAttributes() {
		return attributes;
	}
	
	@Override
	public List<ISnomedExpressionGroup> getGroups() {
		return groups;
	}
	
}
