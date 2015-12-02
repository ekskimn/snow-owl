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
package com.b2international.snowowl.datastore.server.snomed.history;

import static com.b2international.commons.Pair.IdenticalPair.identicalPairOf;
import static com.b2international.commons.StringUtils.isEmpty;
import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.b2international.snowowl.datastore.BranchPathUtils.createPath;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.ACCEPTABILITY_ID_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.CASE_SIGNIFICANCE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.CHARACTERISTIC_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.CORRELATION_ID_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.DEFINITION_STATUS_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.DESCRIPTION_FORMAT_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.DESCRIPTION_LENGTH_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.DESCRIPTION_TERM_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.DESCRIPTION_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.EFFECTIVE_TIME_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.EXHAUSTIVE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.GROUP_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.MAP_GROUP_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.MAP_TARGET_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.MODIFIER_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.MODULE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.MODULE_ID_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.OPERATOR_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.RELATIONSHIP_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.RELEASED_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.SOURCE_EFFECTIVE_TIME_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.STATUS_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.TARGET_EFFECTIVE_TIME_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.UNION_GROUP_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.UNIT_TYPE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.VALUE_FEATURE_NAME;
import static com.b2international.snowowl.datastore.server.snomed.history.SnomedHistoryInfoConstants.VALUE_ID_FEATURE_NAME;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.delta.CDOSetFeatureDelta;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.spi.cdo.CDOStore;

import com.b2international.commons.ChangeKind;
import com.b2international.commons.Pair;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.IHistoryInfoDetails;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.datastore.history.HistoryInfoDetails;
import com.b2international.snowowl.datastore.server.history.AbstractHistoryInfoDetailsBuilder;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.services.ISnomedConceptNameProvider;
import com.b2international.snowowl.snomed.datastore.services.ISnomedDescriptionNameProvider;
import com.b2international.snowowl.snomed.snomedrefset.SnomedAttributeValueRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedComplexMapRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedConcreteDataTypeRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedDescriptionTypeRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedLanguageRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedMappingRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedQueryRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetPackage;
import com.b2international.snowowl.snomed.snomedrefset.SnomedSimpleMapRefSetMember;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Builder class for SNOMED CT specific detailed history information. 
 *
 */
public class SnomedConceptHistoryInfoDetailsBuilder extends AbstractHistoryInfoDetailsBuilder {

	private final LoadingCache<Pair<IBranchPath, String>, String> idLabelCache = CacheBuilder.newBuilder().build(new CacheLoader<Pair<IBranchPath, String>, String>() {
		public String load(final Pair<IBranchPath, String> pair) throws Exception {
			final String id = pair.getB();
			final String label = getPreferredTerm(pair.getA(), id);
			return isEmpty(label) ? id : label; 
		}
	});
	
	private static HashMap<String, String> map;

	@Override
	protected Collection<? extends IHistoryInfoDetails> processNewObjects(final List<CDOIDAndVersion> newObjects, final CDOView beforeView, final CDOView currentView) {
		return processNewObjects(newObjects, beforeView, currentView, true);
	}
	
	@Override
	protected IHistoryInfoDetails generateInfoForNewObject(final CDOObject cdoObject, final CDOView beforeView, final CDOView currentView) {
		final String description = getDescription(cdoObject, beforeView, currentView, "New ", "added to ");
		if (isEmpty(description)) {
			return IHistoryInfoDetails.IGNORED_DETAILS;
		}
		return new HistoryInfoDetails(getComponent(cdoObject), description, ChangeKind.ADDED);
	}

	@Override
	protected IHistoryInfoDetails generateInfoForDetachedObject(final CDOObject cdoObject, final CDOView beforeView, final CDOView currentView) {
		final String description = getDescription(cdoObject, beforeView, currentView, "Detached ", "detached from ");
		if (isEmpty(description)) {
			return IHistoryInfoDetails.IGNORED_DETAILS;
		}
		return new HistoryInfoDetails(getComponent(cdoObject), description, ChangeKind.DELETED);
	}

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.history.AbstractHistoryInfoDetailsBuilder#generateInfoForChangedObject(org.eclipse.emf.cdo.CDOObject, org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta)
	 */
	@Override
	protected IHistoryInfoDetails generateInfoForChangedObject(final CDOObject cdoObject, final CDOView currentView, final CDOView beforeView, final CDOSetFeatureDelta featureDelta) {
		final String description = generateChangeDescription(cdoObject, currentView, beforeView, featureDelta);
		if (isEmpty(description)) {
			return IHistoryInfoDetails.IGNORED_DETAILS;
		}
		return new HistoryInfoDetails(getComponent(cdoObject), description, ChangeKind.UPDATED);
	}

