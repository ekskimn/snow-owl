package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Constants;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
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
	public static final Set<String> caseSignificantWords = new HashSet<>();
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
				caseSignificantWords.add(words[0]);
			}
			fileReader.close();
			logger.info("Loaded " + caseSignificantWords.size() + " case sensitive words into cache from: " + fileName);
		} catch (IOException e) {
			logger.info("Failed to retrieve case sensitive words file: " + fileName);

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
			logger.info("Loaded " + words.size() + " language-specific spellings into cache for refset " + refsetId
					+ " from: " + fileName);

		} catch (IOException e) {
			logger.info("Failed to retrieve language-specific terms for refset " + refsetId + " in file " + fileName);
		} finally {
			refsetToLanguageSpecificWordsMap.put(refsetId, words);
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

	private void cacheHierarchyRootConcepts() {

		hierarchyRootIds = new HashSet<>();

		final SnomedConcepts rootConcepts = SnomedRequests.prepareSearchConcept()
				.setComponentIds(Arrays.asList("138875005")).setExpand("descendants(direct:true)").build(branchPath)
				.executeSync(bus);
		for (ISnomedConcept rootConcept : rootConcepts) {
			for (ISnomedConcept childConcept : rootConcept.getDescendants()) {
				hierarchyRootIds.add(childConcept.getId());
			}
		}
	}

	private String getHierarchyIdForConcept(ISnomedConcept concept) {

		// if concept null, return null
		if (concept == null) {
			return null;
		}

		// if not yet retrieved, cache the root concepts
		if (hierarchyRootIds == null) {
			cacheHierarchyRootConcepts();
		}

		// check if this concept is a root
		for (String rootId : hierarchyRootIds) {
			if (rootId.equals(concept.getId())) {
				return rootId;
			}
		}

		// otherwise check ancestors
		for (ISnomedConcept ancestor : concept.getAncestors()) {
			if (hierarchyRootIds.contains(ancestor.getId())) {
				return ancestor.getId().toString();
			}
		}
		return null;
	}

	@Override
	public Set<Description> findMatchingDescriptionInHierarchy(Concept concept, Description description) {

		// on first invocation, cache the hierarchy root ids
		if (hierarchyRootIds == null) {
			cacheHierarchyRootConcepts();
		}

		// the return set
		Set<Description> matchesInHierarchy = new HashSet<>();

		// retrieve partially-matching descriptions
		final SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().filterByActive(true)
				.filterByTerm(description.getTerm()).filterByLanguageCodes(Arrays.asList(description.getLanguageCode()))
				.build(branchPath).executeSync(bus);

		System.out.println("Partial matches found in SNOMED " + descriptions.getTotal());

		// filter by exact match
		Set<ISnomedDescription> matchesInSnomed = new HashSet<>();
		for (ISnomedDescription iSnomedDescription : descriptions) {
			if (iSnomedDescription.getTerm().equals(description.getTerm())) {
				matchesInSnomed.add(iSnomedDescription);
			}
		}

		// if matches found
		if (matchesInSnomed.size() > 0) {

			System.out.println(" Exact matches found in SNOMED " + matchesInSnomed.size());

			// the concept id used to determine the hierarchy
			String lookupId = null;

			// if this is a root id, use it as the lookup id
			if (hierarchyRootIds.contains(concept.getId())) {
				lookupId = concept.getId();
			}

			// otherwise, use the first active parent as lookup id
			else {
				Iterator<? extends Relationship> iter = concept.getRelationships().iterator();
				while (iter.hasNext()) {
					Relationship rel = iter.next();
					if (rel.isActive() && Constants.IS_A.equals(rel.getTypeId())) {
						lookupId = rel.getDestinationId();
					}
				}
			}

			System.out.println(" lookup id: " + lookupId);

			// if id could not be retrieved, cannot determine hierarchy
			// either SNOMED CT root concept, or has no active parents
			if (lookupId == null) {
				return matchesInHierarchy;
			}

			// use id to retrieve concept for hierarchy detection
			ISnomedConcept hierarchyConcept = null;
			// try {
			hierarchyConcept = SnomedRequests.prepareSearchConcept()
					.filterByComponentIds(Collections.singleton(lookupId)).setLimit(1000)
					.setExpand(String.format("ancestors(direct:%s,offset:%d,limit:%d)", "true", 0, 1000))
					.build(branchPath).executeSync(bus).getItems().get(0);
			// } catch (Exception e) {
			// System.out.println(" Could not retrieve ancestors for lookup id "
			// + lookupId);
			// }

			System.out.println("  lookup hierarchy: " + getHierarchyIdForConcept(hierarchyConcept));

			// back out if cannot determine hierarchy
			if (hierarchyConcept == null || getHierarchyIdForConcept(hierarchyConcept) == null) {
				return matchesInHierarchy;
			}

			// retrieve matching concepts (with ancestors) and compare
			for (ISnomedDescription iSnomedDescription : matchesInSnomed) {

				System.out.println("   Searching using prepare: " + iSnomedDescription.getConceptId());

				ISnomedConcept matchingConceptWithAncestors = null;
				// try {
				matchingConceptWithAncestors = SnomedRequests.prepareSearchConcept().filterByActive(true)
						.filterByComponentIds(Collections.singleton(iSnomedDescription.getConceptId())).setLimit(1000)
						.setExpand(String.format("ancestors(direct:%s,offset:%d,limit:%d)", "true", 0, 1000))
						.build(branchPath).executeSync(bus).getItems().get(0);

				System.out.println("    No. of matches from prepareSearch "
						+ matchingConceptWithAncestors.getAncestors().getTotal());

				// } catch (Exception e) {
				// System.out.println(" Could not retrieve ancestors for " +
				// iSnomedDescription.getConceptId());
				// }
				System.out.println("     Hierarchy: " + getHierarchyIdForConcept(matchingConceptWithAncestors));

				if (getHierarchyIdForConcept(hierarchyConcept)
						.equals(getHierarchyIdForConcept(matchingConceptWithAncestors))) {
					System.out.println("      -> MATCH from prepareSearch");
					matchesInHierarchy.add(
							new ValidationSnomedDescription(iSnomedDescription, iSnomedDescription.getConceptId()));
				}

			}
		}

		return matchesInHierarchy;

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
				if (Constants.ACCEPTABILITY_PREFERRED.equals(usAcc)
						&& refsetToLanguageSpecificWordsMap.containsKey(Constants.GB_EN_LANG_REFSET)
						&& refsetToLanguageSpecificWordsMap.get(Constants.GB_EN_LANG_REFSET)
								.contains(word.toLowerCase())) {
					errorMessage += "Synonym is preferred in the en-us refset but refers to a word that has en-gb spelling: "
							+ word + "\n";
				}

				// Step 2: Check en-gb preferred synonyms for en-en spellings
				if (Constants.ACCEPTABILITY_PREFERRED.equals(gbAcc)
						&& refsetToLanguageSpecificWordsMap.containsKey(Constants.US_EN_LANG_REFSET)
						&& refsetToLanguageSpecificWordsMap.get(Constants.US_EN_LANG_REFSET)
								.contains(word.toLowerCase())) {
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

		// return immediately if description or term null
		if (description == null || description.getTerm() == null) {
			return result;
		}

		String[] words = description.getTerm().split("\\s+");

		for (String word : words) {

			// NOTE: Simple test to see if a case-sensitive term exists as
			// written. Original check for mis-capitalization, but false
			// positives, e.g. "oF" appears in list but spuriously reports "of"
			// Map preserved for lower-case matching in future
			if (caseSignificantWords.contains(word)) {

				// term starting with case sensitive word must be ETCS
				if (description.getTerm().startsWith(word)
						&& !Constants.ENTIRE_TERM_CASE_SENSITIVE.equals(description.getCaseSignificanceId())) {
					result += "Description starts with case-sensitive word but is not marked entire term case sensitive: "
							+ word + ".\n";
				}

				// term containing case sensitive word (not at start) must be
				// ETCS or OICCI
				else if (!Constants.ENTIRE_TERM_CASE_SENSITIVE.equals(description.getCaseSignificanceId())
						&& !Constants.ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE
								.equals(description.getCaseSignificanceId())) {
					result += "Description contains case-sensitive word but is not marked entire term case sensitive or only initial character case insensitive: "
							+ word + ".\n";
				}
			}
		}
		return result;
	}

}
