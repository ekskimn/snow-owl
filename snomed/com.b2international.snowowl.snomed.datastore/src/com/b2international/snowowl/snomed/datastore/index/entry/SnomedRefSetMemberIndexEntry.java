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
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.b2international.commons.BooleanUtils;
import com.b2international.commons.functions.UncheckedCastFunction;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.api.IComponent;
import com.b2international.snowowl.datastore.index.mapping.Mappings;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.core.domain.RelationshipRefinability;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetUtil;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.snomedrefset.DataType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * Lightweight representation of a SNOMED CT reference set member.
 */
public class SnomedRefSetMemberIndexEntry extends SnomedIndexEntry implements IComponent<String>, Serializable {

	private static final Set<String> ADDITIONAL_FIELDS = SnomedMappings.fieldsToLoad()
			.memberAcceptabilityId()
			.memberValueId()
			.memberTargetComponentId()
			.memberMapTargetComponentId()
			.memberMapTargetComponentDescription()
			.memberMapGroup()
			.memberMapPriority()
			.memberMapRule()
			.memberMapAdvice()
			.memberMapCategoryId()
			.memberCorrelationId()
			.memberDescriptionFormatId()
			.memberDescriptionLength()
			.memberOperatorId()
			.memberContainerModuleId()
			.memberUomId()
			.memberDataTypeLabel()
			.memberDataTypeOrdinal()
			.memberSerializedValue()
			.memberCharacteristicTypeId()
			.memberQuery()
			.memberSourceEffectiveTime()
			.memberTargetEffectiveTime()
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

	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member from a CDOish object.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry create(final SnomedRefSetMember member) {
	//		return create(member, null);
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member from a CDOish object with the given label of the referenced component.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry create(final SnomedRefSetMember member, @Nullable final String label) {
	//		Preconditions.checkNotNull(member, "Reference set member argument cannot be null.");
	//		Preconditions.checkArgument(!CDOState.NEW.equals(member.cdoState()), "Reference set member CDO state should not be NEW. " +
	//				"Use com.b2international.snowowl.snomed.datastore.index.refset.SnomedRefSetLuceneIndexDTO.createForNewMember(SnomedRefSetMember<?>) instead.");
	//		Preconditions.checkArgument(!CDOState.TRANSIENT.equals(member.cdoState()), "Reference set member CDO state should not be TRANSIENT. " +
	//				"Use com.b2international.snowowl.snomed.datastore.index.refset.SnomedRefSetLuceneIndexDTO.createForDetachedMember(SnomedRefSetMember<?>, SnomedRefSet<?>) instead");
	//		return new SnomedRefSetMemberIndexEntry(member, label, tryGetIconId(member), CDOIDUtil.getLong(member.cdoID()), member.getRefSet());
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member from a new CDOish reference set member.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry createForNewMember(final SnomedRefSetMember member) {
	//		return createForNewMember(member, null);
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member from a new CDOish reference set member with the given label of the referenced component.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry createForNewMember(final SnomedRefSetMember member, @Nullable final String label) {
	//		Preconditions.checkNotNull(member, "Reference set member argument cannot be null.");
	//		Preconditions.checkArgument(CDOState.NEW.equals(member.cdoState()), "Reference set member CDO state must be NEW.");
	//		return new SnomedRefSetMemberIndexEntry(member, label, tryGetIconId(member), 0L, member.getRefSet());
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member representing a detached member.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry createForDetachedMember(final SnomedRefSetMember member, final SnomedRefSet refSet) {
	//		return createForDetachedMember(member, refSet, null);
	//	}
	//
	//	/**
	//	 * (non-API)
	//	 * Creates a reference set member representing a detached member.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry createForDetachedMember(final SnomedRefSetMember member, final SnomedRefSet refSet, @Nullable final String label) {
	//		Preconditions.checkNotNull(member, "Reference set member argument cannot be null.");
	//		Preconditions.checkNotNull(refSet, "Container reference set argument cannot be null.");
	//		Preconditions.checkArgument(CDOState.TRANSIENT.equals(member.cdoState()), "Reference set member CDO state must be TRANSIENT.");
	//		return new SnomedRefSetMemberIndexEntry(member, label, SnomedConstants.Concepts.ROOT_CONCEPT, 0L, refSet);
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a mock reference set member.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry createMockMember(final IComponent<String> component) {
	//		return new SnomedRefSetMemberIndexEntry(component, SnomedIconProvider.getInstance().getIconComponentId(component.getId()));
	//	}
	//	
	//	protected static String tryGetIconId(final SnomedRefSetMember member) {
	//		
	//		if (CDOUtils.checkObject(member)) {
	//			
	//			if (member instanceof SnomedQueryRefSetMember) {
	//				return Concepts.REFSET_ROOT_CONCEPT;
	//			}
	//			
	//			final short referencedComponentType = member.getReferencedComponentType();
	//			final String referencedComponentId = member.getReferencedComponentId();
	//			Object iconIdAsobject = CoreTerminologyBroker.getInstance().getComponentIconIdProvider(referencedComponentType).getIconId(BranchPathUtils.createPath(member.cdoView()), referencedComponentId);
	//			if (null != iconIdAsobject) {
	//				return String.valueOf(iconIdAsobject);
	//			}
	//			
	//		}
	//		
	//		return Concepts.ROOT_CONCEPT;
	//	}
	//	
	//	/**
	//	 * (non-API)
	//	 * Creates a new reference set member based on the given index document.
	//	 */
	//	public static SnomedRefSetMemberIndexEntry create(final Document doc, @Nullable final IBranchPath branchPath) {
	//		Preconditions.checkNotNull(doc, "Document argument cannot be null.");
	//		
	//		final SnomedRefSetType type = SnomedRefSetType.get(SnomedMappings.memberRefSetType().getValue(doc));
	//		final String uuid = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_UUID);
	//		final String moduleId = SnomedMappings.module().getValueAsString(doc);
	//		final long storageKey = Mappings.storageKey().getValue(doc);
	//		final boolean active = SnomedMappings.active().getValue(doc) == 1;
	//		final boolean released = IndexUtils.getBooleanValue(doc.getField(SnomedIndexBrowserConstants.COMPONENT_RELEASED));
	//		final long refSetId = SnomedMappings.memberRefSetId().getValue(doc);
	//		final short referencedComponentType = SnomedMappings.memberReferencedComponentType().getShortValue(doc);
	//		final String referencedComponentId = SnomedMappings.memberReferencedComponentId().getValueAsString(doc);
	//		final long effectiveTimeLong = SnomedMappings.effectiveTime().getValue(doc);
	//		final short specialFieldComponentType = isMapping(type) ? getSpecialFieldComponentTypeId(doc) : getSpecialFieldComponentTypeId(type);
	//		final String specialFieldId = doc.get(SnomedRefSetUtil.getSpecialComponentIdIndexField(type));
	//		final String mapTargetDescription = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_MAP_TARGET_COMPONENT_DESCRIPTION);
	//		
	//		String iconId = null;
	//		if (null != branchPath) {
	//			Object iconIdAsObjact = CoreTerminologyBroker.getInstance().getComponentIconIdProvider(referencedComponentType).getIconId(branchPath, referencedComponentId);
	//			if (null != iconIdAsObjact) {
	//				iconId = String.valueOf(iconIdAsObjact);
	//			}
	//		}
	//		
	//		return new SnomedRefSetMemberIndexEntry(
	//				uuid, 
	//				uuid, 
	//				iconId, 
	//				moduleId, 
	//				0.0F, 
	//				storageKey, 
	//				released, 
	//				active, 
	//				refSetId, 
	//				referencedComponentType, 
	//				referencedComponentId, 
	//				type, 
	//				effectiveTimeLong, 
	//				specialFieldComponentType, 
	//				specialFieldId,
	//				mapTargetDescription);
	//		
	//	}
	//	
	//
	//	/**
	//	 * @param id
	//	 * @param label
	//	 * @param iconId TODO
	//	 * @param moduleId
	//	 * @param score
	//	 * @param storageKey
	//	 * @param storageKey
	//	 * @param released
	//	 * @param active
	//	 * @param refSetIdentifierId
	//	 * @param refComponentType
	//	 * @param referencedComponentId
	//	 * @param refSetType
	//	 * @param effectiveTime
	//	 * @param specialFieldComponentType
	//	 * @param specialFieldLabel
	//	 * @param specialFieldId
	//	 * @param mapTargetDescription2 
	//	 */
	//	private SnomedRefSetMemberIndexEntry(final String id, final String label, String iconId, final String moduleId, 
	//			final float score, final long storageKey, final boolean released, final boolean active,
	//			final long refSetIdentifierId, final short refComponentType, final String referencedComponentId, 
	//			final SnomedRefSetType refSetType, final long effectiveTime, final short specialFieldComponentType, final String specialFieldLabel, final String specialFieldId, final String mapTargetDescription) {
	//		
	//		super(id, iconId, score, storageKey, moduleId, released, active, effectiveTime);
	//		this.refSetIdentifierId = refSetIdentifierId;
	//		this.refComponentType = refComponentType;
	//		this.referencedComponentId = referencedComponentId;
	//		this.refSetType = refSetType;
	//		this.storageKey = storageKey;
	//		this.specialFieldComponentType = specialFieldComponentType;
	//		this.specialFieldLabel = specialFieldLabel;
	//		this.specialFieldId = specialFieldId;
	//		this.label = label;
	//		this.active = active;
	//		this.mapTargetDescription = mapTargetDescription;
	//	}
	//
	//	protected SnomedRefSetMemberIndexEntry(final IComponent<String> component, String iconId) {
	//		super(Preconditions.checkNotNull(component, "Component argument cannot be null.").getId(), iconId, 0.0F, -1L, Concepts.MODULE_SCT_CORE, false, true, -1L);
	//		referencedComponentId = component.getId();
	//		refComponentType = CoreTerminologyBroker.getInstance().getTerminologyComponentIdAsShort(component);
	//		label = component.getLabel();
	//		storageKey = -1L;
	//	}
	//	
	//	/**
	//	 * Copy constructor.
	//	 * @param entry
	//	 */
	//	protected SnomedRefSetMemberIndexEntry(final SnomedRefSetMemberIndexEntry entry) {
	//		super(entry.getId(), entry.getIconId(), 0.0F, entry.getStorageKey(), entry.getModuleId(), entry.isReleased(), entry.isActive(), entry.getEffectiveTimeAsLong());
	//		referencedComponentId = entry.getReferencedComponentId();
	//		refComponentType = CoreTerminologyBroker.getInstance().getTerminologyComponentIdAsShort(entry.getReferencedComponentType());
	//		specialFieldComponentType = CoreTerminologyBroker.getInstance().getTerminologyComponentIdAsShort(entry.getSpecialFieldComponentType());
	//		specialFieldId = entry.getSpecialFieldId();
	//		specialFieldLabel = entry.getSpecialFieldLabel();
	//		refSetIdentifierId = Long.parseLong(entry.getRefSetIdentifierId());
	//		refSetType = entry.getRefSetType();
	//		active = entry.isActive();
	//		storageKey = entry.getStorageKey();
	//		label = entry.getLabel();
	//		mapTargetDescription = entry.mapTargetDescription;
	//	}
	//	
	//	protected SnomedRefSetMemberIndexEntry(final SnomedRefSetMember member, @Nullable final String label, String iconId, final long cdoId, final SnomedRefSet refSet) {
	//		super(member.getUuid(), iconId, 0.0F, cdoId, member.getModuleId(), member.isReleased(), member.isActive(), 
	//				member.isSetEffectiveTime() ? member.getEffectiveTime().getTime() : EffectiveTimes.UNSET_EFFECTIVE_TIME);
	//		refSetIdentifierId = Long.parseLong(refSet.getIdentifierId());
	//		referencedComponentId = member.getReferencedComponentId();
	//		refComponentType = member.getReferencedComponentType();
	//		active = member.isActive();
	//		refSetType = refSet.getType();
	//		this.label = super.label; //required as we are managing quite different reference for the label
	//		storageKey = cdoId;
	//		
	//		switch (refSetType) {
	//			case SIMPLE: 
	//				specialFieldComponentType = CoreTerminologyBroker.UNSPECIFIED_NUMBER;
	//				specialFieldId = null;
	//				specialFieldLabel = null;
	//				break;
	//			case QUERY:
	//				specialFieldComponentType = CoreTerminologyBroker.UNSPECIFIED_NUMBER;
	//				specialFieldId = ((SnomedQueryRefSetMember) member).getQuery(); //query string should be set as ID
	//				specialFieldLabel = ((SnomedQueryRefSetMember) member).getQuery();
	//				break;
	//			case EXTENDED_MAP: //$FALL-THROUGH$
	//			case COMPLEX_MAP: //$FALL-THROUGH$
	//			case SIMPLE_MAP:
	//				specialFieldComponentType = ((SnomedMappingRefSet) refSet).getMapTargetComponentType();
	//				specialFieldId = ((SnomedSimpleMapRefSetMember) member).getMapTargetComponentId();
	//				mapTargetDescription = ((SnomedSimpleMapRefSetMember) member).getMapTargetComponentDescription();
	//				if (null == specialFieldId) {
	//					specialFieldId = "";
	//				} else {
	//					if (!isUnspecified()) {
	//						final IComponent<String> concept = getTerminologyBrowser().getConcept(specialFieldId);
	//						if (null == concept) {
	//							specialFieldLabel = specialFieldId;
	//						} else {
	//							specialFieldLabel =  concept.getLabel();
	//						}
	//					} else {
	//						specialFieldLabel = specialFieldId;
	//					}
	//				}
	//				break;
	//			case DESCRIPTION_TYPE:
	//				final SnomedDescriptionTypeRefSetMember descrMember = (SnomedDescriptionTypeRefSetMember) member;
	//				specialFieldId = descrMember.getDescriptionFormat();
	//				specialFieldComponentType = SnomedTerminologyComponentConstants.CONCEPT_NUMBER;
	//				specialFieldLabel = CoreTerminologyBroker.getInstance().getComponent(
	//						createPair(SnomedTerminologyComponentConstants.CONCEPT_NUMBER, descrMember.getDescriptionFormat())).getLabel();
	//				break;
	//			case ATTRIBUTE_VALUE:
	//				final SnomedAttributeValueRefSetMember attrMember = (SnomedAttributeValueRefSetMember) member;
	//				specialFieldId = attrMember.getValueId();
	//				specialFieldComponentType = SnomedTerminologyComponentConstants.CONCEPT_NUMBER;
	//				specialFieldLabel = CoreTerminologyBroker.getInstance().getComponent(
	//						createPair(SnomedTerminologyComponentConstants.CONCEPT_NUMBER, attrMember.getValueId())).getLabel();
	//				break;
	//			case LANGUAGE:
	//				final SnomedLanguageRefSetMember langMember = (SnomedLanguageRefSetMember) member;
	//				specialFieldComponentType = SnomedTerminologyComponentConstants.DESCRIPTION_NUMBER;
	//				specialFieldId = langMember.getAcceptabilityId();
	//				specialFieldLabel = CoreTerminologyBroker.getInstance().getComponent(
	//						createPair(SnomedTerminologyComponentConstants.CONCEPT_NUMBER, specialFieldId)).getLabel();
	//				break;
	//			case CONCRETE_DATA_TYPE:
	//				specialFieldComponentType = CoreTerminologyBroker.UNSPECIFIED_NUMBER_SHORT;
	//				specialFieldId = String.valueOf(((SnomedConcreteDataTypeRefSetMember) member).getDataType().getValue());
	//				specialFieldLabel = ((SnomedConcreteDataTypeRefSetMember) member).getSerializedValue();
	//				break;
	//			default: throw new IllegalArgumentException("Unknown reference set type: " + refSet.getType());
	//		}
	//		
	//	}

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
		return getField(fieldName, String.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Integer} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code Integer}
	 */
	public Integer getIntegerField(final String fieldName) {
		return getField(fieldName, Integer.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code BigDecimal} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code BigDecimal}
	 */
	public BigDecimal getBigDecimalField(final String fieldName) {
		return getField(fieldName, BigDecimal.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Date} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code Date}
	 */
	public Date getDateField(final String fieldName) {
		return getField(fieldName, Date.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Boolean} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 * @throws ClassCastException if the value is not of type {@code Boolean}
	 */
	public Boolean getBooleanField(final String fieldName) {
		return getField(fieldName, Boolean.class);
	}

	/**
	 * @param fieldName the name of the additional field
	 * @return the {@code Object} value stored for the field
	 * @throws IllegalStateException if no value was set for the field
	 */	
	public Object getField(final String fieldName) {
		return getOptionalField(fieldName).get();
	}

	private Optional<Object> getOptionalField(final String fieldName) {
		return Optional.fromNullable(additionalFields.get(fieldName));
	}

	private <T> T getField(final String fieldName, final Class<T> type) {
		return getField(fieldName, new UncheckedCastFunction<Object, T>(type));
	}

	private <T> T getField(final String fieldName, Function<Object, T> transformFunction) {
		return getOptionalField(fieldName).transform(transformFunction).get();
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

	@SuppressWarnings("unchecked")
	public <T> T getValue() {
		return (T) getField(SnomedMappings.memberSerializedValue().fieldName()); 
	}

	public DataType getRefSetPackageDataType() {
		return DataType.get(getIntegerField(SnomedMappings.memberDataTypeOrdinal().fieldName()));
	}

	public String getUomComponentId() {
		return getStringField(SnomedMappings.memberUomId().fieldName());
	}

	public String getAttributeLabel() {
		return getStringField(SnomedMappings.memberDataTypeLabel().fieldName());
	}

	public String getOperatorComponentId() {
		return getStringField(SnomedMappings.memberOperatorId().fieldName());
	}

	public String getCharacteristicTypeId() {
		return getStringField(SnomedMappings.memberCharacteristicTypeId().fieldName());
	}	

	public Acceptability getAcceptability() {
		return Acceptability.getByConceptId(getAcceptabilityId());
	}

	public String getAcceptabilityId() {
		return getStringField(SnomedMappings.memberAcceptabilityId().fieldName());
	}

	public Integer getDescriptionLength() {
		return getIntegerField(SnomedMappings.memberDescriptionLength().fieldName());
	}

	public String getMapTargetComponentId() {
		return getStringField(SnomedMappings.memberMapTargetComponentId().fieldName());
	}

	public int getMapGroup() {
		return getIntegerField(SnomedMappings.memberMapGroup().fieldName());
	}

	public int getMapPriority() {
		return getIntegerField(SnomedMappings.memberMapPriority().fieldName());
	}

	public String getMapRule() {
		return getStringField(SnomedMappings.memberMapRule().fieldName());
	}

	public String getMapAdvice() {
		return getStringField(SnomedMappings.memberMapAdvice().fieldName());
	}
	
	public String getMapCategoryId() {
		return getStringField(SnomedMappings.memberMapCategoryId().fieldName());
	}
	
	public String getCorrelationId() {
		return getStringField(SnomedMappings.memberCorrelationId().fieldName());
	}

	public String getMapTargetDescription() {
		return getStringField(SnomedMappings.memberMapTargetComponentDescription().fieldName());
	}
	
	public String getQuery() {
		return getStringField(SnomedMappings.memberQuery().fieldName());
	}
	
	public String getTargetComponentId() {
		return getStringField(SnomedMappings.memberTargetComponentId().fieldName());
	}

	public RelationshipRefinability getRefinability() {
		return RelationshipRefinability.getByConceptId(getValueId());
	}
	
	public InactivationIndicator getInactivationIndicator() {
		return InactivationIndicator.getByConceptId(getValueId());
	}

	public String getValueId() {
		return getStringField(SnomedMappings.memberValueId().fieldName());
	}

	@Deprecated
	public String getSpecialFieldLabel() {
		throw new UnsupportedOperationException("Special field label needs to be computed separately.");
	}
}