	public String getDescription(final CDOObject cdoObject, final CDOView beforeView, final CDOView currentView, final String change, final String refsetChange) {
		if (cdoObject instanceof Concept) {
			return change + "concept: \"" + getPreferredTerm(((Concept) cdoObject)) + "\".";
		} else if (cdoObject instanceof Description) {
			return change + getPreferredTerm(((Description) cdoObject).getType()) + ": \"" + ((Description) cdoObject).getTerm() + "\".";
		} else if (cdoObject instanceof Relationship) {
			final Relationship relationship = (Relationship) cdoObject;
			if (null != relationship.getSource() && null != relationship.getType() && null != relationship.getDestination())
				return change + "relationship: " +  getLabel(relationship) + ".";
		} else if (cdoObject instanceof SnomedConcreteDataTypeRefSetMember) {
			return change + "concrete domain element: \"" + getConcreteDataTypeItem((SnomedConcreteDataTypeRefSetMember) cdoObject) + "\".";
		} else if (cdoObject instanceof SnomedRefSetMember) {
			return getRefSetChangeDescription((SnomedRefSetMember) cdoObject, beforeView, currentView, refsetChange);
		} else if (cdoObject instanceof SnomedRefSet) {
			return change + "reference set: \"" + getLabel((SnomedRefSet) cdoObject) + "\".";
		} 
		return null;
	}
	
	@Override
	public short getTerminologyComponentId() {
		return SnomedTerminologyComponentConstants.CONCEPT_NUMBER;
	}

