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
package com.b2international.snowowl.snomed.datastore.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.EList;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.IDisposableService;
import com.b2international.snowowl.core.api.ComponentTextProvider;
import com.b2international.snowowl.core.api.ILookupService;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.CdoViewComponentTextProvider;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.CaseSignificance;
import com.b2international.snowowl.snomed.datastore.SnomedConceptLookupService;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedDescriptionLookupService;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 * Lookup service for the most common terminology lookup functions.
 * <p>This service is registered to the {@link ApplicationContext application context} as {@link SnomedLookupService}.
 * 
 */
public class SnomedLookupService implements IDisposableService, ISnomedLookupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedLookupService.class);
	/**Predicate which returns with {@code false} if a description is FSN or preferred term.*/
	private static final long SYNONYM_ID = Long.valueOf(Concepts.SYNONYM);
	private static final long FSN_ID = Long.valueOf(Concepts.FULLY_SPECIFIED_NAME);

	/* predicate for excluding inactive/retired SNOMED CT components. */
	private static final Predicate<Component> ACTIVE_PREDICATE = new Predicate<Component>() {
		@Override
		public boolean apply(final Component historicalItem) {
			return historicalItem.isActive();
		}
	};
	
	/* function for extracting the term of a SNOMED CT description. */
	private static final Function<Description, String> DESCRIPTION_TO_TERM = new Function<Description, String>() {
		@Override
		public String apply(final Description description) {
			return description.getTerm();
		}
	};
	
	//access to the db
	private final CDOView cdoView;

	private final ComponentTextProvider conceptTextProvider;
	
	/**
	 * Constructor
	 * @param cdoView to provide access to the db
	 */
	public SnomedLookupService(final CDOView cdoView) {
		this.cdoView = cdoView;
		conceptTextProvider = new CdoViewComponentTextProvider(ApplicationContext.getServiceForClass(ISnomedConceptNameProvider.class), cdoView);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#hasRelationship(long, long, long)
	 */
	@Override
	public boolean hasRelationship(final long sourceConceptId, final long destinationConceptId, final long relationshipTypeId) {
		return hasRelationship(String.valueOf(sourceConceptId), String.valueOf(destinationConceptId), String.valueOf(relationshipTypeId));
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#hasRelationship(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean hasRelationship(final String sourceConceptId, final String destinationConceptId, final String relationshipTypeId) {
		
		Preconditions.checkNotNull(sourceConceptId, "Source SNOMED CT concept ID cannot be null.");
		Preconditions.checkNotNull(destinationConceptId, "Destination SNOMED CT concept ID cannot be null.");
		Preconditions.checkNotNull(relationshipTypeId, "Relationship type SNOMED CT concept ID cannot be null.");
		
		final SnomedConceptLookupService conceptLookupService = new SnomedConceptLookupService();
		final Concept sourceConcept = conceptLookupService.getComponent(sourceConceptId, cdoView);
		
		if (sourceConcept == null)
			throw new IllegalArgumentException("Source concept not found with ID: " + sourceConceptId);
		
		final Concept destinationConcept = conceptLookupService.getComponent(destinationConceptId, cdoView);
		if (destinationConcept == null)
			throw new IllegalArgumentException("Destination concept not found with ID: " + destinationConceptId);
		
		final Concept relationshipConcept = conceptLookupService.getComponent(relationshipTypeId, cdoView);
		if (relationshipConcept == null)
			throw new IllegalArgumentException("Relationship type concept not found with ID: " + relationshipTypeId);
		
		
		for (final Relationship relationship : sourceConcept.getOutboundRelationships()) {
			if (!relationship.isActive()) {
				continue;
			} 
			if (!relationshipTypeId.equals(relationship.getType().getId())) {
				continue;
			}
			if (!destinationConceptId.equals(relationship.getDestination().getId())) {
				continue;
			}
			
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getConceptId(java.lang.String)
	 */
	@Override
	public String getConceptId(final String descriptionId) {
		final SnomedDescriptionIndexEntry component = new SnomedDescriptionLookupService().getComponent(BranchPathUtils.createActivePath(SnomedDatastoreActivator.REPOSITORY_UUID), descriptionId);
		if (component == null) {
			return null;
		}
		return component.getConceptId();
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getDescriptionTerms(java.lang.String)
	 */
	@Override
	public String[] getDescriptionTerms(final String conceptId) {
		final Collection<Description> descriptions = getDescriptionsFromStore(conceptId);

		if (descriptions == null) {
			return new String[] {};
		}
		return buildTermListFromDescriptions(descriptions);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getDescriptionTerms(long, long)
	 */
	@Override
	public String[] getDescriptionTerms(final long conceptId, final long descriptionTypeConceptId) {
		final Concept concept = getConcept(conceptId);
		if (null == concept) {
			return new String[] {};
		}

		final Collection<Description> activeDescriptions = getActiveDescriptions(concept, descriptionTypeConceptId);

		if (activeDescriptions.isEmpty()) {
			LOGGER.error("SNOMED CT concept does not have any active description. Concept ID: " + conceptId);
			return new String[] {};
		}
		return Iterables.toArray(Collections2.transform(activeDescriptions, DESCRIPTION_TO_TERM), String.class);
	}
	
	@Override
	public String getPreferredTerm(final String conceptId) {
		return conceptTextProvider.getText(conceptId);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getFullySpecifiedName(long)
	 */
	@Override
	public String getFullySpecifiedName(final long conceptId) {
		return getFsnDescription(conceptId).getTerm();
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getFsnDescription(long)
	 */
	@Override
	public Description getFsnDescription(final long conceptId) {
		final Concept concept = getConcept(conceptId);
		if (null == concept) {
			return null;
		}

		final Collection<Description> activeFullySpecifiedNames = getActiveDescriptions(concept, FSN_ID);
		if (activeFullySpecifiedNames.isEmpty()) {
			LOGGER.error("SNOMED CT concept does not have any active fully specified name. Concept ID: " + conceptId);
			return null;
		}

		if (activeFullySpecifiedNames.size() > 1) {
			LOGGER.warn("SNOMED CT concept has more than one any active fully specified name. Concept ID: " + conceptId
					+ " Fully specified names: " + Arrays.toString(activeFullySpecifiedNames.toArray()));
		}

		final Description description = Iterables.get(activeFullySpecifiedNames, 0);
		final String term = description.getTerm();
		if (StringUtils.isEmpty(term)) {
			LOGGER.error("The term of the active fully specified name for concept (ID: " + conceptId + ") is missing");
			return null;
		}

		return description;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getSynonyms(long)
	 */
	@Override
	public String[] getSynonyms(final long conceptId) {
		final String[] descriptions = getDescriptionTerms(conceptId, SYNONYM_ID);
		if (CompareUtils.isEmpty(descriptions)) {
			LOGGER.error("SNOMED CT concept does not have any active synonym. Concept ID: " + conceptId);
			return new String[] {};
		}
		return descriptions;
	}



	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isDescriptionExist(java.lang.String, java.lang.String, com.b2international.snowowl.snomed.datastore.CaseSignificance)
	 */
	@Override
	public boolean isDescriptionExist(final String conceptId, final String termToMatch, final CaseSignificance caseSensitivity) {
		final String[] descriptions = getDescriptionTerms(conceptId);

		if (descriptions == null) {
			return false;
		}

		final boolean descriptionExist = isDescriptionExist(descriptions, caseSensitivity, termToMatch);

		return descriptionExist;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isDescriptionExist(long, java.lang.String, com.b2international.snowowl.snomed.datastore.CaseSignificance, long)
	 */
	@Override
	public boolean isDescriptionExist(final long conceptId, final String termToMatch, final CaseSignificance caseSensitivity, final long descriptionTypeConceptId) {
		final String[] descriptionsFilteredByType = getDescriptionTerms(conceptId, descriptionTypeConceptId);

		if (descriptionsFilteredByType == null) {
			return false;
		}

		return isDescriptionExist(descriptionsFilteredByType, caseSensitivity, termToMatch);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isDescriptionExist(java.lang.String, java.lang.String, com.b2international.snowowl.snomed.datastore.CaseSignificance, java.lang.String)
	 */
	@Override
	public boolean isDescriptionExist(final String conceptId, final String termToMatch, final CaseSignificance caseSensitivity, final String descriptionTypeConceptId) {
		
		return isDescriptionExist(Long.valueOf(conceptId), termToMatch, caseSensitivity, Long.valueOf(descriptionTypeConceptId));
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#descriptionTermMatches(java.lang.String, java.lang.String, com.b2international.snowowl.snomed.datastore.CaseSignificance)
	 */
	@Override
	public boolean descriptionTermMatches(final String descriptionTerm, final String termToMatch, final CaseSignificance caseSensitivity) {
		switch (caseSensitivity) {
		case ENTIRE_TERM_CASE_SENSITIVE:
			return descriptionTerm.equals(termToMatch);
		case ENTIRE_TERM_CASE_INSENSITIVE:
			return descriptionTerm.equalsIgnoreCase(termToMatch);
		case ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE:
			return equalsFirstLetterCaseIgnored(descriptionTerm, termToMatch);
		default:
			throw new IllegalArgumentException("Couldn't recognize case sensitivity: " + caseSensitivity.toString());
		}
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getConcept(long)
	 */
	@Override
	public Concept getConcept(final long conceptId) {
		final Concept concept = getConceptLookupService().getComponent(String.valueOf(conceptId), cdoView);
		if (null == concept) {
			LOGGER.warn("SNOMED CT concept does not exists in the store by the specified ID. ID: " + conceptId);
		}
		return concept;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isConceptExist(long)
	 */
	@Override
	public boolean isConceptExist(final long conceptId) {
		return null != getConcept(conceptId);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isConceptExist(java.lang.String)
	 */
	@Override
	public boolean isConceptExist(final String conceptId) {
		return null != getConceptById(conceptId);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#dispose()
	 */
	@Override
	public void dispose() {
		LifecycleUtil.deactivate(cdoView);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return cdoView.isClosed();
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#getConceptById(java.lang.String)
	 */
	@Override
	public Concept getConceptById(final String conceptId) {
		final SnomedConceptLookupService snomedConceptLookupService = new SnomedConceptLookupService();
		return snomedConceptLookupService.getComponent(conceptId, cdoView);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#generateNewConceptId()
	 */
	@Override
	public String generateNewConceptId() {
		return generateNewComponsnetId(ComponentCategory.CONCEPT);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#generateNewDescriptionId()
	 */
	@Override
	public String generateNewDescriptionId() {
		return generateNewComponsnetId(ComponentCategory.DESCRIPTION);
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.services.ISnomedLookupService#generateNewRelationshipId()
	 */
	@Override
	public String generateNewRelationshipId() {
		return generateNewComponsnetId(ComponentCategory.RELATIONSHIP);
	}
	
	/*generates and returns with a brand new, non-existing component ID based on the component nature argument.*/
	private String generateNewComponsnetId(final ComponentCategory componentNature) {
		Preconditions.checkNotNull(componentNature, "Component nature argument cannot be null.");
		return ApplicationContext.getInstance().getServiceChecked(ISnomedIdentifierService.class).generate(SnomedEditingContext.getDefaultNamespace(), componentNature);
	}
	
	private boolean isDescriptionExist(final String[] descriptionTerms, final CaseSignificance caseSensitivity, final String termToMatch) {
		boolean found = false;
		for (final String description : descriptionTerms) {
			found = descriptionTermMatches(description, termToMatch, caseSensitivity);

			if (found) {
				return true;
			}
		}

		return false;
	}

	private boolean equalsFirstLetterCaseIgnored(final String descriptionTerm, final String termToMatch) {
		if (StringUtils.isEmpty(descriptionTerm) || StringUtils.isEmpty(termToMatch)) {
			return false;
		}

		final char descriptionFirstLetter = Character.toLowerCase(descriptionTerm.charAt(0));
		final char termToMatchFirstLetter = Character.toLowerCase(termToMatch.charAt(0));

		if (descriptionFirstLetter != termToMatchFirstLetter) {
			return false;
		}

		if (descriptionTerm.length() == 1 || termToMatch.length() == 1) {
			return false;
		}

		final String subsequentDescriptionTerm = descriptionTerm.substring(1);
		final String subsequentTermToMatch = termToMatch.substring(1);

		return subsequentDescriptionTerm.equals(subsequentTermToMatch);
	}

	private List<Description> getDescriptionsFromStore(final String conceptId) {
		final SnomedConceptLookupService snomedConceptLookupService = new SnomedConceptLookupService();
		final Concept concept = snomedConceptLookupService.getComponent(conceptId, cdoView);

		if (concept == null) {
			return null;
		}

		final EList<Description> descriptions = concept.getDescriptions();

		return descriptions;
	}

	private String[] buildTermListFromDescriptions(final Collection<Description> descriptions) {
		final Collection<String> transformed = Collections2.transform(descriptions, new Function<Description, String>() {
			@Override
			public String apply(final Description description) {
				return description.getTerm();
			}
		});

		return transformed.toArray(new String[] {});
	}

	/*
	 * returns with all the active descriptions of the SNOMED CT concept where
	 * the description type matches with the expected SNOMED CT description type
	 * concept identified by the specified description type concept ID
	 */
	private Collection<Description> getActiveDescriptions(@Nonnull final Concept concept, final long descriptionTypeConceptId) {
		return Collections2.filter(getActiveDescriptions(concept), new DescriptionTypeIdPredicate(descriptionTypeConceptId));
	}

	/*
	 * return with all the active descriptions of the specified SNOMED CT
	 * concept
	 */
	private Collection<Description> getActiveDescriptions(@Nonnull final Concept concept) {
		final Collection<Description> collection = Collections2.filter(Preconditions.checkNotNull(concept, "SNOMED CT concept argument cannot be null.").getDescriptions(),
				ACTIVE_PREDICATE);
		
		return collection;
	}

	/*
	 * returns with the lookup service for retrieving SNOMED CT concepts by
	 * their unique ID
	 */
	private ILookupService<String, Concept, CDOView> getConceptLookupService() {
		return CoreTerminologyBroker.getInstance().getLookupService(SnomedTerminologyComponentConstants.CONCEPT);
	}
	
	/**
	 * Predicate for checking a SNOMED&nbsp;CT component by its unique ID.
	 * 
	 * @see Predicate
	 */
	private static final class DescriptionTypeIdPredicate implements Predicate<Description> {
		private final long descriptionTypeId;
	
		private DescriptionTypeIdPredicate(final long descriptionTypeId) {
			this.descriptionTypeId = descriptionTypeId;
		}
	
		@Override
		public boolean apply(final Description description) {
			return Long.toString(descriptionTypeId).equals(description.getType().getId());
		}
	}
	
}