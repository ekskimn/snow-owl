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
package com.b2international.snowowl.snomed.datastore.index.entry;

import static com.b2international.index.query.Expressions.exactMatch;
import static com.b2international.index.query.Expressions.matchAny;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.CONCEPT_NUMBER;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.DESCRIPTION_NUMBER;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.RELATIONSHIP_NUMBER;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

<<<<<<< HEAD
=======
import java.io.Serializable;
>>>>>>> origin/ms-develop
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.b2international.commons.StringUtils;
import com.b2international.index.Doc;
import com.b2international.index.query.Expression;
import com.b2international.snowowl.core.CoreTerminologyBroker;
<<<<<<< HEAD
=======
import com.b2international.snowowl.core.api.IComponent;
>>>>>>> origin/ms-develop
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.cdo.CDOIDUtils;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.core.domain.RelationshipRefinability;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedCoreComponent;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSetMember;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetUtil;
import com.b2international.snowowl.snomed.snomedrefset.DataType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedAssociationRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedAttributeValueRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedComplexMapRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedConcreteDataTypeRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedDescriptionTypeRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedLanguageRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedModuleDependencyRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedQueryRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedSimpleMapRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.util.SnomedRefSetSwitch;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Function;
<<<<<<< HEAD
import com.google.common.base.Objects.ToStringHelper;
=======
import com.google.common.base.Functions;
import com.google.common.base.Optional;
>>>>>>> origin/ms-develop
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * Lightweight representation of a SNOMED CT reference set member.
 */
<<<<<<< HEAD
@Doc
@JsonDeserialize(builder = SnomedRefSetMemberIndexEntry.Builder.class)
public final class SnomedRefSetMemberIndexEntry extends SnomedDocument {

	private static final long serialVersionUID = 5198766293865046258L;

	public static class Fields {
		// known RF2 fields
		public static final String REFERENCE_SET_ID = "referenceSetId"; // XXX different than the RF2 header field name
		public static final String REFERENCED_COMPONENT_ID = SnomedRf2Headers.FIELD_REFERENCED_COMPONENT_ID;
		public static final String ACCEPTABILITY_ID = SnomedRf2Headers.FIELD_ACCEPTABILITY_ID;
		public static final String VALUE_ID = SnomedRf2Headers.FIELD_VALUE_ID;
		public static final String TARGET_COMPONENT = SnomedRf2Headers.FIELD_TARGET_COMPONENT;
		public static final String MAP_TARGET = SnomedRf2Headers.FIELD_MAP_TARGET;
		public static final String MAP_TARGET_DESCRIPTION = SnomedRf2Headers.FIELD_MAP_TARGET_DESCRIPTION;
		public static final String MAP_GROUP = SnomedRf2Headers.FIELD_MAP_GROUP;
		public static final String MAP_PRIORITY = SnomedRf2Headers.FIELD_MAP_PRIORITY;
		public static final String MAP_RULE = SnomedRf2Headers.FIELD_MAP_RULE;
		public static final String MAP_ADVICE = SnomedRf2Headers.FIELD_MAP_ADVICE;
		public static final String MAP_CATEGORY_ID = SnomedRf2Headers.FIELD_MAP_CATEGORY_ID;
		public static final String CORRELATION_ID = SnomedRf2Headers.FIELD_CORRELATION_ID;
		public static final String DESCRIPTION_FORMAT = SnomedRf2Headers.FIELD_DESCRIPTION_FORMAT;
		public static final String DESCRIPTION_LENGTH = SnomedRf2Headers.FIELD_DESCRIPTION_LENGTH;
		public static final String OPERATOR_ID = SnomedRf2Headers.FIELD_OPERATOR_ID;
		public static final String UNIT_ID = SnomedRf2Headers.FIELD_UNIT_ID;
		public static final String QUERY = SnomedRf2Headers.FIELD_QUERY;
		public static final String CHARACTERISTIC_TYPE_ID = SnomedRf2Headers.FIELD_CHARACTERISTIC_TYPE_ID;
		public static final String SOURCE_EFFECTIVE_TIME = SnomedRf2Headers.FIELD_SOURCE_EFFECTIVE_TIME;
		public static final String TARGET_EFFECTIVE_TIME = SnomedRf2Headers.FIELD_TARGET_EFFECTIVE_TIME;
		public static final String DATA_VALUE = SnomedRf2Headers.FIELD_VALUE;
		public static final String ATTRIBUTE_NAME = SnomedRf2Headers.FIELD_ATTRIBUTE_NAME;
		// extra index fields to store datatype and map target type
		public static final String DATA_TYPE = "dataType";
		public static final String REFSET_TYPE = "referenceSetType";
		public static final String REFERENCED_COMPONENT_TYPE = "referencedComponentType";
=======
public class SnomedRefSetMemberIndexEntry extends SnomedIndexEntry implements IComponent<String>, Serializable {

	private static final String EMPTY_STRING = "";
	
	private static final Set<String> ADDITIONAL_FIELDS = SnomedMappings.fieldsToLoad()
			// Simple type: 
			// (no additional fields)
			// Language type:
			.memberAcceptabilityId()				// long
			// Attribute value type:
			.memberValueId()						// String
			// Association type:
			.memberTargetComponentId()				// String
			// Simple map type:
			.memberMapTargetComponentId()			// String
			.memberMapTargetComponentDescription()	// String	(optional)
			// Complex/Extended map type:
			.memberMapGroup()						// int
			.memberMapPriority()					// int
			.memberMapRule()						// String	(optional)
			.memberMapAdvice()						// String	(optional)
			.memberCorrelationId()					// long
			.memberMapCategoryId()					// long		(extended map type only)
			// Description format type:
			.memberDescriptionFormatId()			// long
			.memberDescriptionLength()				// int
			// Concrete domain type:
			.memberOperatorId()						// long
			.memberUomId()							// long		(optional)
			.memberDataTypeLabel()					// String
			.memberDataTypeOrdinal()				// int
			.memberSerializedValue()				// String
			.memberCharacteristicTypeId()			// long		(optional)
			// Query type:
			.memberQuery()							// String
			// Module dependency type:
			.memberSourceEffectiveTime()			// long
			.memberTargetEffectiveTime()			// long
			.build();

