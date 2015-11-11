package com.b2international.snowowl.snomed.api.impl.validation;

import org.ihtsdo.drools.InvalidContent;

import com.b2international.snowowl.snomed.api.validation.ISnomedInvalidContent;

public class SnomedInvalidContent implements ISnomedInvalidContent {

	private InvalidContent invalidContent;

	public SnomedInvalidContent(InvalidContent invalidContent) {
		this.invalidContent = invalidContent;
	}
	
	@Override
	public String getConceptId() {
		return invalidContent.getComponentId();
	}

	@Override
	public String getComponentId() {
		return invalidContent.getComponentId();
	}

	@Override
	public String getMessage() {
		return invalidContent.getMessage();
	}

}
