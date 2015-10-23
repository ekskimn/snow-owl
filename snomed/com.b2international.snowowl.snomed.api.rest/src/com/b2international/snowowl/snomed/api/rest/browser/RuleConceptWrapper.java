package com.b2international.snowowl.snomed.api.rest.browser;

import java.util.Collection;
import java.util.List;

import org.ihtsdo.validation.domain.Concept;
import org.ihtsdo.validation.domain.Description;
import org.ihtsdo.validation.domain.Relationship;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class RuleConceptWrapper implements Concept {

	private SnomedBrowserConcept concept;
	private List<Description> descriptions;
	private List<Relationship> relationships;

	public RuleConceptWrapper(SnomedBrowserConcept concept) {
		this.concept = concept;
		this.descriptions = Lists.transform(concept.getDescriptions(), new Function<ISnomedBrowserDescription, Description>() {
			@Override
			public Description apply(ISnomedBrowserDescription input) {
				return new RuleDescriptionWrapper(input);
			}});
		this.relationships = Lists.transform(concept.getRelationships(), new Function<ISnomedBrowserRelationship, Relationship>() {
			@Override
			public Relationship apply(ISnomedBrowserRelationship input) {
				return new RuleRelationshipWrapper(input);
			}});
	}

	@Override
	public String getId() {
		return concept.getConceptId();
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
