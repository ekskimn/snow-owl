package com.b2international.snowowl.snomed.api.domain.expression;

public interface ISnomedExpressionConcept extends ISnomedExpressionAttributeValue {

	String getId();

	String getTerm();

	boolean isPrimative();

}
