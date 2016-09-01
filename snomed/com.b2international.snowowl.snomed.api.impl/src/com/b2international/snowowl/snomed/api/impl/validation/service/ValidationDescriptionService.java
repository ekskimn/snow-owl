package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.LanguageCodeReferenceSetIdentifierMapping;
import com.b2international.snowowl.snomed.api.impl.DescriptionService;
import com.b2international.snowowl.snomed.api.impl.validation.domain.ValidationSnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedConcept;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;

public class ValidationDescriptionService implements org.ihtsdo.drools.service.DescriptionService {

	private DescriptionService descriptionService;
	private String branchPath;
	private IEventBus bus;

	public ValidationDescriptionService(DescriptionService descriptionService, String branchPath, IEventBus bus) {
		this.descriptionService = descriptionService;
		this.branchPath = branchPath;
		this.bus = bus;
	}

	final static Logger logger = LoggerFactory.getLogger(ValidationDescriptionService.class);

	// Static block of sample case significant words - lower case word -> exact
	// word as supplied
	// NOTE: This will not handle cases where the same word exists with
	// different capitalizations
	// However, at this time no further precision is required
	public static final Map<String, String> caseSignificantWordsMap = new HashMap<>();
	static {

		String fileName = "/opt/termserver/resources/test-resources/cs_words.txt";

		File file = new File(fileName);
		FileReader fileReader;
		BufferedReader bufferedReader;
		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			// skip header line
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null) {
				String[] words = line.split("\\s+");

				// format: 0: word, 1: type (unused)
				caseSignificantWordsMap.put(words[0].toLowerCase(), words[0]);
			}
			fileReader.close();
			logger.info(
					"Loaded " + caseSignificantWordsMap.size() + " case sensitive words into cache from: " + fileName);
		} catch (IOException e) {
			logger.debug("Failed to retrieve case sensitive words file: " + fileName);

		}

	}

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Map<String, Set<String>> refsetToLanguageSpecificWordsMap = new HashMap<>();
	static {
		loadRefsetSpecificWords(Constants.GB_EN_LANG_REFSET, "/opt/termserver/resources/test-resources/gbTerms.txt");
		loadRefsetSpecificWords(Constants.US_EN_LANG_REFSET, "/opt/termserver/resources/test-resources/usTerms.txt");

	}

	private static void loadRefsetSpecificWords(String refsetId, String fileName) {

		Set<String> words = new HashSet<>();

		File file = new File(fileName);
		FileReader fileReader;
		BufferedReader bufferedReader;
		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			String line;
			// skip header line
			bufferedReader.readLine();
			while ((line = bufferedReader.readLine()) != null) {
				words.add(line.toLowerCase()); // assumed to be single-word
												// lines
			}
			fileReader.close();
			refsetToLanguageSpecificWordsMap.put(refsetId, words);
			logger.info("Loaded " + words.size() + " language-specific spellings into cache for refset " + refsetId
					+ " from: " + fileName);

		} catch (IOException e) {
			logger.info("Failed to retrieve language-specific terms for refset " + refsetId + " in file " + fileName);
		}
	}

	// On initial load, retrieve the top-level hierarchy roots for hierarchy
	// detection
	private static Set<String> hierarchyRootIds = null;

	@Override
	public Set<String> getFSNs(Set<String> conceptIds, String... languageRefsetIds) {
		Set<String> fsns = new HashSet<>();
		List<ExtendedLocale> locales = new ArrayList<>();
		for (String languageRefsetId : languageRefsetIds) {
			String languageCode = LanguageCodeReferenceSetIdentifierMapping.getLanguageCode(languageRefsetId);
			locales.add(new ExtendedLocale(languageCode, null, languageRefsetId));
		}
		Map<String, ISnomedDescription> fullySpecifiedNames = descriptionService.getFullySpecifiedNames(conceptIds,
				locales);
		for (ISnomedDescription description : fullySpecifiedNames.values()) {
			fsns.add(description.getTerm());
		}
		return fsns;
	}

	@Override
	public Set<Description> findActiveDescriptionByExactTerm(String exactTerm) {
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().filterByActive(true)
				.filterByTerm(exactTerm).build(branchPath).executeSync(bus);

		Set<Description> matches = new HashSet<>();
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(exactTerm)) {
				matches.add(new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
			}
		}
		return matches;
	}

	@Override
	public Set<Description> findInactiveDescriptionByExactTerm(String exactTerm) {
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().filterByActive(false)
				.filterByTerm(exactTerm).build(branchPath).executeSync(bus);

		Set<Description> matches = new HashSet<>();
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(exactTerm)) {
				matches.add(new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
			}
		}
		return matches;
	}

	private void cacheHierarchyRootIds() {
		
		hierarchyRootIds = new HashSet<>();

		System.out.println("Caching hierarchy root ids");
		final SnomedConcepts rootConcepts = SnomedRequests.prepareSearchConcept()
				.setComponentIds(Arrays.asList("138875005")).setExpand("descendants(direct:true)").build(branchPath)
				.executeSync(bus);
		System.out.println(" Retrieved SNOMEDCT root concept");
		for (ISnomedConcept rootConcept : rootConcepts) {
			for (ISnomedConcept childConcept : rootConcept.getDescendants()) {
				System.out.println(" Adding hierarchy root id " + childConcept.getId());
				hierarchyRootIds.add(childConcept.getId());
			}
		}

		System.out.println("hierarchyRootIds (" + hierarchyRootIds.size() + "): " + hierarchyRootIds);

	}

	private String getHierarchyId(ISnomedConcept concept) {
		for (ISnomedConcept ancestor : concept.getAncestors()) {
			if (hierarchyRootIds.contains(ancestor.getId())) {
				return ancestor.getId();
			}
		}
		return null;
	}

	@Override
	// TODO Remove the semantic tag argument (requires engine change)
	public boolean isActiveDescriptionUniqueWithinHierarchy(Description description, String semanticTag) {

		System.out.println("Checking unique within hierarchy for " + description.getId() + "/"
				+ description.getConceptId() + "/" + description.getTerm() + "/" + description.getLanguageCode());

		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().filterByActive(true)
				.filterByTerm(description.getTerm()).build(branchPath).executeSync(bus);

		Set<String> conceptIds = new HashSet<>();

		// check against id, exact term, and language code
		for (ISnomedDescription iSnomedDescription : descriptions) {
			System.out.println("Checking against description " + iSnomedDescription.getId() + "/"
					+ iSnomedDescription.getConceptId() + "/" + iSnomedDescription.getTerm() + "/"
					+ iSnomedDescription.getLanguageCode());
			if (!iSnomedDescription.getId().equals(description.getId())
					&& iSnomedDescription.getTerm().equals(description.getTerm())
					&& iSnomedDescription.getLanguageCode().equals(description.getLanguageCode())) {
				System.out.println("  Match found!");
				conceptIds.add(iSnomedDescription.getConceptId());
			}
		}

		// if matches found
		if (conceptIds.size() > 0) {

			System.out.println("Matches found " + conceptIds);

			// if hierarchy root ids not already cached, get them
			if (hierarchyRootIds == null) {
				cacheHierarchyRootIds();
			}

			// get the concept (with ancestors)
			ISnomedConcept concept = SnomedRequests.prepareGetConcept()
					.setComponentId(description.getConceptId())
					.setExpand("ancestors(direct:false)")
					.build(branchPath)
					.executeSync(bus);
			
			System.out.println("  Concept hierarchy id:  " + getHierarchyId(concept));
			// retrieve matching concepts (with ancestors) and compare
			for (String conceptId : conceptIds) {
				ISnomedConcept matchingConcept = SnomedRequests.prepareGetConcept()
						.setComponentId(conceptId)
						.setExpand("ancestors(direct:false)")
						.build(branchPath)
						.executeSync(bus);
				System.out.println("  Matching hierarchy id: " + getHierarchyId(matchingConcept));
				
				if (getHierarchyId(concept).equals(getHierarchyId(matchingConcept))) {
					return false;
				}
			}
		}

		return true;

	}

	@Override
	public String getLanguageSpecificErrorMessage(Description description) {

		String errorMessage = "";

		// null checks
		if (description == null || description.getAcceptabilityMap() == null || description.getTerm() == null) {
			return errorMessage;
		}

		String[] words = description.getTerm().split("\\s+");

		// convenience variables
		String usAcc = description.getAcceptabilityMap().get(Constants.US_EN_LANG_REFSET);
		String gbAcc = description.getAcceptabilityMap().get(Constants.GB_EN_LANG_REFSET);

		// NOTE: Supports international only at this point
		// Only check active synonyms
		if (description.isActive() && Constants.SYNONYM.equals(description.getTypeId())) {
			for (String word : words) {

				// Step 1: Check en-us preferred synonyms for en-gb spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(usAcc) && refsetToLanguageSpecificWordsMap
						.get(Constants.GB_EN_LANG_REFSET).contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-us refset but refers to a word that has en-gb spelling: "
							+ word + "\n";
				}

				// Step 2: Check en-gb preferred synonyms for en-en spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(gbAcc) && refsetToLanguageSpecificWordsMap
						.get(Constants.US_EN_LANG_REFSET).contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-gb refset but refers to a word that has en-us spelling: "
							+ word + "\n";
				}
			}
		}

		return errorMessage;

	}

	@Override
	public String getCaseSensitiveWordsErrorMessage(Description description) {
		String result = "";

		// return immediately if description null
		if (description == null) {
			return result;
		}

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			if (caseSignificantWordsMap.containsKey(word.toLowerCase())
					&& !Constants.ENTIRE_TERM_CASE_SENSITIVE.equals(description.getCaseSignificanceId())) {
				result += "Description contains case-sensitive words but is not marked case sensitive: "
						+ caseSignificantWordsMap.get(word.toLowerCase()) + ".\n";

			}
		}
		return result;
	}

}
