package com.b2international.snowowl.snomed.api.rest.browser;

import org.ihtsdo.validation.domain.Description;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;

public class RuleDescriptionWrapper implements Description {

	private ISnomedBrowserDescription description;

	public RuleDescriptionWrapper(ISnomedBrowserDescription description) {
		this.description = description;
	}

	@Override
	public String getConceptId() {
		return description.getConceptId();
	}

	@Override
	public String getTerm() {
		return description.getTerm();
	}

}
