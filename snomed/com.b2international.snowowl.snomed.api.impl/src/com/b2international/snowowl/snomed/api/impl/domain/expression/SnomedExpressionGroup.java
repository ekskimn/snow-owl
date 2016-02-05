package com.b2international.snowowl.snomed.api.impl.domain.expression;

import java.util.ArrayList;
import java.util.List;

import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionGroup;

public class SnomedExpressionGroup implements ISnomedExpressionGroup {

	private List<ISnomedExpressionAttribute> attributes;

	public SnomedExpressionGroup() {
		attributes = new ArrayList<>();
	}

	public void addAttribute(ISnomedExpressionAttribute attribute) {
		attributes.add(attribute);
	}
	
	@Override
	public List<ISnomedExpressionAttribute> getAttributes() {
		return attributes;
	}

}
