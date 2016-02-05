package com.b2international.snowowl.snomed.api.domain.expression;

import java.util.List;

public interface ISnomedExpression extends ISnomedExpressionAttributeValue {

	List<ISnomedExpressionConcept> getConcepts();

	List<ISnomedExpressionAttribute> getAttributes();

	List<ISnomedExpressionGroup> getGroups();

}
