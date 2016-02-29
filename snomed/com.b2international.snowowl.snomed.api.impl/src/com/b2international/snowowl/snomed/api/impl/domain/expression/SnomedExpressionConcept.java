package com.b2international.snowowl.snomed.api.impl.domain.expression;

import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionAttributeValue;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpressionConcept;
import com.b2international.snowowl.snomed.api.impl.domain.ISnomedConceptMin;

public class SnomedExpressionConcept implements ISnomedExpressionConcept, ISnomedExpressionAttributeValue, ISnomedConceptMin {

	private final String id;
	private final boolean primitive;
	private String term;

	public SnomedExpressionConcept(String id, boolean primitive) {
		this.id = id;
		this.primitive = primitive;
	}

	public void setTerm(String term) {
		this.term = term;
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
	public boolean isPrimitive() {
		return primitive;
	}

}
