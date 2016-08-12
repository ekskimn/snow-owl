package com.b2international.snowowl.snomed.api.impl.validation.domain;

import java.util.HashMap;
import java.util.Map;

import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;

/**
 * Wrapper for the ISnomedDescription class 
 */
public class ValidationSnomedDescription implements org.ihtsdo.drools.domain.Description {

	private ISnomedDescription description;
	private String conceptId;

	public ValidationSnomedDescription(ISnomedDescription description, String conceptId) {
		this.description = description;
		this.conceptId = conceptId;
	}

	@Override
	public String getId() {
		return description.getId();
	}

	@Override
	public boolean isActive() {
		return description.isActive();
	}

	@Override
	public boolean isPublished() {
		return description.getEffectiveTime() != null;
	}

	@Override
	public String getLanguageCode() {
		return description.getLanguageCode();
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getTypeId() {
		return description.getTypeId();
	}

	@Override
	public String getCaseSignificanceId() {
		return description.getCaseSignificance().getConceptId();
	}
	
	@Override
	public String getTerm() {
		return description.getTerm();
	}

	@Override
	public boolean isTextDefinition() {
		return SnomedBrowserDescriptionType.TEXT_DEFINITION.getConceptId().equals(description.getTypeId());
	}
	
	@Override
	public Map<String, String> getAcceptabilityMap() {
		Map<String, String> langRefsetIdToAcceptabliltyIdMap = new HashMap<>();
		Map<String, Acceptability> acceptabilityMap = description.getAcceptabilityMap();
		for (String langRefsetId : acceptabilityMap.keySet()) {
			langRefsetIdToAcceptabliltyIdMap.put(langRefsetId, acceptabilityMap.get(langRefsetId).getConceptId());
		}
		return langRefsetIdToAcceptabliltyIdMap;
	}

}
