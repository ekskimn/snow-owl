package com.b2international.snowowl.snomed.api.impl.domain.expression;

import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttribute;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttributeValue;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionConcept;

public class SnomedExpressionAttribute implements ISnomedExpressionAttribute {

	private final ISnomedExpressionConcept type;
	private final ISnomedExpressionAttributeValue value;

	public SnomedExpressionAttribute(ISnomedExpressionConcept type, ISnomedExpressionAttributeValue value) {
		this.type = type;
		this.value = value;
	}

	
	@Override
	public ISnomedExpressionConcept getType() {
		return type;
	}

	@Override
	public ISnomedExpressionAttributeValue getValue() {
		return value;
	}

}
