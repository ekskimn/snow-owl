package com.b2international.snowowl.snomed.api.rest.domain;

import java.util.Date;

import com.b2international.snowowl.snomed.api.domain.AssociationType;
import com.b2international.snowowl.snomed.api.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.api.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.api.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.api.domain.SubclassDefinitionStatus;
import com.google.common.collect.Multimap;

public class ExpandableSnomedConcept implements ISnomedConcept {

	private ISnomedConcept concept;
	private String fsn;

	public ExpandableSnomedConcept(ISnomedConcept concept) {
		this.concept = concept;
	}
	
	public String getFsn() {
		return fsn;
	}
	
	public void setFsn(String fsn) {
		this.fsn = fsn;
	}

	@Override
	public boolean isActive() {
		return concept.isActive();
	}

	@Override
	public Date getEffectiveTime() {
		return concept.getEffectiveTime();
	}

	@Override
	public String getModuleId() {
		return concept.getModuleId();
	}

	@Override
	public String getId() {
		return concept.getId();
	}

	@Override
	public boolean isReleased() {
		return concept.isReleased();
	}

	@Override
	public DefinitionStatus getDefinitionStatus() {
		return concept.getDefinitionStatus();
	}

	@Override
	public SubclassDefinitionStatus getSubclassDefinitionStatus() {
		return concept.getSubclassDefinitionStatus();
	}

	@Override
	public InactivationIndicator getInactivationIndicator() {
		return concept.getInactivationIndicator();
	}

	@Override
	public Multimap<AssociationType, String> getAssociationTargets() {
		return concept.getAssociationTargets();
	}

}
