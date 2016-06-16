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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.eclipse.emf.spi.cdo.FSMUtil;

import com.b2international.commons.BooleanUtils;
import com.b2international.commons.functions.UncheckedCastFunction;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.api.IComponent;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.cdo.CDOIDUtils;
import com.b2international.snowowl.datastore.index.mapping.Mappings;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.core.domain.RelationshipRefinability;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetUtil;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * Lightweight representation of a SNOMED CT reference set member.
 */
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
	}
	
	private static final long serialVersionUID = 3504576207161692354L;

	public static Builder builder() {
		return new Builder();
	}
	
	public static Builder builder(final Document doc) {
		final SnomedRefSetType refSetType = SnomedRefSetType.get(SnomedMappings.memberRefSetType().getValue(doc));
		final Builder builder = builder() 
				.active(BooleanUtils.valueOf(SnomedMappings.active().getValue(doc)))
				.effectiveTimeLong(SnomedMappings.effectiveTime().getValue(doc))
				.id(SnomedMappings.memberUuid().getValue(doc))
				.moduleId(SnomedMappings.module().getValueAsString(doc))
				.referencedComponentId(SnomedMappings.memberReferencedComponentId().getValueAsString(doc))
				.referencedComponentType(SnomedMappings.memberReferencedComponentType().getShortValue(doc))
				.referenceSetId(SnomedMappings.memberRefSetId().getValueAsString(doc))
				.referenceSetType(refSetType)
				.released(BooleanUtils.valueOf(SnomedMappings.released().getValue(doc)))
				.storageKey(Mappings.storageKey().getValue(doc));
		
		if (SnomedRefSetUtil.isMapping(refSetType)) {
			builder.mapTargetComponentType(SnomedMappings.memberMapTargetComponentType().getShortValue(doc));
		}
		
		for (IndexableField storedField : doc) {
			if (SnomedRefSetMemberIndexEntry.isAdditionalField(storedField.name())) {
				if (storedField.numericValue() != null) {
					builder.additionalField(storedField.name(), storedField.numericValue());
				} else {
					builder.additionalField(storedField.name(), storedField.stringValue());
				}
			}
		}
		
		return builder;
	}
	
	public static Builder builder(final SnomedRefSetMemberIndexEntry source) {
		return builder()
				.active(source.active)
				.effectiveTimeLong(source.effectiveTimeLong)
				.id(source.id)
				.moduleId(source.moduleId)
				.referencedComponentId(source.referencedComponentId)
				.referencedComponentType(source.referencedComponentType)
				.referenceSetId(source.getRefSetIdentifierId())
				.referenceSetType(source.referenceSetType)
				.released(source.released)
				.storageKey(source.storageKey)
				.score(source.score)
				.mapTargetComponentType(source.mapTargetComponentType)
				.additionalFields(source.additionalFields);
	}
	
	public static Builder builder(SnomedRefSetMember refSetMember) {
		final Builder builder = SnomedRefSetMemberIndexEntry.builder()
				.id(refSetMember.getUuid()) 
				.moduleId(refSetMember.getModuleId())
				.active(refSetMember.isActive())
				.released(refSetMember.isReleased())
				.effectiveTimeLong(refSetMember.isSetEffectiveTime() ? refSetMember.getEffectiveTime().getTime() : EffectiveTimes.UNSET_EFFECTIVE_TIME)
				.referenceSetId(refSetMember.getRefSetIdentifierId())
				.referenceSetType(refSetMember.getRefSet().getType())
				.referencedComponentType(refSetMember.getReferencedComponentType())
				.referencedComponentId(refSetMember.getReferencedComponentId());

		if (!FSMUtil.isTransient(refSetMember)) {
			builder.storageKey(CDOIDUtils.asLongSafe(refSetMember.cdoID()));
		}
		
		return new SnomedRefSetSwitch<Builder>() {

			@Override
			public Builder caseSnomedRefSetMember(SnomedRefSetMember object) {
				return builder;
			}

			@Override
			public Builder caseSnomedLanguageRefSetMember(final SnomedLanguageRefSetMember languageMember) {
				return builder.additionalField(SnomedMappings.memberAcceptabilityId().fieldName(), Long.valueOf(languageMember.getAcceptabilityId()));
			}

			@Override
			public Builder caseSnomedAttributeValueRefSetMember(final SnomedAttributeValueRefSetMember attributeValueMember) {
				return builder.additionalField(SnomedMappings.memberValueId().fieldName(), attributeValueMember.getValueId());
			}

			@Override
			public Builder caseSnomedAssociationRefSetMember(final SnomedAssociationRefSetMember associationMember) {
				return builder.additionalField(SnomedMappings.memberTargetComponentId().fieldName(), associationMember.getTargetComponentId());
			}

			@Override
			public Builder caseSnomedSimpleMapRefSetMember(final SnomedSimpleMapRefSetMember mapRefSetMember) {
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
			}

			@Override
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

	public static class Builder extends AbstractBuilder<Builder> {

		private String referencedComponentId;
		private final Map<String, Object> additionalFields = newHashMap();

		private String referenceSetId;
		private SnomedRefSetType referenceSetType;
		private short referencedComponentType;
		private short mapTargetComponentType = CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT;

		private Builder() {
			// Disallow instantiation outside static method
		}

		@Override
		protected Builder getSelf() {
			return this;
		}

		public Builder referencedComponentId(final String referencedComponentId) {
			this.referencedComponentId = referencedComponentId;
			return this;
		}

		public Builder additionalField(final String fieldName, final Object fieldValue) {
			this.additionalFields.put(fieldName, fieldValue);
			return this;
		}

		public Builder additionalFields(final Map<String, Object> additionalFields) {
			this.additionalFields.putAll(additionalFields);
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

		public Builder mapTargetComponentType(final short mapTargetComponentType) {
			this.mapTargetComponentType = mapTargetComponentType;
			return this;
		}

		public SnomedRefSetMemberIndexEntry build() {
			return new SnomedRefSetMemberIndexEntry(id, 
					score, 
					storageKey, 
					moduleId, 
					released, 
					active, 
					effectiveTimeLong, 
					referencedComponentId, 
					ImmutableMap.copyOf(additionalFields),
					referenceSetId,
					referenceSetType,
					referencedComponentType,
					mapTargetComponentType);
		}
	}

	private final String referencedComponentId;
	private final ImmutableMap<String, Object> additionalFields;

	private final String referenceSetId;
	private final SnomedRefSetType referenceSetType;
	private final short referencedComponentType;
	private final short mapTargetComponentType;

	private SnomedRefSetMemberIndexEntry(final String id,
			final float score, 
			final long storageKey, 
			final String moduleId, 
			final boolean released,
			final boolean active, 
			final long effectiveTimeLong, 
			final String referencedComponentId, 
			final ImmutableMap<String, Object> additionalFields,
			final String referenceSetId,
			final SnomedRefSetType referenceSetType,
			final short referencedComponentType,
			final short mapTargetComponentType) {

		super(id, 
				referenceSetId, // XXX: iconId is the reference set identifier
				score, 
				storageKey, 
				moduleId, 
				released, 
				active, 
				effectiveTimeLong);

		checkArgument(referencedComponentType >= CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT, "Referenced component type '%s' is invalid.", referencedComponentType);
		checkArgument(mapTargetComponentType >= CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT, "Map target component type '%s' is invalid.", referencedComponentType);

		this.referencedComponentId = checkNotNull(referencedComponentId, "Reference component identifier may not be null.");
		this.additionalFields = checkNotNull(additionalFields, "Additional field map may not be null.");
		this.referenceSetId = checkNotNull(referenceSetId, "Reference set identifier may not be null.");
		this.referenceSetType = checkNotNull(referenceSetType, "Reference set type may not be null.");
		this.referencedComponentType = referencedComponentType;
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
	}

	/**
	 * @return the identifier of the member's reference set
	 */
	public String getRefSetIdentifierId() {
		return referenceSetId;
	}

	/**
	 * @return the type of the member's reference set
	 */
	public SnomedRefSetType getRefSetType() {
		return referenceSetType;
	}

	/**
	 * @return the {@code String} terminology component identifier of the component referenced in this member
	 */
	public String getReferencedComponentType() {
		return CoreTerminologyBroker.getInstance().getTerminologyComponentId(referencedComponentType);
	}

	/**
	 * @return the {@code String} terminology component identifier of the map target in this member, or
	 *         {@link CoreTerminologyBroker#UNSPECIFIED} if not known (or the reference set is not a map)
	 */
	public String getMapTargetComponentType() {
		return CoreTerminologyBroker.getInstance().getTerminologyComponentId(mapTargetComponentType);
	}
	
	/**
	 * @return the {@code String} terminology component identifier of the map target in this member, or
	 *         {@link CoreTerminologyBroker#UNSPECIFIED_NUMBER_SHORT} if not known (or the reference set is not a map)
	 */
	public short getMapTargetComponentTypeAsShort() {
		return mapTargetComponentType;
	}

	@Override
	public String toString() {
		return toStringHelper()
				.add("referencedComponentId", referencedComponentId)
				.add("additionalFields", additionalFields)
				.add("referenceSetType", referenceSetType)
				.add("referencedComponentType", referencedComponentType)
				.add("mapTargetComponentType", mapTargetComponentType)
				.toString();
	}

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
	}

	public String getCorrelationId() {
		return getLongField(SnomedMappings.memberCorrelationId().fieldName()).toString();
	}

	public String getMapCategoryId() {
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
		return (T) SnomedRefSetUtil.deserializeValue(getRefSetPackageDataType(), getSerializedValue()); 
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
	}
}
