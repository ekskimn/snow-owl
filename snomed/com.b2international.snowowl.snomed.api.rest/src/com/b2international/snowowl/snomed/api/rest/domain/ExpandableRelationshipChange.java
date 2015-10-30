package com.b2international.snowowl.snomed.api.rest.domain;

import com.b2international.snowowl.snomed.api.domain.RelationshipModifier;
import com.b2international.snowowl.snomed.api.domain.classification.ChangeNature;
import com.b2international.snowowl.snomed.api.domain.classification.IRelationshipChange;

public class ExpandableRelationshipChange implements IRelationshipChange {

	private IRelationshipChange change;
	private SnomedConceptMini source;
	private SnomedConceptMini type;
	private SnomedConceptMini destination;

	public ExpandableRelationshipChange(IRelationshipChange wrappedChange) {
		this.change = wrappedChange;
	}
	
	public SnomedConceptMini getSource() {
		return source;
	}

	public void setSource(SnomedConceptMini source) {
		this.source = source;
	}

	public SnomedConceptMini getType() {
		return type;
	}

	public void setType(SnomedConceptMini type) {
		this.type = type;
	}

	public SnomedConceptMini getDestination() {
		return destination;
	}

	public void setDestination(SnomedConceptMini destination) {
		this.destination = destination;
	}

	@Override
	public ChangeNature getChangeNature() {
		return change.getChangeNature();
	}

	@Override
	public String getSourceId() {
		return change.getSourceId();
	}

	@Override
	public String getTypeId() {
		return change.getTypeId();
	}

	@Override
	public String getDestinationId() {
		return change.getDestinationId();
	}

	@Override
	public boolean isDestinationNegated() {
		return change.isDestinationNegated();
	}

	@Override
	public String getCharacteristicTypeId() {
		return change.getCharacteristicTypeId();
	}

	@Override
	public int getGroup() {
		return change.getGroup();
	}

	@Override
	public int getUnionGroup() {
		return change.getUnionGroup();
	}

	@Override
	public RelationshipModifier getModifier() {
		return change.getModifier();
	}

}
