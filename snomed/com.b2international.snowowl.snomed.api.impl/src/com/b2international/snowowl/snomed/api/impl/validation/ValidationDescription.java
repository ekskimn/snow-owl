package com.b2international.snowowl.snomed.api.impl.validation;

import java.util.HashMap;
import java.util.Map;

import com.b2international.snowowl.snomed.api.domain.Acceptability;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserDescription;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;

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
	public boolean isActive() {
		return browserDesciption.isActive();
	}

	@Override
	public boolean isPublished() {
		return browserDesciption.getEffectiveTime() != null;
	}

	@Override
	public String getConceptId() {
		return browserDesciption.getConceptId();
	}

	@Override
	public String getTypeId() {
		return browserDesciption.getType().getConceptId();
	}

	@Override
	public String getCaseSignificanceId() {
		return browserDesciption.getCaseSignificance().getConceptId();
	}
	
	@Override
	public String getTerm() {
		return browserDesciption.getTerm();
	}

	@Override
	public boolean isTextDefinition() {
		return browserDesciption.getType() == SnomedBrowserDescriptionType.TEXT_DEFINITION;
	}
	
	@Override
	public Map<String, String> getAcceptabilityMap() {
		Map<String, String> langRefsetIdToAcceptabliltyIdMap = new HashMap<>();
		Map<String, Acceptability> acceptabilityMap = browserDesciption.getAcceptabilityMap();
		for (String langRefsetId : acceptabilityMap.keySet()) {
			langRefsetIdToAcceptabliltyIdMap.put(langRefsetId, acceptabilityMap.get(langRefsetId).getConceptId());
		}
		return langRefsetIdToAcceptabliltyIdMap;
	}

}