	/**
	 * @param name the field name to check
	 * @return {@code true} if the specified field name is valid as an additional {@code String} or {@link Number} value, {@code false} otherwise
	 */
	public static boolean isAdditionalField(final String name) {
		return ADDITIONAL_FIELDS.contains(name);
>>>>>>> origin/ms-develop
	}
	
		public static Builder builder() {
		return new Builder();
	}
	
	public static Builder builder(final SnomedRefSetMemberIndexEntry source) {
		return builder()
				.storageKey(source.getStorageKey())
				.active(source.isActive())
				.effectiveTime(source.getEffectiveTime())
				.id(source.getId())
				.moduleId(source.getModuleId())
				.referencedComponentId(source.getReferencedComponentId())
				.referencedComponentType(source.getReferencedComponentType())
				.referenceSetId(source.getReferenceSetId())
				.referenceSetType(source.getReferenceSetType())
				.released(source.isReleased())
				.fields(source.getAdditionalFields());
	}
	
	public static final Builder builder(final SnomedReferenceSetMember input) {
		final Builder builder = builder()
				.storageKey(input.getStorageKey())
				.active(input.isActive())
				.effectiveTime(EffectiveTimes.getEffectiveTime(input.getEffectiveTime()))
				.id(input.getId())
				.moduleId(input.getModuleId())
				.referencedComponentId(input.getReferencedComponent().getId())
				.referenceSetId(input.getReferenceSetId())
				.referenceSetType(input.type())
				.released(input.isReleased());
		
		if (input.getReferencedComponent() instanceof SnomedConcept) {
			builder.referencedComponentType(CONCEPT_NUMBER);
		} else if (input.getReferencedComponent() instanceof SnomedDescription) {
			builder.referencedComponentType(DESCRIPTION_NUMBER);
		} else if (input.getReferencedComponent() instanceof SnomedRelationship) {
			builder.referencedComponentType(RELATIONSHIP_NUMBER);
		} else {
			builder.referencedComponentType(CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT);
		}
		
		
		for (Entry<String, Object> entry : input.getProperties().entrySet()) {
			final Object value = entry.getValue();
			final String fieldName = entry.getKey();
			// certain RF2 fields can be expanded into full blown representation class, get the ID in this case
			if (value instanceof SnomedCoreComponent) {
				builder.field(fieldName, ((SnomedCoreComponent) value).getId());
			} else {
				builder.field(fieldName, convertValue(entry.getKey(), value));
			}
		}
		
		return builder;
	}
	
