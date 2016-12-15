/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
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

	private static final class ExtractFirstFunction implements Function<Collection<ISnomedDescription>, ISnomedDescription> {
		private static final ExtractFirstFunction INSTANCE = new ExtractFirstFunction();

		@Override
		public ISnomedDescription apply(Collection<ISnomedDescription> input) {
			return Iterables.getFirst(input, null);
		}
	}

	private static final class ExtractBestFunction<T> implements Function<Collection<ISnomedDescription>, ISnomedDescription> {
		private final List<T> orderedValues;
		private final Function<T, Predicate<ISnomedDescription>> predicateFactory;

		private ExtractBestFunction(List<T> orderedValues, Function<T, Predicate<ISnomedDescription>> predicateFactory) {
			this.orderedValues = orderedValues;
			this.predicateFactory = predicateFactory;
		}

		@Override 
		public ISnomedDescription apply(Collection<ISnomedDescription> descriptions) {
			for (T value : orderedValues) {
				final Predicate<ISnomedDescription> predicateForValue = predicateFactory.apply(value);
				for (ISnomedDescription description : descriptions) {
					if (predicateForValue.apply(description)) {
						return description;
					}
				}
			}
			
			return null;
		}
	}

	private static final class LocalePredicateFactory implements Function<ExtendedLocale, Predicate<ISnomedDescription>> {
		private static final LocalePredicateFactory INSTANCE = new LocalePredicateFactory();

		@Override
		public Predicate<ISnomedDescription> apply(final ExtendedLocale locale) {
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

			return new Predicate<ISnomedDescription>() { 
				@Override 
				public boolean apply(final ISnomedDescription description) {
					final boolean acceptabilityMatches = Acceptability.PREFERRED.equals(description.getAcceptabilityMap().get(languageRefSetId));
					final boolean languageCodeMatches = locale.getLanguage().equals(description.getLanguageCode());
					return acceptabilityMatches && languageCodeMatches;
				}
			};
		}
	}

	private static final class LanguageCodePredicateFactory implements Function<String, Predicate<ISnomedDescription>> {
		private static final LanguageCodePredicateFactory INSTANCE = new LanguageCodePredicateFactory();

		@Override
		public Predicate<ISnomedDescription> apply(final String languageCode) {
			return new Predicate<ISnomedDescription>() { 
				@Override 
				public boolean apply(final ISnomedDescription description) {
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
	public ISnomedDescription getPreferredTerm(final String conceptId, final List<ExtendedLocale> locales) {
		final SnomedDescriptionSearchRequestBuilder req = preparePtSearch(conceptId, locales);
		final SnomedDescriptions descriptions = execute(req);
		final Map<String, ISnomedDescription> bestMatchByConceptId = indexBestPreferredByConceptId(descriptions, locales);
		return bestMatchByConceptId.get(conceptId);
	}
	
	public Map<String, ISnomedDescription> getPreferredTerms(Set<String> conceptIds, List<ExtendedLocale> locales) {
		if (conceptIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final SnomedDescriptionSearchRequestBuilder req = preparePtSearch(conceptIds, locales);
		final SnomedDescriptions descriptions = execute(req);
		final Map<String, ISnomedDescription> bestMatchByConceptId = indexBestPreferredByConceptId(descriptions, locales);
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
	public ISnomedDescription getFullySpecifiedName(final String conceptId, final List<ExtendedLocale> locales) {
		final SnomedDescriptionSearchRequestBuilder acceptabilityReq = prepareFsnSearchByAcceptability(conceptId, locales);
		final SnomedDescriptions preferredDescriptions = execute(acceptabilityReq);
		final Map<String, ISnomedDescription> bestPreferredByConceptId = indexBestPreferredByConceptId(preferredDescriptions, locales);
		
		if (bestPreferredByConceptId.containsKey(conceptId)) {
			return bestPreferredByConceptId.get(conceptId);
		}
		
		final List<String> languageCodes = getLanguageCodes(locales);
		
		final SnomedDescriptionSearchRequestBuilder languageCodeReq = prepareFsnSearchByLanguageCodes(conceptId, languageCodes);
		final SnomedDescriptions languageCodeDescriptions = execute(languageCodeReq);
		final Map<String, ISnomedDescription> bestLanguageByConceptId = indexBestLanguageByConceptId(languageCodeDescriptions, languageCodes);

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

	public Map<String, ISnomedDescription> getFullySpecifiedNames(Set<String> conceptIds, List<ExtendedLocale> locales) {
		if (conceptIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final Map<String, ISnomedDescription> fsnMap = newHashMap();
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
				.filterByConceptId(conceptId)
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
				.filterByConceptId(conceptId)
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

	private Map<String, ISnomedDescription> indexBestPreferredByConceptId(final SnomedDescriptions descriptions, final List<ExtendedLocale> orderedLocales) {
		return extractBest(indexByConceptId(descriptions), orderedLocales, LocalePredicateFactory.INSTANCE);
	}
	
	private Map<String, ISnomedDescription> indexBestLanguageByConceptId(final SnomedDescriptions descriptions, final List<String> orderedLanguages) {
		return extractBest(indexByConceptId(descriptions), orderedLanguages, LanguageCodePredicateFactory.INSTANCE);
	}
	
	private Map<String, ISnomedDescription> indexFirstByConceptId(final SnomedDescriptions descriptions) {
		return extractFirst(indexByConceptId(descriptions));
	}

	private Multimap<String, ISnomedDescription> indexByConceptId(final SnomedDescriptions descriptions) {
		return Multimaps.index(descriptions.getItems(), new Function<ISnomedDescription, String>() {
			@Override public String apply(ISnomedDescription description) {
				return description.getConceptId();
			}
		});
	}
	
	private Map<String, ISnomedDescription> extractFirst(final Multimap<String, ISnomedDescription> descriptionsByConceptId) {
		final Map<String, ISnomedDescription> uniqueMap = Maps.transformValues(descriptionsByConceptId.asMap(), ExtractFirstFunction.INSTANCE);
		return ImmutableMap.copyOf(Maps.filterValues(uniqueMap, Predicates.notNull()));
	}
	
	private <T> Map<String, ISnomedDescription> extractBest(final Multimap<String, ISnomedDescription> descriptionsByConceptId, 
			final List<T> orderedValues, 
			final Function<T, Predicate<ISnomedDescription>> predicateFactory) {
		
		final Map<String, ISnomedDescription> uniqueMap = Maps.transformValues(descriptionsByConceptId.asMap(), new ExtractBestFunction<T>(orderedValues, predicateFactory));
		return ImmutableMap.copyOf(Maps.filterValues(uniqueMap, Predicates.notNull()));
	}
	
	protected abstract SnomedDescriptions execute(SnomedDescriptionSearchRequestBuilder req);
}
