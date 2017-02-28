/*
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.datastore.request;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.domain.IComponentRef;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.SnomedConstants.LanguageCodeReferenceSetIdentifierMapping;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * @since 4.5
 */
public abstract class DescriptionRequestHelper {

	private static final class ExtractFirstFunction implements Function<Collection<SnomedDescription>, SnomedDescription> {
		private static final ExtractFirstFunction INSTANCE = new ExtractFirstFunction();

		@Override
		public SnomedDescription apply(Collection<SnomedDescription> input) {
			return Iterables.getFirst(input, null);
		}
	}

	private static final class ExtractBestFunction<T> implements Function<Collection<SnomedDescription>, SnomedDescription> {
		private final List<T> orderedValues;
		private final Function<T, Predicate<SnomedDescription>> predicateFactory;

		private ExtractBestFunction(List<T> orderedValues, Function<T, Predicate<SnomedDescription>> predicateFactory) {
			this.orderedValues = orderedValues;
			this.predicateFactory = predicateFactory;
		}

		@Override 
		public SnomedDescription apply(Collection<SnomedDescription> descriptions) {
			for (T value : orderedValues) {
				final Predicate<SnomedDescription> predicateForValue = predicateFactory.apply(value);
				for (SnomedDescription description : descriptions) {
					if (predicateForValue.apply(description)) {
						return description;
					}
				}
			}
			
			return null;
		}
	}

	private static final class LocalePredicateFactory implements Function<ExtendedLocale, Predicate<SnomedDescription>> {
		private static final LocalePredicateFactory INSTANCE = new LocalePredicateFactory();

		@Override
		public Predicate<SnomedDescription> apply(final ExtendedLocale locale) {
			final String languageRefSetId;
			
			if (!locale.getLanguageRefSetId().isEmpty()) {
				languageRefSetId = locale.getLanguageRefSetId();
			} else {
				languageRefSetId = LanguageCodeReferenceSetIdentifierMapping.getReferenceSetIdentifier(locale.getLanguageTag());
			}
			
			// Couldn't map the locale to a language reference set, reject all descriptions
			if (languageRefSetId == null) {
				return Predicates.alwaysFalse();
			}

			return new Predicate<SnomedDescription>() { 
				@Override 
				public boolean apply(final SnomedDescription description) {
					final boolean acceptabilityMatches = Acceptability.PREFERRED.equals(description.getAcceptabilityMap().get(languageRefSetId));
					final boolean languageCodeMatches = locale.getLanguage().equals(description.getLanguageCode());
					return acceptabilityMatches && languageCodeMatches;
				}
			};
		}
	}

	private static final class LanguageCodePredicateFactory implements Function<String, Predicate<SnomedDescription>> {
		private static final LanguageCodePredicateFactory INSTANCE = new LanguageCodePredicateFactory();

		@Override
		public Predicate<SnomedDescription> apply(final String languageCode) {
			return new Predicate<SnomedDescription>() { 
				@Override 
				public boolean apply(final SnomedDescription description) {
					return languageCode.equals(description.getLanguageCode()) ;
				}
			};
		}
	}

	/**
	 * Retrieves the preferred term for the concept identified by the given {@link IComponentRef component reference}, if it exists. 
	 * <p>
	 * The first active description with "synonym" or descendant as the type will be returned, where all of the following conditions apply:
	 * <ul>
	 * <li>a matching well-known language reference set exists for the given {@code ExtendedLocale} (eg. {@code "en-GB"});
	 * <li>the description has a language reference set member in the reference set identified above with preferred acceptability.
	 * </ul>
	 * <p>
	 * If no such description can be found, the process is repeated with the next {@code Locale} in the list.
	 * 
	 * @param conceptId 	the identifier of the concept for which the preferred term should be returned (may not be {@code null})
	 * @param locales		a list of {@link Locale}s to use, in order of preference
	 * @return 				the preferred term for the concept, or {@code null} if no results could be retrieved
	 */
	public SnomedDescription getPreferredTerm(final String conceptId, final List<ExtendedLocale> locales) {
		final SnomedDescriptionSearchRequestBuilder req = preparePtSearch(conceptId, locales);
		final SnomedDescriptions descriptions = execute(req);
		final Map<String, SnomedDescription> bestMatchByConceptId = indexBestPreferredByConceptId(descriptions, locales);
		return bestMatchByConceptId.get(conceptId);
	}
	
