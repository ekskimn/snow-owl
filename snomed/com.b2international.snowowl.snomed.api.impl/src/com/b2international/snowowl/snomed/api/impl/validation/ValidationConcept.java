package com.b2international.snowowl.snomed.api.impl.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;

public class ValidationConcept implements org.ihtsdo.drools.domain.Concept {

	private ISnomedBrowserConcept browserConcept;
	private List<Description> descriptions;
	private List<Relationship> relationships;

	public ValidationConcept(ISnomedBrowserConcept browserConcept) {
		this.browserConcept = browserConcept;
		descriptions = new ArrayList<>();
		for (ISnomedBrowserDescription browserDescription : browserConcept.getDescriptions()) {
			descriptions.add(new ValidationDescription(browserDescription));
		}
		relationships = new ArrayList<>();
	}
	
	@Override
	public String getId() {
		return browserConcept.getConceptId();
	}
	
	@Override
	public boolean isActive() {
		return browserConcept.isActive();
	}
	
	@Override
	public boolean isPublished() {
		return browserConcept.getEffectiveTime() != null;
	}

	@Override
	public Collection<Description> getDescriptions() {
		return descriptions;
	}

	@Override
	public Collection<Relationship> getRelationships() {
		return relationships;
	}

}
