package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.snomed.SnomedConstants.LanguageCodeReferenceSetIdentifierMapping;
import com.b2international.snowowl.snomed.api.impl.DescriptionService;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;

public class ValidationDescriptionService implements org.ihtsdo.drools.service.DescriptionService {

	private DescriptionService descriptionService;

	public ValidationDescriptionService(DescriptionService descriptionService) {
		this.descriptionService = descriptionService;
	}

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		List<ExtendedLocale> locales = new ArrayList<>();
		for (String languageRefsetId : languageRefsetIds) {
			String languageCode = LanguageCodeReferenceSetIdentifierMapping.getLanguageCode(languageRefsetId);
			locales.add(new ExtendedLocale(languageCode, null, languageRefsetId));
		}
		Map<String, ISnomedDescription> fullySpecifiedNames = descriptionService.getFullySpecifiedNames(conceptIds, locales);
		for (ISnomedDescription description : fullySpecifiedNames.values()) {
			fsns.add(description.getTerm());
		}
		return fsns;
	}

}
