package com.b2international.snowowl.snomed.api;

import java.util.List;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;

public interface ISnomedExpressionService {

	ISnomedExpression getConceptShortNormalForm(String conceptId, String branchPath, List<ExtendedLocale> extendedLocales, boolean normaliseAttributeValues);

}