	public String generateChangeDescription(final CDOObject changedObject, final CDOView currentView, final CDOView beforeView, final CDOSetFeatureDelta featureDelta) {
	
		final EStructuralFeature feature = featureDelta.getFeature();
		final Object featureValue = featureDelta.getValue();
		final String featureName = feature.getName();
		final StringBuilder builder = new StringBuilder();
		
		if (changedObject instanceof Concept) {
			final Concept concept = (Concept) changedObject;
			if (SnomedHistoryInfoConstants.STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue),
						getPreferredTerm(concept)).toString();
			} else if (SnomedHistoryInfoConstants.DEFINITION_STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getPreferredTerm(concept)).toString();
			} else if (SnomedHistoryInfoConstants.EXHAUSTIVE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue, "mutually disjoint", "non-disjoint"), 
						getPreferredTerm(concept)).toString();
			} else if (SnomedHistoryInfoConstants.EFFECTIVE_TIME_FEATURE_NAME.equals(featureName))  {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getPreferredTerm(concept)).toString();
			} else if (MODULE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getPreferredTerm(concept)).toString();
			} else if (RELEASED_FEATURE_NAME.equals(featureName)) {
				final String label = getPreferredTerm(concept);
				return getPublishedChange(featureName, builder, label, featureValue);
			}
		} else if (changedObject instanceof Description) {
			final Description description = (Description) changedObject;
			if (STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue), 
						"\"" + description.getTerm() + "\"").toString();
			} else if (CASE_SIGNIFICANCE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						"\"" + description.getTerm() + "\"").toString();
			} else if (MODULE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						"\"" + description.getTerm() + "\"").toString();
			} else if (DESCRIPTION_TYPE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						"\"" + description.getTerm() + "\"").toString();
			} else if (EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						description.getTerm()).toString();
			} else if (DESCRIPTION_TERM_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						String.valueOf(featureValue),
						description.getTerm()).toString();
			} else if (RELEASED_FEATURE_NAME.equals(featureName)) {
				return getPublishedChange(featureName, builder, description.getTerm(), featureValue);
			}
		} else if (changedObject instanceof Relationship) {
			final Relationship relationship = (Relationship) changedObject;
			if (STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue), 
						getLabel(relationship)).toString();
			} else if (MODULE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getLabel(relationship)).toString();
			} else if (EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getLabel(relationship)).toString();
			} else if (GROUP_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), String.valueOf(featureValue), 
						getLabel(relationship)).toString();
			} else if (UNION_GROUP_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), String.valueOf(featureValue), 
						getLabel(relationship)).toString();
			} else if (CHARACTERISTIC_TYPE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getLabel(relationship)).toString();
			} else if (MODIFIER_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getLabel(relationship)).toString();
			} else if (RELEASED_FEATURE_NAME.equals(featureName)) {
				return getPublishedChange(featureName, builder, getLabel(relationship), featureValue);
			} else if (RELATIONSHIP_TYPE_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, "relationship type" /*;( quite ugly but there is a collision with the description type feature name*/
						, SnomedHistoryUtils.getNewFeatureValue(featureValue), 
						getLabel(relationship)).toString();
			}
		} else if (changedObject instanceof SnomedConcreteDataTypeRefSetMember) {
			if (STATUS_FEATURE_NAME.equals(featureName)) {
				final String status = getBooleanValue(featureValue);
				if ("active".equals(status)) {
					//does it ever happen?
					return getDescription(changedObject, beforeView, currentView, "New ", "added to ");
				} else if ("inactive".equals(status)) {
					//akitta: we agreed to indicate incativation as deletion. 
					return getDescription(changedObject, beforeView, currentView, "Detached ", "detached from ");
				} else {
					return "Unknown change on status feature for '" + changedObject + "'.";
				}
			}
		} else if (changedObject instanceof SnomedComplexMapRefSetMember) {
			if (CORRELATION_ID_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), String.valueOf(featureValue), 
						getReferencedComponentLabel((SnomedComplexMapRefSetMember) changedObject)).toString();
			} else if (STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue), 
						getReferencedComponentLabel((SnomedComplexMapRefSetMember) changedObject)).toString();
			} else if (EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getReferencedComponentLabel((SnomedComplexMapRefSetMember) changedObject)).toString();
			} else if (MAP_GROUP_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), String.valueOf(featureValue), 
						getReferencedComponentLabel((SnomedComplexMapRefSetMember) changedObject)).toString();
			}
		} else if (changedObject instanceof SnomedRefSetMember) {
			if (DESCRIPTION_LENGTH_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), String.valueOf(featureValue), 
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (DESCRIPTION_FORMAT_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(changedObject.cdoView()), String.valueOf(featureValue)), 
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (STATUS_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), getBooleanValue(featureValue), 
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (RELEASED_FEATURE_NAME.equals(featureName)) {
				return getPublishedChange(featureName, builder, getReferencedComponentLabel((SnomedRefSetMember) changedObject), featureValue);
			} else if (VALUE_ID_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(changedObject.cdoView()), String.valueOf(featureValue)), 
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (MODULE_ID_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(changedObject.cdoView()), String.valueOf(featureValue)), 
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (SOURCE_EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();
			} else if (TARGET_EFFECTIVE_TIME_FEATURE_NAME.equals(featureName)) {
				return appendDescription(builder, getFeatureMapping().get(featureName), 
						DateFormat.getDateInstance().format(featureValue),
						getReferencedComponentLabel((SnomedRefSetMember) changedObject)).toString();

			} else if (ACCEPTABILITY_ID_FEATURE_NAME.equals(featureName)) {
				// IHTSDO is changing the acceptability of existing language members in case of PT changes on a concept. 
				// We have to show the change if it is changing to PREFERRED.
				if (Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED.equals(featureValue)) {
					return getRefSetChangeDescription((SnomedRefSetMember)changedObject, beforeView, currentView, "");
				} else {
					return null;
				}
			}
			
		} else if (changedObject instanceof SnomedMappingRefSet) {
			if (MAP_TARGET_TYPE_FEATURE_NAME.equals(featureName)) {
				final String label = getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(changedObject.cdoView()), ((SnomedMappingRefSet) changedObject).getIdentifierId());
				return appendDescription(builder, getFeatureMapping().get(featureName), getTerminologyComponentName(featureValue), label).toString();
			}
		} else if (changedObject instanceof SnomedRefSet) {
			if (RELEASED_FEATURE_NAME.equals(featureName)) {
				final String label = getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(changedObject.cdoView()), ((SnomedRefSet) changedObject).getIdentifierId());
				return getPublishedChange(featureName, builder, label, featureValue);
			}
		}
		return "Unexpected change on object: " + String.valueOf(changedObject) + " with the feature name of: '" + featureName + "'.";
	}

	private String getPublishedChange(final String featureName, final StringBuilder builder, final String label, final Object featureValue) {
		if ((boolean) featureValue) {
			return builder.append(label).append(" ").append(getFeatureMapping().get(featureName)).append(".").toString();
		} else {
			return null;
		}
	}

	private boolean isPtLanguageMember(final CDOObject member) {
		if (member instanceof SnomedLanguageRefSetMember) {
			final SnomedLanguageRefSetMember languageMember = (SnomedLanguageRefSetMember) member;
			if (isPreferred(languageMember)) {
				if (languageMember.eContainer() instanceof Description) {
					final Description description = (Description) languageMember.eContainer();
					if (canBePreferredDescription(description)) {
						return true;
					} 
				}
			}
		}
		return false;
	}

	private boolean canBePreferredDescription(final Description description) {
		final String typeId = description.getType().getId();
		return !Concepts.FULLY_SPECIFIED_NAME.equals(typeId) && !Concepts.TEXT_DEFINITION.equals(typeId);
	}
	
	private String getRefSetChangeDescription(final SnomedRefSetMember cdoObject, final CDOView beforeView, final CDOView currentView, final String change) {
		if (cdoObject instanceof SnomedRefSetMember) {
			final SnomedRefSetMember member = (SnomedRefSetMember) cdoObject;
			String label = getReferencedComponentLabel(member);
			
			if (isEmpty(label)) {
				label = member.getReferencedComponentId();
			}
			
			if (member instanceof SnomedSimpleMapRefSetMember) {
				return label + " " + change + getIdentifierConceptLabel(member) + "."; 
			} else if (member instanceof SnomedDescriptionTypeRefSetMember) {
				return label + " " + change + getIdentifierConceptLabel(member) + ".";
			} else if (member instanceof SnomedLanguageRefSetMember) {
				
				if (isPtLanguageMember(member)) {
					
					//ignore deletion
					if ("detached from ".equals(change)) {
						return null;
					}
					
					final SnomedLanguageRefSetMember languageMember = (SnomedLanguageRefSetMember) member;
					final Description description = (Description) languageMember.eContainer();
					final Concept concept = description.getConcept();
					final String refSetId = languageMember.getRefSetIdentifierId();
					final CDOObject beforeConcept = CDOUtils.getObjectIfExists(beforeView, concept.cdoID());
					final String previousPt = tryFindPreviousPtForLanguage(beforeConcept, refSetId);
					final String languageRefSetPt = getPreferredTerm(createPath(cdoObject), refSetId);

					if (null == previousPt) {
						return "New " + languageRefSetPt + " preferred term \"" + description.getTerm() + "\".";
					} else {
						return languageRefSetPt + " preferred term changed to \"" + description.getTerm() + "\" from \"" + previousPt + "\".";
					}
				}
				
				//intentionally null. we will ignore everything but the PT language changes
				return null;
			} else if (member instanceof SnomedAttributeValueRefSetMember) {	
				return label + " " + change + getIdentifierConceptLabel(member) + ".";
			}else if (member instanceof SnomedRefSetMember || member instanceof SnomedQueryRefSetMember) {
				return label + " " + change + getIdentifierConceptLabel(member) + ".";
			} else if (member instanceof SnomedConcreteDataTypeRefSetMember) {
				
			}
		}
		throw new IllegalArgumentException("Unsupported reference set member: " + cdoObject.getClass());
	}
	
	private String tryFindPreviousPtForLanguage(final CDOObject concept, final String refSetId) {

		if (concept instanceof Concept) {
			for (final Description description : ((Concept) concept).getDescriptions()) {
				if (description.isActive()) {
					if (canBePreferredDescription(description)) {
						for (final SnomedLanguageRefSetMember member : description.getLanguageRefSetMembers()) {
							if (refSetId.equals(member.getRefSetIdentifierId()) && member.isActive() && isPreferred(member)) {
								return description.getTerm();
							}
						}
					} 
				}
			}
		}
		
		return null;
	}

	private boolean isPreferred(final SnomedLanguageRefSetMember member) {
		return Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED.equals(member.getAcceptabilityId());
	}

	private String getConcreteDataTypeItem(final SnomedConcreteDataTypeRefSetMember cdtMember) {
		final StringBuilder builder = new StringBuilder();
		builder.append(cdtMember.getLabel());
		builder.append(" " + String.valueOf(cdtMember.getSerializedValue()));
		final String uomComponentId = cdtMember.getUomComponentId();
		if (null != uomComponentId) {
			builder.append(" " + getConceptNameProvider().getComponentLabel(BranchPathUtils.createPath(cdtMember.cdoView()), uomComponentId));
		}
		return builder.toString();
	}
	
	private ISnomedConceptNameProvider getConceptNameProvider() {
		return ApplicationContext.getServiceForClass(ISnomedConceptNameProvider.class);
	}

	private String getComponent(final CDOObject cdoObject) {
		if (isPtLanguageMember(cdoObject)) { //act as a concept change if the PT changed
			return CoreTerminologyBroker.getInstance().getComponentInformation(SnomedTerminologyComponentConstants.CONCEPT_NUMBER).getName();
		}
		return CoreTerminologyBroker.getInstance().getComponentInformation(cdoObject).getName();
	}
	
	private String getTerminologyComponentName(final Object featureValue) {
		final int value = Integer.parseInt(String.valueOf(featureValue));
		if (CoreTerminologyBroker.UNSPECIFIED_NUMBER == value) 
			return "Unspecified";
		
		final String terminologyComponentId = CoreTerminologyBroker.getInstance().getTerminologyComponentId(value);
		return CoreTerminologyBroker.getInstance().getComponentInformation(terminologyComponentId).getName();
	}

	private String getAttributeValueSeparator(final Relationship relationship) {
		return relationship.isDestinationNegated() ? " NOT " : " ";
	}

	private String getBooleanValue(final Object featureValue) {
		return getBooleanValue(featureValue, "active", "inactive");
	}

	private String getBooleanValue(final Object featureValue, final String trueMessage, final String falseMessage) {
		return Boolean.valueOf(featureValue.toString()) ? trueMessage : falseMessage;
	}
	
	private StringBuilder appendDescription(final StringBuilder builder, final String attributeName, final String newValue, final String changedOn) {
		builder.append("Attribute '");
		builder.append(attributeName);
		builder.append("' changed to ");
		builder.append(newValue);
		builder.append(" on ");
		builder.append(changedOn);
		builder.append(".");
		return builder;
	}
	
	private Map<String, String> getFeatureMapping() {
		if (null == map) {
			//can happen when this info details builder used from task compare editor
			//and multiple thread accessing this static map
			/*
			 * Example:
			 * I created two subtasks, classified, and on
			 * Task 1: Set Intravenous Infusion Liquid to primitive
			 * Task 2: Set Intravenous Infusion Solution to primitive

			 * Marked tasks as FIXED.
			 * The compare tasks.

			 * But the first one says "Attribute 'null' changed to Primitive...
			 * (it should be 'definition status' rather than null.)
			 * */
			synchronized (SnomedConceptHistoryInfoDetailsBuilder.class) {
				if (null == map) {
					map = new HashMap<String, String>();
					map.put(CASE_SIGNIFICANCE_FEATURE_NAME, "case significance");
					map.put(STATUS_FEATURE_NAME, "status");
					map.put(VALUE_FEATURE_NAME, "has strength");
					map.put(OPERATOR_TYPE_FEATURE_NAME, "operator type");
					map.put(UNIT_TYPE_FEATURE_NAME, "unit type");
			 		map.put(DEFINITION_STATUS_FEATURE_NAME, "definition status");
					map.put(EFFECTIVE_TIME_FEATURE_NAME, "effective time");
					map.put(EXHAUSTIVE_FEATURE_NAME, "subclass definitions");
					map.put(MODULE_FEATURE_NAME, "module");
//					map.put(RELATIONSHIP_TYPE_FEATURE_NAME, "relationship type"); //XXX intentionally removed due to description type feature collision
					map.put(MODULE_ID_FEATURE_NAME, "module");
					map.put(MAP_TARGET_TYPE_FEATURE_NAME, "map target");
					map.put(DESCRIPTION_TYPE_FEATURE_NAME, "description type");
					map.put(DESCRIPTION_TERM_FEATURE_NAME, "description term");
					map.put(GROUP_FEATURE_NAME, "relationship group");
					map.put(UNION_GROUP_FEATURE_NAME, "relationship union group");
					map.put(CHARACTERISTIC_TYPE_FEATURE_NAME, "characteristic type");
					map.put(MODIFIER_FEATURE_NAME, "modifier");
					map.put(CORRELATION_ID_FEATURE_NAME, "correlation identifier");
					map.put(MAP_GROUP_FEATURE_NAME, "map group");
					map.put(RELEASED_FEATURE_NAME, "published");
					map.put(DESCRIPTION_LENGTH_FEATURE_NAME, "description length");
					map.put(DESCRIPTION_FORMAT_FEATURE_NAME, "description format");
					map.put(VALUE_ID_FEATURE_NAME, "value");
					map.put(SOURCE_EFFECTIVE_TIME_FEATURE_NAME, "source effective time");
					map.put(TARGET_EFFECTIVE_TIME_FEATURE_NAME, "target effective time");
				}
			}
		}
		return Collections.unmodifiableMap(map);
	}
	
	private String getPreferredTerm(final Concept concept) {
		final String label = getPreferredTerm(createPath(concept), concept.getId());
		return isEmpty(label) ? concept.getFullySpecifiedName() : label; 
	}

	private String getPreferredTerm(final IBranchPath branchPath, final String id) {
		return getServiceForClass(ISnomedConceptNameProvider.class).getComponentLabel(branchPath, id);
	}
	
	private String getReferencedComponentLabel(final SnomedRefSetMember member) {

		final InternalCDORevision revision = (InternalCDORevision) member.cdoRevision();
		final IBranchPath branchPath = createPath(member);
		final String id = String.valueOf(revision.get(SnomedRefSetPackage.eINSTANCE.getSnomedRefSetMember_ReferencedComponentId(), CDOStore.NO_INDEX));
		final short referencedComponentType = SnomedTerminologyComponentConstants.getTerminologyComponentIdValue(id);
		switch (referencedComponentType) {
			case SnomedTerminologyComponentConstants.CONCEPT_NUMBER: //$FALL-THROUGH$
			case SnomedTerminologyComponentConstants.REFSET_MEMBER_NUMBER:
				return getPreferredTerm(branchPath, id);
			case SnomedTerminologyComponentConstants.DESCRIPTION_NUMBER: //$FALL-THROUGH$
				return getServiceForClass(ISnomedDescriptionNameProvider.class).getComponentLabel(branchPath, id);
			case SnomedTerminologyComponentConstants.RELATIONSHIP_NUMBER:
				final String terminologyComponentId = CoreTerminologyBroker.getInstance().getTerminologyComponentId(referencedComponentType);
				return CoreTerminologyBroker.getInstance().getNameProviderFactory(terminologyComponentId).getNameProvider().getComponentLabel(createPath(member), id);
			default:
				throw new IllegalArgumentException("Unexpected or unknown terminology component type: " + referencedComponentType);
		}
	}

	private String getLabel(final SnomedRefSet refSet) {
		return getPreferredTerm(createPath(refSet), refSet.getIdentifierId());
	}
	
	private String getIdentifierConceptLabel(final SnomedRefSetMember member) {
		final IBranchPath branchPath = createPath(member.cdoView());
		final String refSetIdentifierId = member.getRefSetIdentifierId();
		final Pair<IBranchPath, String> pair = identicalPairOf(branchPath, refSetIdentifierId);
		final String label = idLabelCache.getUnchecked(pair);
		if (refSetIdentifierId.equals(label)) {
			return "deleted reference set " + label;
		}
		return label;
	}
	
	private String getLabel(final Relationship relationship) {
		
		return getPreferredTerm(relationship.getSource()) 
				+ " " 
				+ getPreferredTerm(relationship.getType()) 
				+ getAttributeValueSeparator(relationship) 
				+ getPreferredTerm(relationship.getDestination());
	}
}