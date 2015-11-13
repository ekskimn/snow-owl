package com.b2international.snowowl.snomed.api.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;

public interface ISnomedBrowserValidationService {

	List<ISnomedInvalidContent> validateConcept(String branchPath, ISnomedBrowserConcept browserConcept, ArrayList<Locale> locales);

}
