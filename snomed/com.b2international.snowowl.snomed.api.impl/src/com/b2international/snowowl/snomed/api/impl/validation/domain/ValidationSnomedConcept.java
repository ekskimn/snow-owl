package com.b2international.snowowl.snomed.api.impl.validation.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;

public class ValidationSnomedConcept implements org.ihtsdo.drools.domain.Concept{

	private SnomedConcept concept;
	
	public ValidationSnomedConcept(SnomedConcept concept) {
		this.concept = concept;
	}
	
	@Override
	public String getId() {
		return concept.getId();
	}

	@Override
	public String getModuleId() {
		return concept.getModuleId();
	}

	@Override
	public boolean isActive() {
		return concept.isActive();
	}

	@Override
	public boolean isPublished() {
		return concept.getEffectiveTime() != null;
	}

	@Override
	public boolean isReleased() {
		return concept.isReleased();
	}

	@Override
	public String getDefinitionStatusId() {
		return concept.getDefinitionStatus().getConceptId();
	}

	@Override
	public Collection<? extends Description> getDescriptions() {
		Set<Description> descriptions = new HashSet<>();
		for (SnomedDescription iSnomedDescription : concept.getDescriptions()) {
			descriptions.add(new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
		}
		return descriptions;
	}

	@Override
	public Collection<? extends Relationship> getRelationships() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((concept == null) ? 0 : concept.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidationSnomedConcept other = (ValidationSnomedConcept) obj;
		if (concept == null) {
			if (other.concept != null)
				return false;
		} else if (!concept.getIconId().equals(other.concept.getId()))
			return false;
		return true;
	}

	
}
