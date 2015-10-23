package com.b2international.snowowl.snomed.api.rest.browser;

import org.ihtsdo.validation.domain.Relationship;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;

public class RuleRelationshipWrapper implements Relationship {

	private ISnomedBrowserRelationship relationship;

	public RuleRelationshipWrapper(ISnomedBrowserRelationship relationship) {
		this.relationship = relationship;
	}

	@Override
	public String getSourceId() {
		return relationship.getSourceId();
	}

	@Override
	public String getTypeId() {
		return relationship.getType().getConceptId();
	}

	@Override
	public String getDestinationId() {
		return relationship.getTarget().getConceptId();
	}

}
