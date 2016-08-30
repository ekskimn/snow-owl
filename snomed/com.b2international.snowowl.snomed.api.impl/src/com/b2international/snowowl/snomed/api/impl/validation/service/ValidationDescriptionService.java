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

import org.ihtsdo.drools.domain.Concept;
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
	
	final static Logger logger = LoggerFactory.getLogger(ValidationDescriptionService.class);

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Set<String> caseSignificantWordsOriginal = new HashSet<>();
	public static final Set<String> caseSignificantWordsLowerCase = new HashSet<>();
	static {
		
		File fileTest = new File(".");
		logger.info("Validation Description Service root directory " + fileTest.getAbsolutePath());

		File file = new File("/opt/termserver/resources/test-resources/cs_words.txt");
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
				caseSignificantWordsOriginal.add(words[0]);
				caseSignificantWordsLowerCase.add(words[0].toLowerCase());
			}
			fileReader.close();
			logger.info("Loaded " + caseSignificantWordsOriginal.size() + " case sensitive words into cache");
		} catch (IOException e) {
			logger.info("Failed to retrieve case significant words file -- test will be skipped");

		}

	}

	// Static block of sample case significant words
	// In non-dev environments, this should initialize on startup
	public static final Map<String, Set<String>> refsetToLanguageSpecificWordsMap = new HashMap<>();
	static {
		loadRefsetSpecificWords(Constants.GB_EN_LANG_REFSET, "src/test/resources/data/gbTerms.txt");
		loadRefsetSpecificWords(Constants.US_EN_LANG_REFSET, "src/test/resources/data/usTerms.txt");

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
				words.add(line); // assumed to be single-word lines
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
	
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription()
				.filterByActive(true)
				.filterByTerm(description.getTerm())
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

	@Override
	public String getLanguageSpecificErrorMessage(Description description) {
		String[] words = description.getTerm().split("\\s+");
		for (String refsetId : description.getAcceptabilityMap().keySet()) {
			for (String word : words) {
				if (refsetToLanguageSpecificWordsMap.get(refsetId).contains(word)) {
					return "Synonym is prefered in the GB Language refset but refers to a word has en-us spelling: "
							+ word;
				}
			}
		}
		return null;
	}

	@Override
	public boolean hasCaseSignificantWord(String term) {
		String[] words = term.split("\\s+");
		for (String word : words) {
			// if lower case match and not original word match
			if (caseSignificantWordsLowerCase.contains(word.toLowerCase())
					&& !caseSignificantWordsOriginal.contains(word)) {
				return true;
			}
		}
		return false;
	}

}