	public static Builder builder(SnomedRefSetMember refSetMember) {
		final Builder builder = SnomedRefSetMemberIndexEntry.builder()
				.storageKey(CDOIDUtils.asLong(refSetMember.cdoID()))
				.id(refSetMember.getUuid()) 
				.moduleId(refSetMember.getModuleId())
				.active(refSetMember.isActive())
				.released(refSetMember.isReleased())
				.effectiveTime(refSetMember.isSetEffectiveTime() ? refSetMember.getEffectiveTime().getTime() : EffectiveTimes.UNSET_EFFECTIVE_TIME)
				.referenceSetId(refSetMember.getRefSetIdentifierId())
				.referenceSetType(refSetMember.getRefSet().getType())
				.referencedComponentType(refSetMember.getReferencedComponentType())
				.referencedComponentId(refSetMember.getReferencedComponentId());

		return new SnomedRefSetSwitch<Builder>() {

			@Override
<<<<<<< HEAD
			public Builder caseSnomedAssociationRefSetMember(final SnomedAssociationRefSetMember associationMember) {
				return builder.targetComponent(associationMember.getTargetComponentId());
			}

			@Override
			public Builder caseSnomedAttributeValueRefSetMember(final SnomedAttributeValueRefSetMember attributeValueMember) {
				return builder.field(Fields.VALUE_ID, attributeValueMember.getValueId());
			}

			@Override
			public Builder caseSnomedConcreteDataTypeRefSetMember(final SnomedConcreteDataTypeRefSetMember concreteDataTypeMember) {
				return builder.field(Fields.ATTRIBUTE_NAME, concreteDataTypeMember.getLabel())
						.field(Fields.DATA_TYPE, concreteDataTypeMember.getDataType())
						.field(Fields.DATA_VALUE, concreteDataTypeMember.getSerializedValue())
						.field(Fields.CHARACTERISTIC_TYPE_ID, concreteDataTypeMember.getCharacteristicTypeId())
						.field(Fields.OPERATOR_ID, concreteDataTypeMember.getOperatorComponentId())
						.field(Fields.UNIT_ID, Strings.nullToEmpty(concreteDataTypeMember.getUomComponentId()));
			}

			@Override
			public Builder caseSnomedDescriptionTypeRefSetMember(final SnomedDescriptionTypeRefSetMember descriptionTypeMember) {
				return builder
						.field(Fields.DESCRIPTION_FORMAT, descriptionTypeMember.getDescriptionFormat())
						.field(Fields.DESCRIPTION_LENGTH, descriptionTypeMember.getDescriptionLength());
			}

			@Override
=======
			public Builder caseSnomedRefSetMember(SnomedRefSetMember object) {
				return builder;
			}

			@Override
>>>>>>> origin/ms-develop
			public Builder caseSnomedLanguageRefSetMember(final SnomedLanguageRefSetMember languageMember) {
				return builder.field(Fields.ACCEPTABILITY_ID, languageMember.getAcceptabilityId());
			}

			@Override
<<<<<<< HEAD
			public Builder caseSnomedModuleDependencyRefSetMember(final SnomedModuleDependencyRefSetMember moduleDependencyMember) {
				return builder
						.field(Fields.SOURCE_EFFECTIVE_TIME, EffectiveTimes.getEffectiveTime(moduleDependencyMember.getSourceEffectiveTime()))
						.field(Fields.TARGET_EFFECTIVE_TIME, EffectiveTimes.getEffectiveTime(moduleDependencyMember.getTargetEffectiveTime()));
			}

			@Override
			public Builder caseSnomedQueryRefSetMember(final SnomedQueryRefSetMember queryMember) {
				return builder.field(Fields.QUERY, queryMember.getQuery());
=======
			public Builder caseSnomedAttributeValueRefSetMember(final SnomedAttributeValueRefSetMember attributeValueMember) {
				return builder.additionalField(SnomedMappings.memberValueId().fieldName(), attributeValueMember.getValueId());
			}

			@Override
			public Builder caseSnomedAssociationRefSetMember(final SnomedAssociationRefSetMember associationMember) {
				return builder.additionalField(SnomedMappings.memberTargetComponentId().fieldName(), associationMember.getTargetComponentId());
>>>>>>> origin/ms-develop
			}

			@Override
			public Builder caseSnomedSimpleMapRefSetMember(final SnomedSimpleMapRefSetMember mapRefSetMember) {
<<<<<<< HEAD
				return builder
						.field(Fields.MAP_TARGET, mapRefSetMember.getMapTargetComponentId())
						.field(Fields.MAP_TARGET_DESCRIPTION, Strings.nullToEmpty(mapRefSetMember.getMapTargetComponentDescription()));
=======
				builder.mapTargetComponentType(mapRefSetMember.getMapTargetComponentType());
				builder.additionalField(SnomedMappings.memberMapTargetComponentId().fieldName(), mapRefSetMember.getMapTargetComponentId());
			
				if (mapRefSetMember.getMapTargetComponentDescription() != null) {
					builder.additionalField(SnomedMappings.memberMapTargetComponentDescription().fieldName(), mapRefSetMember.getMapTargetComponentDescription());
				}
			
				return builder;
			}

			@Override
			public Builder caseSnomedComplexMapRefSetMember(final SnomedComplexMapRefSetMember complexMapRefSetMember) {
				builder.mapTargetComponentType(complexMapRefSetMember.getMapTargetComponentType());
				builder.additionalField(SnomedMappings.memberMapTargetComponentId().fieldName(), complexMapRefSetMember.getMapTargetComponentId());
				
				builder.additionalField(SnomedMappings.memberMapGroup().fieldName(), Integer.valueOf(complexMapRefSetMember.getMapGroup()));
				builder.additionalField(SnomedMappings.memberMapPriority().fieldName(), Integer.valueOf(complexMapRefSetMember.getMapPriority()));
				builder.additionalField(SnomedMappings.memberCorrelationId().fieldName(), Long.valueOf(complexMapRefSetMember.getCorrelationId()));
				
				if (complexMapRefSetMember.getMapRule() != null) {
					builder.additionalField(SnomedMappings.memberMapRule().fieldName(), complexMapRefSetMember.getMapRule());
				}
				
				if (complexMapRefSetMember.getMapAdvice() != null) {
					builder.additionalField(SnomedMappings.memberMapAdvice().fieldName(), complexMapRefSetMember.getMapAdvice());
				}
				
				if (complexMapRefSetMember.getMapCategoryId() != null) {
					builder.additionalField(SnomedMappings.memberMapCategoryId().fieldName(), Long.valueOf(complexMapRefSetMember.getMapCategoryId()));	
				}
				
				return builder;
>>>>>>> origin/ms-develop
			}

			@Override
<<<<<<< HEAD
			public Builder caseSnomedComplexMapRefSetMember(final SnomedComplexMapRefSetMember mapRefSetMember) {
				return builder
						.field(Fields.MAP_TARGET, mapRefSetMember.getMapTargetComponentId())
						.field(Fields.CORRELATION_ID, mapRefSetMember.getCorrelationId())
						.field(Fields.MAP_GROUP, Integer.valueOf(mapRefSetMember.getMapGroup()))
						.field(Fields.MAP_ADVICE, Strings.nullToEmpty(mapRefSetMember.getMapAdvice()))
						.field(Fields.MAP_PRIORITY, Integer.valueOf(mapRefSetMember.getMapPriority()))
						.field(Fields.MAP_RULE, Strings.nullToEmpty(mapRefSetMember.getMapRule()))
						// extended refset
						.field(Fields.MAP_CATEGORY_ID, Strings.nullToEmpty(mapRefSetMember.getMapCategoryId()));
=======
			public Builder caseSnomedDescriptionTypeRefSetMember(final SnomedDescriptionTypeRefSetMember descriptionTypeMember) {
				return builder
						.additionalField(SnomedMappings.memberDescriptionFormatId().fieldName(), Long.valueOf(descriptionTypeMember.getDescriptionFormat()))
						.additionalField(SnomedMappings.memberDescriptionLength().fieldName(), Integer.valueOf(descriptionTypeMember.getDescriptionLength()));
			}

			@Override
			public Builder caseSnomedConcreteDataTypeRefSetMember(final SnomedConcreteDataTypeRefSetMember concreteDataTypeMember) {
				builder.additionalField(SnomedMappings.memberDataTypeLabel().fieldName(), concreteDataTypeMember.getLabel())
						.additionalField(SnomedMappings.memberDataTypeOrdinal().fieldName(), concreteDataTypeMember.getDataType().ordinal())
						.additionalField(SnomedMappings.memberSerializedValue().fieldName(), concreteDataTypeMember.getSerializedValue())
						.additionalField(SnomedMappings.memberOperatorId().fieldName(), Long.valueOf(concreteDataTypeMember.getOperatorComponentId()));

				if (null != concreteDataTypeMember.getCharacteristicTypeId()) {
					builder.additionalField(SnomedMappings.memberCharacteristicTypeId().fieldName(), Long.valueOf(concreteDataTypeMember.getCharacteristicTypeId()));
				}
				
				if (!Strings.isNullOrEmpty(concreteDataTypeMember.getUomComponentId())) {
					builder.additionalField(SnomedMappings.memberUomId().fieldName(), Long.valueOf(concreteDataTypeMember.getUomComponentId()));
				}

				return builder;
>>>>>>> origin/ms-develop
			}

			@Override
			public Builder caseSnomedQueryRefSetMember(final SnomedQueryRefSetMember queryMember) {
				return builder.additionalField(SnomedMappings.memberQuery().fieldName(), queryMember.getQuery());
			}

			@Override
			public Builder caseSnomedModuleDependencyRefSetMember(final SnomedModuleDependencyRefSetMember moduleDependencyMember) {
				return builder
						.additionalField(SnomedMappings.memberSourceEffectiveTime().fieldName(), EffectiveTimes.getEffectiveTime(moduleDependencyMember.getSourceEffectiveTime()))
						.additionalField(SnomedMappings.memberTargetEffectiveTime().fieldName(), EffectiveTimes.getEffectiveTime(moduleDependencyMember.getTargetEffectiveTime()));
			};

		}.doSwitch(refSetMember);
	}
	
<<<<<<< HEAD
	private static Object convertValue(String rf2Field, Object value) {
		switch (rf2Field) {
=======
	/*Converts RF2 field names to their index field equivalents*/
	private static String getIndexFieldName(String rf2Field) {
		switch (rf2Field) {
		case SnomedRf2Headers.FIELD_ACCEPTABILITY_ID: return SnomedMappings.memberAcceptabilityId().fieldName();
		case SnomedRf2Headers.FIELD_ATTRIBUTE_NAME: return SnomedMappings.memberDataTypeLabel().fieldName();
		case SnomedRf2Headers.FIELD_CHARACTERISTIC_TYPE_ID: return SnomedMappings.memberCharacteristicTypeId().fieldName();
		case SnomedRf2Headers.FIELD_CORRELATION_ID: return SnomedMappings.memberCorrelationId().fieldName();
		case SnomedRf2Headers.FIELD_DESCRIPTION_FORMAT: return SnomedMappings.memberDescriptionFormatId().fieldName();
		case SnomedRf2Headers.FIELD_DESCRIPTION_LENGTH: return SnomedMappings.memberDescriptionLength().fieldName();
		case SnomedRf2Headers.FIELD_MAP_ADVICE: return SnomedMappings.memberMapAdvice().fieldName();
		case SnomedRf2Headers.FIELD_MAP_CATEGORY_ID: return SnomedMappings.memberMapCategoryId().fieldName();
		case SnomedRf2Headers.FIELD_MAP_GROUP: return SnomedMappings.memberMapGroup().fieldName();
		case SnomedRf2Headers.FIELD_MAP_PRIORITY: return SnomedMappings.memberMapPriority().fieldName();
		case SnomedRf2Headers.FIELD_MAP_RULE: return SnomedMappings.memberMapRule().fieldName();
		case SnomedRf2Headers.FIELD_MAP_TARGET: return SnomedMappings.memberMapTargetComponentId().fieldName();
		case SnomedRf2Headers.FIELD_MAP_TARGET_DESCRIPTION: return SnomedMappings.memberMapTargetComponentDescription().fieldName();
		case SnomedRf2Headers.FIELD_OPERATOR_ID: return SnomedMappings.memberOperatorId().fieldName();
		case SnomedRf2Headers.FIELD_QUERY: return SnomedMappings.memberQuery().fieldName();
		case SnomedRf2Headers.FIELD_SOURCE_EFFECTIVE_TIME: return SnomedMappings.memberSourceEffectiveTime().fieldName();
		case SnomedRf2Headers.FIELD_TARGET_COMPONENT: return SnomedMappings.memberTargetComponentId().fieldName();
		case SnomedRf2Headers.FIELD_TARGET_EFFECTIVE_TIME: return SnomedMappings.memberTargetEffectiveTime().fieldName();
		case SnomedRf2Headers.FIELD_UNIT_ID: return SnomedMappings.memberUomId().fieldName();
		case SnomedRf2Headers.FIELD_VALUE: return SnomedMappings.memberSerializedValue().fieldName();
		case SnomedRf2Headers.FIELD_VALUE_ID: return SnomedMappings.memberValueId().fieldName();
		default: return rf2Field;
		}
	}
	
	private static Object convertValue(String rf2Field, Object value) {
		switch (rf2Field) {
		case SnomedRf2Headers.FIELD_ACCEPTABILITY_ID:
		case SnomedRf2Headers.FIELD_CORRELATION_ID:
		case SnomedRf2Headers.FIELD_MAP_CATEGORY_ID:
		case SnomedRf2Headers.FIELD_DESCRIPTION_FORMAT:
		case SnomedRf2Headers.FIELD_OPERATOR_ID:
		case SnomedRf2Headers.FIELD_UNIT_ID:
		case SnomedRf2Headers.FIELD_CHARACTERISTIC_TYPE_ID:
>>>>>>> origin/ms-develop
		case SnomedRf2Headers.FIELD_SOURCE_EFFECTIVE_TIME:
		case SnomedRf2Headers.FIELD_TARGET_EFFECTIVE_TIME:
			if (value instanceof String && !StringUtils.isEmpty((String) value)) {
				return Long.valueOf((String) value);
			}
		default: return value;
		}
	}

	public static Collection<SnomedRefSetMemberIndexEntry> from(final Iterable<SnomedReferenceSetMember> refSetMembers) {
		return FluentIterable.from(refSetMembers).transform(new Function<SnomedReferenceSetMember, SnomedRefSetMemberIndexEntry>() {
			@Override
			public SnomedRefSetMemberIndexEntry apply(final SnomedReferenceSetMember refSetMember) {
				return builder(refSetMember).build();
			}
		}).toList();
	}

	public static final class Expressions extends SnomedDocument.Expressions {
		
		public static Expression referenceSetId(String referenceSetId) {
			return exactMatch(Fields.REFERENCE_SET_ID, referenceSetId);
		}

		public static Expression referenceSetId(Collection<String> referenceSetIds) {
			return matchAny(Fields.REFERENCE_SET_ID, referenceSetIds);
		}
		
		public static Expression referencedComponentId(String referencedComponentId) {
			return exactMatch(Fields.REFERENCED_COMPONENT_ID, referencedComponentId);
		}

		public static Expression referencedComponentIds(Collection<String> referencedComponentIds) {
			return matchAny(Fields.REFERENCED_COMPONENT_ID, referencedComponentIds);
		}
		
		public static Expression targetComponents(Collection<String> targetComponentIds) {
			return matchAny(Fields.TARGET_COMPONENT, targetComponentIds);
		}
		
		public static Expression acceptabilityIds(Collection<String> acceptabilityIds) {
			return matchAny(Fields.ACCEPTABILITY_ID, acceptabilityIds);
		}
		
		public static Expression characteristicTypeIds(Collection<String> characteristicTypeIds) {
			return matchAny(Fields.CHARACTERISTIC_TYPE_ID, characteristicTypeIds);
		}
		
		public static Expression correlationIds(Collection<String> correlationIds) {
			return matchAny(Fields.CORRELATION_ID, correlationIds);
		}
		
		public static Expression descriptionFormats(Collection<String> descriptionFormats) {
			return matchAny(Fields.DESCRIPTION_FORMAT, descriptionFormats);
		}
		
		public static Expression mapCategoryIds(Collection<String> mapCategoryIds) {
			return matchAny(Fields.MAP_CATEGORY_ID, mapCategoryIds);
		}
		
		public static Expression operatorIds(Collection<String> operatorIds) {
			return matchAny(Fields.OPERATOR_ID, operatorIds);
		}
		
		public static Expression unitIds(Collection<String> unitIds) {
			return matchAny(Fields.UNIT_ID, unitIds);
		}
		
		public static Expression valueIds(Collection<String> valueIds) {
			return matchAny(Fields.VALUE_ID, valueIds);
		}
		
		public static Expression sourceEffectiveTime(long effectiveTime) {
			return exactMatch(Fields.SOURCE_EFFECTIVE_TIME, effectiveTime);
		}
		
		public static Expression targetEffectiveTime(long effectiveTime) {
			return exactMatch(Fields.TARGET_EFFECTIVE_TIME, effectiveTime);
		}
		
		public static Expression refSetTypes(Collection<SnomedRefSetType> refSetTypes) {
			return matchAny(Fields.REFSET_TYPE, FluentIterable.from(refSetTypes).transform(new Function<SnomedRefSetType, String>() {
				@Override
				public String apply(SnomedRefSetType input) {
					return input.name();
				}
			}).toSet());
		}
		
	}

	@JsonPOJOBuilder(withPrefix="")
	public static final class Builder extends SnomedDocumentBuilder<Builder> {

		private String referencedComponentId;

		private String referenceSetId;
		private SnomedRefSetType referenceSetType;
		private short referencedComponentType;

		// Member specific fields, they can be null or emptyish values
		// ASSOCIATION reference set members
		private String targetComponent;
		// ATTRIBUTE VALUE
		private String valueId;
		// CONCRETE DOMAIN reference set members
		private DataType dataType;
		private String attributeName;
		private String value;
		private String operatorId;
		private String characteristicTypeId;
		private String unitId;
		// DESCRIPTION
		private Integer descriptionLength;
		private String descriptionFormat;
		// LANGUAGE
		private String acceptabilityId;
		// MODULE
		private Long sourceEffectiveTime;
		private Long targetEffectiveTime;
		// SIMPLE MAP reference set members
		private String mapTarget;
		private String mapTargetDescription;
		// COMPLEX MAP
		private String mapCategoryId;
		private String correlationId;
		private String mapAdvice;
		private String mapRule;
		private Integer mapGroup;
		private Integer mapPriority;
		// QUERY
		private String query;

		@JsonCreator
		private Builder() {
			// Disallow instantiation outside static method
		}

		Builder fields(Map<String, Object> fields) {
			for (Entry<String, Object> entry : fields.entrySet()) {
				field(entry.getKey(), entry.getValue());
			}
			return this;
		}
		
		Builder field(String fieldName, Object value) {
			switch (fieldName) {
			case Fields.ACCEPTABILITY_ID: this.acceptabilityId = (String) value; break;
			case Fields.ATTRIBUTE_NAME: this.attributeName = (String) value; break;
			case Fields.CHARACTERISTIC_TYPE_ID: this.characteristicTypeId = (String) value; break;
			case Fields.CORRELATION_ID: this.correlationId = (String) value; break;
			case Fields.DATA_TYPE: this.dataType = (DataType) value; break;
			case Fields.DATA_VALUE: this.value = (String) value; break;
			case Fields.DESCRIPTION_FORMAT: this.descriptionFormat = (String) value; break;
			case Fields.DESCRIPTION_LENGTH: this.descriptionLength = (Integer) value; break;
			case Fields.MAP_ADVICE: this.mapAdvice = (String) value; break;
			case Fields.MAP_CATEGORY_ID: this.mapCategoryId = (String) value; break;
			case Fields.MAP_GROUP: this.mapGroup = (Integer) value; break;
			case Fields.MAP_PRIORITY: this.mapPriority = (Integer) value; break;
			case Fields.MAP_RULE: this.mapRule = (String) value; break;
			case Fields.MAP_TARGET: this.mapTarget = (String) value; break;
			case Fields.MAP_TARGET_DESCRIPTION: this.mapTargetDescription = (String) value; break;
			case Fields.OPERATOR_ID: this.operatorId = (String) value; break;
			case Fields.QUERY: this.query = (String) value; break;
			case Fields.SOURCE_EFFECTIVE_TIME: this.sourceEffectiveTime = (Long) value; break;
			case Fields.TARGET_COMPONENT: this.targetComponent = (String) value; break;
			case Fields.TARGET_EFFECTIVE_TIME: this.targetEffectiveTime = (Long) value; break;
			case Fields.UNIT_ID: this.unitId = (String) value; break;
			case Fields.VALUE_ID: this.valueId = (String) value; break;
			default: throw new UnsupportedOperationException("Unknown RF2 member field: " + fieldName);
			}
			return this;
		}

		@Override
		protected Builder getSelf() {
			return this;
		}

		public Builder referencedComponentId(final String referencedComponentId) {
			this.referencedComponentId = referencedComponentId;
			return this;
		}

		public Builder referenceSetId(final String referenceSetId) {
			this.referenceSetId = referenceSetId;
			return this;
		}

		public Builder referenceSetType(final SnomedRefSetType referenceSetType) {
			this.referenceSetType = referenceSetType;
			return this;
		}

		public Builder referencedComponentType(final short referencedComponentType) {
			this.referencedComponentType = referencedComponentType;
			return this;
		}
		
		public Builder targetComponent(String targetComponent) {
			this.targetComponent = targetComponent;
			return this;
		}
		
		Builder acceptabilityId(String acceptabilityId) {
			this.acceptabilityId = acceptabilityId;
			return getSelf();
		}
		
		Builder attributeName(String attributeName) {
			this.attributeName = attributeName;
			return getSelf();
		}
		
		Builder characteristicTypeId(final String characteristicTypeId) {
			this.characteristicTypeId = characteristicTypeId;
			return getSelf();
		}
		
		Builder correlationId(final String correlationId) {
			this.correlationId = correlationId;
			return getSelf();
		}
		
		Builder dataType(final DataType dataType) {
			this.dataType = dataType;
			return getSelf();
		}
		
		Builder descriptionFormat(final String descriptionFormat) {
			this.descriptionFormat = descriptionFormat;
			return getSelf();
		}
		
		Builder descriptionLength(final Integer descriptionLength) {
			this.descriptionLength = descriptionLength;
			return getSelf();
		}
		
		Builder mapAdvice(final String mapAdvice) {
			this.mapAdvice = mapAdvice;
			return getSelf();
		}
		
		Builder mapCategoryId(final String mapCategoryId) {
			this.mapCategoryId = mapCategoryId;
			return getSelf();
		}
		
		Builder mapGroup(final Integer mapGroup) {
			this.mapGroup = mapGroup;
			return getSelf();
		}
		
		Builder mapPriority(final Integer mapPriority) {
			this.mapPriority = mapPriority;
			return getSelf();
		}
		
		Builder mapRule(final String mapRule) {
			this.mapRule = mapRule;
			return getSelf();
		}
		
		Builder mapTarget(final String mapTarget) {
			this.mapTarget = mapTarget;
			return getSelf();
		}
		
		Builder mapTargetDescription(final String mapTargetDescription) {
			this.mapTargetDescription = mapTargetDescription;
			return getSelf();
		}
		
		Builder operatorId(final String operatorId) {
			this.operatorId = operatorId;
			return getSelf();
		}
		
		Builder query(final String query) {
			this.query = query;
			return getSelf();
		}
		
		Builder sourceEffectiveTime(final Long sourceEffectiveTime) {
			this.sourceEffectiveTime = sourceEffectiveTime;
			return getSelf();
		}
		
		Builder targetEffectiveTime(final Long targetEffectiveTime) {
			this.targetEffectiveTime = targetEffectiveTime;
			return getSelf();
		}
		
		Builder unitId(final String unitId) {
			this.unitId = unitId;
			return getSelf();
		}
		
		Builder value(final String value) {
			this.value = value;
			return getSelf();
		}
		
		Builder valueId(String valueId) {
			this.valueId = valueId;
			return getSelf();
		}
		
		public SnomedRefSetMemberIndexEntry build() {
			final SnomedRefSetMemberIndexEntry doc = new SnomedRefSetMemberIndexEntry(id,
					label,
					moduleId, 
					released, 
					active, 
					effectiveTime, 
					referencedComponentId, 
					referenceSetId,
					referenceSetType,
					referencedComponentType);
			// association members
			doc.targetComponent = targetComponent;
			// attribute value
			doc.valueId = valueId;
			// concrete domain members
			doc.dataType = dataType;
			doc.attributeName = attributeName;
			doc.value = value;
			doc.characteristicTypeId = characteristicTypeId;
			doc.operatorId = operatorId;
			doc.unitId = unitId;
			// description
			doc.descriptionFormat = descriptionFormat;
			doc.descriptionLength = descriptionLength;
			// language reference set
			doc.acceptabilityId = acceptabilityId;
			// module
			doc.sourceEffectiveTime = sourceEffectiveTime;
			doc.targetEffectiveTime = targetEffectiveTime;
			// simple map
			doc.mapTarget = mapTarget;
			doc.mapTargetDescription = mapTargetDescription;
			// complex map
			doc.mapCategoryId = mapCategoryId;
			doc.mapAdvice = mapAdvice;
			doc.correlationId = correlationId;
			doc.mapGroup = mapGroup;
			doc.mapPriority = mapPriority;
			doc.mapRule = mapRule;
			// query
			doc.query = query;
			
			doc.setScore(score);
			// metadata
			doc.setBranchPath(branchPath);
			doc.setCommitTimestamp(commitTimestamp);
			doc.setStorageKey(storageKey);
			doc.setReplacedIns(replacedIns);
			doc.setSegmentId(segmentId);
			return doc;
		}
	}

	private final String referencedComponentId;
	private final String referenceSetId;
	private final SnomedRefSetType referenceSetType;
	private final short referencedComponentType;
	
	// Member specific fields, they can be null or emptyish values
	// ASSOCIATION reference set members
	private String targetComponent;
	// ATTRIBUTE VALUE
	private String valueId;
	// CONCRETE DOMAIN reference set members
	private DataType dataType;
	private String attributeName;
	private String value;
	private String operatorId;
	private String characteristicTypeId;
	private String unitId;
	// DESCRIPTION
	private Integer descriptionLength;
	private String descriptionFormat;
	// LANGUAGE
	private String acceptabilityId;
	// MODULE
	private Long sourceEffectiveTime;
	private Long targetEffectiveTime;
	// SIMPLE MAP reference set members
	private String mapTarget;
	private String mapTargetDescription;
	// COMPLEX MAP
	private String mapCategoryId;
	private String correlationId;
	private String mapAdvice;
	private String mapRule;
	private Integer mapGroup;
	private Integer mapPriority;
	// QUERY
	private String query;
	

	private SnomedRefSetMemberIndexEntry(final String id,
			final String label,
			final String moduleId, 
			final boolean released,
			final boolean active, 
			final long effectiveTimeLong, 
			final String referencedComponentId, 
			final String referenceSetId,
			final SnomedRefSetType referenceSetType,
			final short referencedComponentType) {

		super(id, 
				label,
				referencedComponentId, // XXX: iconId is the referenced component identifier
				moduleId, 
				released, 
				active, 
				effectiveTimeLong);

		checkArgument(referencedComponentType >= CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT, "Referenced component type '%s' is invalid.", referencedComponentType);

		this.referencedComponentId = checkNotNull(referencedComponentId, "Reference component identifier may not be null.");
		this.referenceSetId = checkNotNull(referenceSetId, "Reference set identifier may not be null.");
		this.referenceSetType = checkNotNull(referenceSetType, "Reference set type may not be null.");
		this.referencedComponentType = referencedComponentType;
<<<<<<< HEAD
	}

	@Override
	public String getContainerId() {
		// XXX hack to make IHTSDO merge review API tests pass and work as before in 4.5
		if (getReferenceSetType() == SnomedRefSetType.MODULE_DEPENDENCY) {
			return null;
		} else {
			return getReferencedComponentId();
		}
	}

	/**
	 * @return the referenced component identifier
	 */
	public String getReferencedComponentId() {
		return referencedComponentId;
=======
		this.mapTargetComponentType = mapTargetComponentType;
	}

	/**
	 * @return the referenced component identifier
	 */
	public String getReferencedComponentId() {
		return referencedComponentId;
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code String} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code String}
	 */
	public String getStringField(final String fieldName) {
		return getRequiredField(fieldName, String.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Integer} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code Integer}
	 */
	private Integer getIntegerField(final String fieldName) {
		return getRequiredField(fieldName, Integer.class);
	}
	
	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Long} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code Long}
	 */	
	private Long getLongField(final String fieldName) {
		return getRequiredField(fieldName, Long.class);
	}

	public Optional<Object> getOptionalField(final String fieldName) {
		return Optional.fromNullable(additionalFields.get(fieldName));
	}

	private <T> Optional<T> getOptionalField(final String fieldName, Function<Object, T> transformFunction) {
		return getOptionalField(fieldName).transform(transformFunction);
	}
	
	private <T> Optional<T> getOptionalField(final String fieldName, final Class<T> type) {
		return getOptionalField(fieldName, new UncheckedCastFunction<Object, T>(type));
	}

	private <T> T getRequiredField(final String fieldName, final Class<T> type) {
		return getOptionalField(fieldName, type).get();
>>>>>>> origin/ms-develop
	}

	/**
	 * @return the identifier of the member's reference set
	 */
	public String getReferenceSetId() {
		return referenceSetId;
	}

	/**
	 * @return the type of the member's reference set
	 */
	public SnomedRefSetType getReferenceSetType() {
		return referenceSetType;
	}

	@JsonIgnore
	@SuppressWarnings("unchecked")
	public <T> T getValueAs() {
		final DataType dataType = getDataType();
		return (T) (dataType == null ? null : SnomedRefSetUtil.deserializeValue(dataType, getValue()));
	}
	
	public String getValue() {
		return value;
	}

	public DataType getDataType() {
		return dataType;
	}

<<<<<<< HEAD
	public String getUnitId() {
		return unitId;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getOperatorId() {
		return operatorId;
	}

	public String getCharacteristicTypeId() {
		return characteristicTypeId;
	}	

	public String getAcceptabilityId() {
		return acceptabilityId;
	}

	public Integer getDescriptionLength() {
		return descriptionLength;
	}
	
	public String getDescriptionFormat() {
		return descriptionFormat;
	}

	public String getMapTarget() {
		return mapTarget;
	}

	public Integer getMapGroup() {
		return mapGroup;
	}

	public Integer getMapPriority() {
		return mapPriority;
	}

	public String getMapRule() {
		return mapRule;
	}

	public String getMapAdvice() {
		return mapAdvice;
=======
	public String getAcceptabilityId() {
		return getLongField(SnomedMappings.memberAcceptabilityId().fieldName()).toString();
	}

	public Acceptability getAcceptability() {
		return Acceptability.getByConceptId(getAcceptabilityId());
	}

	public String getValueId() {
		return getStringField(SnomedMappings.memberValueId().fieldName());
	}

	public RelationshipRefinability getRefinability() {
		return RelationshipRefinability.getByConceptId(getValueId());
	}

	public InactivationIndicator getInactivationIndicator() {
		return InactivationIndicator.getByConceptId(getValueId());
	}

	public String getTargetComponentId() {
		return getStringField(SnomedMappings.memberTargetComponentId().fieldName());
	}

	public String getMapTargetComponentId() {
		return getStringField(SnomedMappings.memberMapTargetComponentId().fieldName());
	}

	public String getMapTargetDescription() {
		return getOptionalField(SnomedMappings.memberMapTargetComponentDescription().fieldName(), String.class).or(EMPTY_STRING);
	}

	public Integer getMapGroup() {
		return getIntegerField(SnomedMappings.memberMapGroup().fieldName());
	}

	public Integer getMapPriority() {
		return getIntegerField(SnomedMappings.memberMapPriority().fieldName());
	}

	public String getMapRule() {
		return getOptionalField(SnomedMappings.memberMapRule().fieldName(), String.class).or(EMPTY_STRING);
	}

	public String getMapAdvice() {
		return getOptionalField(SnomedMappings.memberMapAdvice().fieldName(), String.class).or(EMPTY_STRING);
>>>>>>> origin/ms-develop
	}

	public String getCorrelationId() {
		return getLongField(SnomedMappings.memberCorrelationId().fieldName()).toString();
	}

	public String getMapCategoryId() {
<<<<<<< HEAD
		return mapCategoryId;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public String getMapTargetDescription() {
		return mapTargetDescription;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String getTargetComponent() {
		return targetComponent;
	}
	
	public String getValueId() {
		return valueId;
	}
	
	public Long getSourceEffectiveTime() {
		return sourceEffectiveTime;
	}
	
	public Long getTargetEffectiveTime() {
		return targetEffectiveTime;
	}
	
	public short getReferencedComponentType() {
		return referencedComponentType;
	}
	
	// model helper methods
	
	@JsonIgnore
	public Acceptability getAcceptability() {
		return Acceptability.getByConceptId(getAcceptabilityId());
	}
	
	@JsonIgnore
	public RelationshipRefinability getRefinability() {
		return RelationshipRefinability.getByConceptId(getValueId());
	}
	
	@JsonIgnore
	public InactivationIndicator getInactivationIndicator() {
		return InactivationIndicator.getByConceptId(getValueId());
	}
	
	@JsonIgnore
	public String getSourceEffectiveTimeAsString() {
		return EffectiveTimes.format(getSourceEffectiveTime(), DateFormats.SHORT);
	}
	
	@JsonIgnore
	public String getTargetEffectiveTimeAsString() {
		return EffectiveTimes.format(getTargetEffectiveTime(), DateFormats.SHORT);
	}
	
	/**
	 * @return the {@code String} terminology component identifier of the component referenced in this member
	 */
	@JsonIgnore
	public String getReferencedComponentTypeAsString() {
		return CoreTerminologyBroker.getInstance().getTerminologyComponentId(referencedComponentType);
	}

	/**
	 * Helper which converts all non-null/empty additional fields to a values {@link Map} keyed by their field name; 
	 * @return
	 */
	@JsonIgnore
	public Map<String, Object> getAdditionalFields() {
		final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		// ASSOCIATION refset members
		putIfPresent(builder, Fields.TARGET_COMPONENT, getTargetComponent());
		// ATTRIBUTE_VALUE refset members 
		putIfPresent(builder, Fields.VALUE_ID, getValueId());
		// CONCRETE DOMAIN reference set members
		putIfPresent(builder, Fields.DATA_TYPE, getDataType());
		putIfPresent(builder, Fields.ATTRIBUTE_NAME, getAttributeName());
		putIfPresent(builder, Fields.DATA_VALUE, getValue());
		putIfPresent(builder, Fields.OPERATOR_ID, getOperatorId());
		putIfPresent(builder, Fields.CHARACTERISTIC_TYPE_ID, getCharacteristicTypeId());
		putIfPresent(builder, Fields.UNIT_ID, getUnitId());
		// DESCRIPTION
		putIfPresent(builder, Fields.DESCRIPTION_LENGTH, getDescriptionLength());
		putIfPresent(builder, Fields.DESCRIPTION_FORMAT, getDescriptionFormat());
		// LANGUAGE
		putIfPresent(builder, Fields.ACCEPTABILITY_ID, getAcceptabilityId());
		// MODULE
		putIfPresent(builder, Fields.SOURCE_EFFECTIVE_TIME, getSourceEffectiveTime());
		putIfPresent(builder, Fields.TARGET_EFFECTIVE_TIME, getTargetEffectiveTime());
		// SIMPLE MAP reference set members
		putIfPresent(builder, Fields.MAP_TARGET, getMapTarget());
		putIfPresent(builder, Fields.MAP_TARGET_DESCRIPTION, getMapTargetDescription());
		// COMPLEX MAP
		putIfPresent(builder, Fields.MAP_CATEGORY_ID, getMapCategoryId());
		putIfPresent(builder, Fields.CORRELATION_ID, getCorrelationId());
		putIfPresent(builder, Fields.MAP_ADVICE, getMapAdvice());
		putIfPresent(builder, Fields.MAP_RULE, getMapRule());
		putIfPresent(builder, Fields.MAP_GROUP, getMapGroup());
		putIfPresent(builder, Fields.MAP_PRIORITY, getMapPriority());
		// QUERY
		putIfPresent(builder, Fields.QUERY, getQuery());
		return builder.build();
	}
	
	private static void putIfPresent(ImmutableMap.Builder<String, Object> builder, String key, Object value) {
		if (key != null && value != null) {
			builder.put(key, value);
		}
	}
	
	@Override
	protected ToStringHelper doToString() {
		return super.doToString()
				.add("referencedComponentId", referencedComponentId)
				.add("referenceSetId", referenceSetId)
				.add("referenceSetType", referenceSetType)
				.add("referencedComponentType", referencedComponentType)
				.add("targetComponent", targetComponent)
				.add("valueId", valueId)
				.add("dataType", dataType)
				.add("attributeName", attributeName)
				.add("value", value)
				.add("operatorId", operatorId)
				.add("characteristicTypeId", characteristicTypeId)
				.add("unitId", unitId)
				.add("descriptionLength", descriptionLength)
				.add("descriptionFormat", descriptionFormat)
				.add("acceptabilityId", acceptabilityId)
				.add("sourceEffectiveTime", sourceEffectiveTime)
				.add("targetEffectiveTime", targetEffectiveTime)
				.add("mapTarget", mapTarget)
				.add("mapTargetDescription", mapTargetDescription)
				.add("mapCategoryId", mapCategoryId)
				.add("correlationId", correlationId)
				.add("mapAdvice", mapAdvice)
				.add("mapRule", mapRule)
				.add("mapGroup", mapGroup)
				.add("mapPriority", mapPriority)
				.add("query", query);
=======
		return getOptionalField(SnomedMappings.memberMapCategoryId().fieldName(), Long.class).transform(Functions.toStringFunction()).or(EMPTY_STRING);
	}

	public String getDescriptionFormatId() {
		return getLongField(SnomedMappings.memberDescriptionFormatId().fieldName()).toString();
	}

	public Integer getDescriptionLength() {
		return getIntegerField(SnomedMappings.memberDescriptionLength().fieldName());
	}

	public String getOperatorComponentId() {
		return getLongField(SnomedMappings.memberOperatorId().fieldName()).toString();
	}

	public String getUomComponentId() {
		return getOptionalField(SnomedMappings.memberUomId().fieldName(), Long.class).transform(Functions.toStringFunction()).or(EMPTY_STRING);
	}

	public String getAttributeLabel() {
		return getStringField(SnomedMappings.memberDataTypeLabel().fieldName());
	}

	public DataType getRefSetPackageDataType() {
		return DataType.get(getIntegerField(SnomedMappings.memberDataTypeOrdinal().fieldName()));
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue() {
		return (T) SnomedRefSetUtil.deserializeValue(getRefSetPackageDataType(), getStringField(SnomedMappings.memberSerializedValue().fieldName())); 
	}
	
	public String getSerializedValue() {
		return getStringField(SnomedMappings.memberSerializedValue().fieldName());
	}

	public String getCharacteristicTypeId() {
		return getOptionalField(SnomedMappings.memberCharacteristicTypeId().fieldName(), Long.class).transform(Functions.toStringFunction()).or(EMPTY_STRING);
	}	

	public String getQuery() {
		return getStringField(SnomedMappings.memberQuery().fieldName());
	}
	
	public String getSourceEffectiveTime() {
		return EffectiveTimes.format(getLongField(SnomedMappings.memberSourceEffectiveTime().fieldName()), DateFormats.SHORT);
	}

	public String getTargetEffectiveTime() {
		return EffectiveTimes.format(getLongField(SnomedMappings.memberTargetEffectiveTime().fieldName()), DateFormats.SHORT);
	}
	
	@Deprecated
	public String getSpecialFieldLabel() {
		throw new UnsupportedOperationException("Special field label needs to be computed separately.");
>>>>>>> origin/ms-develop
	}
}
