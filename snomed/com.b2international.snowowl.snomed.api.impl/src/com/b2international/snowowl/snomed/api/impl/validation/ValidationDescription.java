package com.b2international.snowowl.snomed.api.impl.validation;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;

public class ValidationDescription implements org.ihtsdo.drools.domain.Description {

	private ISnomedBrowserDescription browserDesciption;

	public ValidationDescription(ISnomedBrowserDescription browserDesciption) {
		this.browserDesciption = browserDesciption;
	}

	@Override
	public String getId() {
		return browserDesciption.getDescriptionId();
	}

	@Override
	public String getConceptId() {
		return browserDesciption.getConceptId();
	}

	@Override
	public String getTerm() {
		return browserDesciption.getTerm();
	}

}
