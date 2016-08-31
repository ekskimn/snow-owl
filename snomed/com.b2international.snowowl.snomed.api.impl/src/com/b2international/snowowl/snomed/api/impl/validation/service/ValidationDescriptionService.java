package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
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

	// Static block of sample case significant words
		// In non-dev environments, this should initialize on startup
		public static final Map<String, String> caseSignificantWordsMap = new HashMap<>();
		static {

			File file = new File("src/test/resources/data/CSWordsSample.txt");
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
				logger.info("Loaded " + caseSignificantWordsMap.size() + " case sensitive words into cache");
			} catch (IOException e) {
				logger.debug("Failed to retrieve case significant words file -- tests will be skipped");

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
			logger.info("Loaded " + words.size() + " language-specific spellings into cache for refset " + refsetId);

		} catch (IOException e) {
			logger.info("Failed to retrieve language-specific terms for refset " + refsetId + " in file " + fileName);
		}
	}

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

	@Override
	public boolean isActiveDescriptionUniqueWithinHierarchy(Description description, String semanticTag) {

		/*
		 * TODO Need to figure out the locale setting
		 * final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().filterByActive(true)
				.filterByTerm(description.getTerm()).build(branchPath).executeSync(bus);

		Set<String> conceptIds = new HashSet<>();

		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(description.getTerm())
					&& iSnomedDescription.getLanguageCode().equals(description.getLanguageCode())) {
				conceptIds.add(iSnomedDescription.getConceptId());
			}
		}

		SnomedConcepts concepts = SnomedRequests.prepareSearchConcept().setComponentIds(conceptIds).setExpand("fsn()")
				.build(branchPath).executeSync(bus);

		for (ISnomedConcept concept : concepts) {
			if (concept.getFsn().getTerm().endsWith(semanticTag)) {
				return false;
			}
		}*/

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

		System.out
				.println("Checking description " + description.getTerm() + ", " + description.getCaseSignificanceId());

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			System.out.println("  Checking word " + word);

			if (caseSignificantWordsMap.containsKey(word.toLowerCase())) {

				// Check 1: term containing case-sensitive words should not be
				// entire-term-case-insensitive
				if (Constants.ENTIRE_TERM_CASE_INSENSITIVE.equals(description.getCaseSignificanceId())) {
					result += "Description marked case insensitive should not contain case-sensitive word: "
							+ caseSignificantWordsMap.get(word.toLowerCase()) + ".\n";
				}

				// Check 2: term marked ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE
				// should not start with case sensitive word
				// TODO Confirm this
				else if (Constants.ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE.equals(description.getCaseSignificanceId())
						&& description.getTerm().startsWith(word)) {
					result += "Description marked only initial character case insensitive should not start with a case-sensitive word: " + word + ".\n";
				}
				// Check 3: term containing case-sensitive word with invalid
				// case
				else if (caseSignificantWordsMap.containsKey(word.toLowerCase())
						&& !caseSignificantWordsMap.get(word.toLowerCase()).equals(word)) {
					result += "Description contains case-sensitive word with improper case: " + word + " should be "
							+ caseSignificantWordsMap.get(word.toLowerCase()) + ".\n";
				}
			}
		}
		if (result.length() > 0) {
			System.out.println(result);
		}
		return result;
	}

}
