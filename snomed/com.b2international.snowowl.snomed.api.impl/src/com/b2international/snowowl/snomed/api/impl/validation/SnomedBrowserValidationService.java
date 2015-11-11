package com.b2international.snowowl.snomed.api.impl.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ihtsdo.drools.InvalidContent;
import org.ihtsdo.drools.RuleExecutor;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.validation.ISnomedBrowserValidationService;
import com.b2international.snowowl.snomed.api.validation.ISnomedInvalidContent;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class SnomedBrowserValidationService implements ISnomedBrowserValidationService {

	private RuleExecutor ruleExecutor;
	
	public SnomedBrowserValidationService() {
		ruleExecutor = new RuleExecutor("/opt/termserver/snomed-drools-rules");
	}

	@Override
	public List<ISnomedInvalidContent> validateConcept(String branchPath, ISnomedBrowserConcept browserConcept, ArrayList<Locale> locales) {
		List<InvalidContent> list = ruleExecutor.execute(new ValidationConcept(browserConcept));
		List<ISnomedInvalidContent> invalidContent = Lists.transform(list, new Function<InvalidContent, ISnomedInvalidContent>() {
			@Override
			public ISnomedInvalidContent apply(InvalidContent input) {
				return new SnomedInvalidContent(input);
			}
		});
		return invalidContent;
	}
	
	public static void main(String[] args) {
		new SnomedBrowserValidationService();
	}

}
