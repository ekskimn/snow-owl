package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.ihtsdo.drools.service.DescriptionService;

import com.b2international.snowowl.api.domain.IComponentRef;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.snomed.SnomedConstants.LanguageCodeReferenceSetIdentifierMapping;
import com.b2international.snowowl.snomed.api.ISnomedDescriptionService;
import com.b2international.snowowl.snomed.api.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.api.impl.SnomedServiceHelper;

public class ValidationDescriptionService implements DescriptionService {

	private ISnomedDescriptionService descriptionService;
	private IBranchPath branchPath = null;

	public ValidationDescriptionService(IBranchPath branchPath, ISnomedDescriptionService descriptionService) {
		this.branchPath = branchPath;
		this.descriptionService = descriptionService;
	}

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		List<Locale> locales = new ArrayList<>();
		for (String languageRefsetId : languageRefsetIds) {
			String languageCode = LanguageCodeReferenceSetIdentifierMapping.getLanguageCode(languageRefsetId);
			locales.add(new Locale(languageCode));
		}
		for (String conceptId : conceptIds) {
			IComponentRef conceptRef = SnomedServiceHelper.createComponentRef(branchPath.getPath(), conceptId);
			ISnomedDescription fullySpecifiedName = descriptionService.getFullySpecifiedName(conceptRef, locales);
			fsns.add(fullySpecifiedName.getTerm());
		}
		return fsns;
	}

}
