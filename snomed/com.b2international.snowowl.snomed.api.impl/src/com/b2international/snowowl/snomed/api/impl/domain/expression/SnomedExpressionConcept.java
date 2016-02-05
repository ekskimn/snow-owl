package com.b2international.snowowl.snomed.api.impl.domain.expression;

import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttributeValue;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionConcept;

public class SnomedExpressionConcept implements ISnomedExpressionConcept, ISnomedExpressionAttributeValue {

	private final String id;
	private final String term;
	private boolean primative;

	public SnomedExpressionConcept(String id, String term, boolean primative) {
		this.id = id;
		this.term = term;
		this.primative = primative;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getTerm() {
		return term;
	}
	
	@Override
	public boolean isPrimative() {
		return primative;
	}

}
