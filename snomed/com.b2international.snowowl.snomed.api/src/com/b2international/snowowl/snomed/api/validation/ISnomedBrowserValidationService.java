package com.b2international.snowowl.snomed.api.validation;

import java.util.List;
import java.util.Map;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;

public interface ISnomedBrowserValidationService {

	List<ISnomedInvalidContent> validateConcept(String branchPath, ISnomedBrowserConcept browserConcept, List<ExtendedLocale> locales);

	Map<String, String> getDialectMatches(List<String> tokenizedWords);
	
	int reloadRules();

}
