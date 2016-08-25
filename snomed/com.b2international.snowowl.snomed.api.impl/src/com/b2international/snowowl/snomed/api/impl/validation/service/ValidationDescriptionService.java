package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.SnomedConstants.LanguageCodeReferenceSetIdentifierMapping;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.impl.DescriptionService;
import com.b2international.snowowl.snomed.api.impl.validation.domain.ValidationConcept;
import com.b2international.snowowl.snomed.api.impl.validation.domain.ValidationSnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;

public class ValidationDescriptionService implements org.ihtsdo.drools.service.DescriptionService {

	private DescriptionService descriptionService;
	private String branchPath;
	private IEventBus bus;

	public ValidationDescriptionService(DescriptionService descriptionService, String branchPath, IEventBus bus) {
		this.descriptionService = descriptionService;
		this.branchPath = branchPath;
		this.bus = bus;
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

	@Override
	public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription()
				.filterByActive(true)
				.filterByTerm(exactTerm)
				.build(branchPath)
				.executeSync(bus);

		Set<Description> matches = new HashSet<>();
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(exactTerm)) {
				matches.add(new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
				iSnomedDescription.getConceptId()
			}
		}

		return matches;
	}
	
	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription()
				.filterByActive(false)
				.filterByTerm(exactTerm)
				.build(branchPath)
				.executeSync(bus);

		Set<Concept> matches = new HashSet<>();
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(exactTerm)) {
				matches.add(new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
			}
		}
		return matches;
	}


	@Override
	public boolean isActiveDescriptionUniqueWithinHierarchy(Description description, String semanticTag, ) {
		Set<Concept> matches = new HashSet<>();
		// TODO This doesn't work due to lack of constructor for ValidationConcept(
		 // 1 - findActiveDescriptionsByExactTerm -- or use descriptionService
		//  2 - retrieve concepts
		//  3 - return true/false based on fsn tag
		
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription()
				.filterByActive(isActive)
				.filterByTerm(exactTerm)
				.build(branchPath)
				.executeSync(bus);

		Set<String> conceptIds = new HashSet<>();
		
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(description.getTerm()) && iSnomedDescription.getLanguageCode().equals(description.getLanguageCode())) {
				conceptIds.add(iSnomedDescription.getConceptId());
			}
		}
		
		SnomedConcepts concepts = SnomedRequests
			     .prepareSearchConcept()
			     .setComponentIds(conceptIds)
			     .setExpand("fsn()")
			     .build(branchPath)
			     .executeSync(bus);
		
		for (ISnomedConcept concept : concepts) {
			if (concept.getFsn().getTerm().endsWith(semanticTag)) {
				return false;
			}
		}
		
		
		return true;
		
		
	}

}