	public Map<String, SnomedDescription> getPreferredTerms(Set<String> conceptIds, List<ExtendedLocale> locales) {
		if (conceptIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final SnomedDescriptionSearchRequestBuilder req = preparePtSearch(conceptIds, locales);
		final SnomedDescriptions descriptions = execute(req);
		final Map<String, SnomedDescription> bestMatchByConceptId = indexBestPreferredByConceptId(descriptions, locales);
		return bestMatchByConceptId;
	}

	/**
	 * Retrieves the fully specified name for the concept identified by the given {@link IComponentRef component reference}, if it exists. 
	 * <p>
	 * The first active description with "fully specified name" as the type will be returned, where all of the following conditions apply:
	 * <ul>
	 * <li>a matching well-known language reference set exists for the given {@code Locale} (eg. {@code "en-GB"});
	 * <li>the description has a language reference set member in the reference set identified above with preferred acceptability.
	 * </ul>
	 * <p>
	 * If no such description can be found, the search is repeated with the following conditions:
	 * <ul>
	 * <li>the description's language code matches the supplied {@code Locale}'s language (eg. {@code "en"} on description, {@code "en-US"} on {@code Locale});
	 * </ul>
	 * <p>
	 * Failing that, the whole check starts from the beginning with the next {@link Locale} in the list.
	 * The method falls back to the first active fully specified name if the language code does not match any of the specified {@code Locale}s.
	 * 
	 * @param conceptRef the reference to the concept for which the preferred term should be returned (may not be {@code null})
	 * @param locales    a list of {@link Locale}s to use, in order of preference
	 * @return the preferred term for the concept
	 */
	public SnomedDescription getFullySpecifiedName(final String conceptId, final List<ExtendedLocale> locales) {
		final SnomedDescriptionSearchRequestBuilder acceptabilityReq = prepareFsnSearchByAcceptability(conceptId, locales);
		final SnomedDescriptions preferredDescriptions = execute(acceptabilityReq);
		final Map<String, SnomedDescription> bestPreferredByConceptId = indexBestPreferredByConceptId(preferredDescriptions, locales);
		
		if (bestPreferredByConceptId.containsKey(conceptId)) {
			return bestPreferredByConceptId.get(conceptId);
		}
		
		final List<String> languageCodes = getLanguageCodes(locales);
		
		final SnomedDescriptionSearchRequestBuilder languageCodeReq = prepareFsnSearchByLanguageCodes(conceptId, languageCodes);
		final SnomedDescriptions languageCodeDescriptions = execute(languageCodeReq);
		final Map<String, SnomedDescription> bestLanguageByConceptId = indexBestLanguageByConceptId(languageCodeDescriptions, languageCodes);

		if (bestLanguageByConceptId.containsKey(conceptId)) {
			return bestLanguageByConceptId.get(conceptId);
		}

		final SnomedDescriptionSearchRequestBuilder defaultReq = prepareFsnSearchDefault(conceptId);
		final SnomedDescriptions activeFsnDescriptions = execute(defaultReq);

		/* 
		 * XXX: we usually expect to see just one active FSN at this point, but depending on the given ExtendedLocale combinations,
		 * there might be more candidates remaining. 
		 */
		return Iterables.getFirst(activeFsnDescriptions, null); 
	}

	public Map<String, SnomedDescription> getFullySpecifiedNames(Set<String> conceptIds, List<ExtendedLocale> locales) {
		if (conceptIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final Map<String, SnomedDescription> fsnMap = newHashMap();
		Set<String> conceptIdsNotInMap;
		
		final SnomedDescriptionSearchRequestBuilder acceptabilityReq = prepareFsnSearchByAcceptability(conceptIds, locales);
		final SnomedDescriptions preferredDescriptions = execute(acceptabilityReq);
		fsnMap.putAll(indexBestPreferredByConceptId(preferredDescriptions, locales));
		
		conceptIdsNotInMap = Sets.difference(conceptIds, fsnMap.keySet());
		if (conceptIdsNotInMap.isEmpty()) {
			return fsnMap;
		}
		
		final List<String> languageCodes = getLanguageCodes(locales);
		final SnomedDescriptionSearchRequestBuilder languageCodeReq = prepareFsnSearchByLanguageCodes(conceptIdsNotInMap, languageCodes);
		final SnomedDescriptions languageCodeDescriptions = execute(languageCodeReq);
		fsnMap.putAll(indexBestLanguageByConceptId(languageCodeDescriptions, languageCodes));
		
		conceptIdsNotInMap = Sets.difference(conceptIds, fsnMap.keySet());
		if (conceptIdsNotInMap.isEmpty()) {
			return fsnMap;
		}

		SnomedDescriptionSearchRequestBuilder defaultReq = prepareFsnSearchDefault(conceptIdsNotInMap);
		SnomedDescriptions activeFsnDescriptions = execute(defaultReq);
		fsnMap.putAll(indexFirstByConceptId(activeFsnDescriptions));

		return fsnMap;
	}

	private List<String> getLanguageCodes(final List<ExtendedLocale> locales) {
		return FluentIterable.from(locales)
				.transform(new Function<ExtendedLocale, String>() { @Override public String apply(ExtendedLocale input) { return input.getLanguage(); } })
				.toSet() // preserves iteration order, but makes elements unique
				.asList();
	}

	// FSN requests
	
	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchByAcceptability(final String conceptId, final List<ExtendedLocale> locales) {
		return prepareFsnSearchDefault(conceptId)
				.filterByAcceptability(Acceptability.PREFERRED)
				.filterByExtendedLocales(locales);
	}
	
	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchByAcceptability(final Collection<String> conceptIds, final List<ExtendedLocale> locales) {
		return prepareFsnSearchDefault(conceptIds)
				.filterByAcceptability(Acceptability.PREFERRED)
				.filterByExtendedLocales(locales);
	}
	
	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchByLanguageCodes(final String conceptId, final List<String> languageCodes) {
		return prepareFsnSearchDefault(conceptId)
				.filterByLanguageCodes(languageCodes);
	}
	
	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchByLanguageCodes(final Collection<String> conceptIds, final List<String> languageCodes) {
		return prepareFsnSearchDefault(conceptIds)
				.filterByLanguageCodes(languageCodes);
	}

	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchDefault(final String conceptId) {
		return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByActive(true)
				.filterByConcept(conceptId)
				.filterByType(Concepts.FULLY_SPECIFIED_NAME);
	}
	
	private SnomedDescriptionSearchRequestBuilder prepareFsnSearchDefault(final Collection<String> conceptIds) {
		return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByActive(true)
				.filterByConceptId(conceptIds)
				.filterByType(Concepts.FULLY_SPECIFIED_NAME);
	}

	// PT requests
	
	private SnomedDescriptionSearchRequestBuilder preparePtSearch(final String conceptId, final List<ExtendedLocale> locales) {
		return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByActive(true)
				.filterByConcept(conceptId)
				.filterByType("<<" + Concepts.SYNONYM)
				.filterByAcceptability(Acceptability.PREFERRED)
				.filterByExtendedLocales(locales);
	}
	
	private SnomedDescriptionSearchRequestBuilder preparePtSearch(final Collection<String> conceptIds, final List<ExtendedLocale> locales) {
		return SnomedRequests.prepareSearchDescription()
				.all()
				.filterByActive(true)
				.filterByConceptId(conceptIds)
				.filterByType("<<" + Concepts.SYNONYM)
				.filterByAcceptability(Acceptability.PREFERRED)
				.filterByExtendedLocales(locales);
	}

	private Map<String, SnomedDescription> indexBestPreferredByConceptId(final SnomedDescriptions descriptions, final List<ExtendedLocale> orderedLocales) {
		return extractBest(indexByConceptId(descriptions), orderedLocales, LocalePredicateFactory.INSTANCE);
	}
	
	private Map<String, SnomedDescription> indexBestLanguageByConceptId(final SnomedDescriptions descriptions, final List<String> orderedLanguages) {
		return extractBest(indexByConceptId(descriptions), orderedLanguages, LanguageCodePredicateFactory.INSTANCE);
	}
	
	private Map<String, SnomedDescription> indexFirstByConceptId(final SnomedDescriptions descriptions) {
		return extractFirst(indexByConceptId(descriptions));
	}

	private Multimap<String, SnomedDescription> indexByConceptId(final SnomedDescriptions descriptions) {
		return Multimaps.index(descriptions.getItems(), new Function<SnomedDescription, String>() {
			@Override public String apply(SnomedDescription description) {
				return description.getConceptId();
			}
		});
	}
	
	private Map<String, SnomedDescription> extractFirst(final Multimap<String, SnomedDescription> descriptionsByConceptId) {
		final Map<String, SnomedDescription> uniqueMap = Maps.transformValues(descriptionsByConceptId.asMap(), ExtractFirstFunction.INSTANCE);
		return ImmutableMap.copyOf(Maps.filterValues(uniqueMap, Predicates.notNull()));
	}
	
	private <T> Map<String, SnomedDescription> extractBest(final Multimap<String, SnomedDescription> descriptionsByConceptId, 
			final List<T> orderedValues, 
			final Function<T, Predicate<SnomedDescription>> predicateFactory) {
		
		final Map<String, SnomedDescription> uniqueMap = Maps.transformValues(descriptionsByConceptId.asMap(), new ExtractBestFunction<T>(orderedValues, predicateFactory));
		return ImmutableMap.copyOf(Maps.filterValues(uniqueMap, Predicates.notNull()));
	}
	
	protected abstract SnomedDescriptions execute(SnomedDescriptionSearchRequestBuilder req);
}
