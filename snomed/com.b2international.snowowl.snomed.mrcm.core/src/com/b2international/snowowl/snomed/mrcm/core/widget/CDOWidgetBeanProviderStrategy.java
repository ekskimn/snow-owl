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
package com.b2international.snowowl.snomed.mrcm.core.widget;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.cdo.view.CDOView;

import com.b2international.commons.StringUtils;
import com.b2international.commons.functions.UncheckedCastFunction;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.mrcm.core.widget.bean.ConceptWidgetBean;
import com.b2international.snowowl.snomed.mrcm.core.widget.bean.DataTypeWidgetBean;
import com.b2international.snowowl.snomed.mrcm.core.widget.bean.LeafWidgetBean;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.ConceptWidgetModel;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.DataTypeContainerWidgetModel;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.DataTypeWidgetModel;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.RelationshipGroupWidgetModel;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.RelationshipGroupWidgetModel.GroupFlag;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.WidgetModel;
import com.b2international.snowowl.snomed.snomedrefset.SnomedConcreteDataTypeRefSetMember;
import com.b2international.snowowl.snomed.snomedrefset.SnomedLanguageRefSetMember;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Widget provider strategy implementation, which uses CDO and RPC calls.
 * 
 */
public class CDOWidgetBeanProviderStrategy extends WidgetBeanProviderStrategy {

	private final Concept concept;

	public CDOWidgetBeanProviderStrategy(ConceptWidgetModel conceptWidgetModel, Concept concept, final boolean includeUnsanctioned) {
		super(checkNotNull(conceptWidgetModel, "conceptWidgetModel"), includeUnsanctioned);
		this.concept = checkNotNull(concept, "concept");
	}

	@Override
	protected Collection<ISnomedDescription> getDescriptions() {
		final Set<ISnomedDescription> descriptions = newHashSet();
		for (final Description description : concept.getDescriptions()) {
			if (!description.isActive()) {
				continue;
			}
			
			final SnomedDescription convertedDescription = new SnomedDescription();
			convertedDescription.setId(description.getId());
			convertedDescription.setActive(description.isActive());
			convertedDescription.setCaseSignificance(CaseSignificance.getByConceptId(description.getCaseSignificance().getId()));
			convertedDescription.setConceptId(description.getConcept().getId());
			convertedDescription.setEffectiveTime(description.getEffectiveTime());
			convertedDescription.setLanguageCode(description.getLanguageCode());
			convertedDescription.setModuleId(description.getModule().getId());
			convertedDescription.setReleased(description.isReleased());
			convertedDescription.setTerm(description.getTerm());
			convertedDescription.setType(new SnomedConcept(description.getType().getId()));
			convertedDescription.setTypeId(description.getType().getId());
			
			final Map<String, Acceptability> acceptabilityMap = newHashMap();
			for (SnomedLanguageRefSetMember member : description.getLanguageRefSetMembers()) {
				if (!member.isActive()) {
					continue;
				}
				acceptabilityMap.put(member.getRefSetIdentifierId(), Acceptability.getByConceptId(member.getAcceptabilityId()));
			}
			convertedDescription.setAcceptabilityMap(acceptabilityMap);
		}
		
		return descriptions;
	}
	
	@Override
	protected Collection<SnomedRelationship> getRelationships() {
		final List<Relationship> sourceRelationships = concept.getOutboundRelationships();
		//have to filter out relationships with null destination
		//can happen when creating child C concept under P parent and before saving C someone deletes P  
		final Collection<Relationship> existingRelationships = Collections2.filter(sourceRelationships, new Predicate<Relationship>() {
			@Override public boolean apply(Relationship input) {
				return null != input.getDestination();
			}
		});
		
		final Collection<SnomedRelationship> transformedRelationships = Collections2.transform(existingRelationships, SnomedRelationship.CDOObjectConverterFunction.INSTANCE);
		return Collections2.filter(transformedRelationships, SnomedRelationship.ActivePredicate.INSTANCE);
	}

	@Override
	public List<LeafWidgetBean> createRelationshipDataTypeWidgetBeans(ConceptWidgetBean cwb, String... relationshipIds) {

		final List<LeafWidgetBean> beans = Lists.newArrayList();
		final CDOView view = concept.cdoView();
		
		final List<SnomedConcreteDataTypeRefSetMember> dataTypes = newArrayList();
		final List<String> relationshipIdList = Arrays.asList(relationshipIds);
		
		for (final Relationship relationship : concept.getOutboundRelationships()) {
			
			if (relationshipIdList.contains(relationship.getId())) {
				dataTypes.addAll(relationship.getConcreteDomainRefSetMembers());
			}
		}
		
		final RelationshipGroupWidgetModel groupModel = conceptWidgetModel.getRelationshipGroupContainerModel().getFirstMatching(GroupFlag.GROUPED);

		for (final SnomedConcreteDataTypeRefSetMember entry : dataTypes) {
			
			// Check unpersisted changes
			final SnomedConcreteDataTypeRefSetMember member = (SnomedConcreteDataTypeRefSetMember) CDOUtils.getObjectIfExists(view, entry.cdoID());
			
			if (member == null || !member.isActive()) {
				continue;
			}
			
			final DataTypeWidgetModel matchingModel = groupModel.getFirstMatching(entry.getLabel(), entry.getDataType());
			final DataTypeWidgetBean widgetBean = new DataTypeWidgetBean(cwb, matchingModel, entry.getReferencedComponentId(), entry.getUuid(), member.isReleased());
			widgetBean.setSelectedValue(entry.getSerializedValue());
			widgetBean.setSelectedLabel(entry.getLabel());
			widgetBean.setSelectedUom(member.getUomComponentId());
			beans.add(widgetBean);
		}
		
		return beans;
	}
		
	@Override
	public List<LeafWidgetBean> createDataTypeWidgetBeans(ConceptWidgetBean cwb) {

		final List<LeafWidgetBean> beans = Lists.newArrayList();
		
		final CDOView view = concept.cdoView();
		final List<SnomedConcreteDataTypeRefSetMember> dataTypes = concept.getConcreteDomainRefSetMembers();
		
		final DataTypeContainerWidgetModel dataTypeModel = conceptWidgetModel.getDataTypeContainerWidgetModel();
		
		final List<DataTypeWidgetModel> unusedModels = Lists.newArrayList(
				Lists.transform(dataTypeModel.getChildren(), new UncheckedCastFunction<WidgetModel, DataTypeWidgetModel>(DataTypeWidgetModel.class)));
		
		for (final SnomedConcreteDataTypeRefSetMember entry : dataTypes) {
			
			// Check unpersisted changes
			final SnomedConcreteDataTypeRefSetMember member = (SnomedConcreteDataTypeRefSetMember) CDOUtils.getObjectIfExists(view, entry.cdoID());
			
			if (member == null || !member.isActive()) {
				continue;
			}
			
			final DataTypeWidgetModel matchingModel = dataTypeModel.getFirstMatching(entry.getLabel(), entry.getDataType());
			final DataTypeWidgetBean widgetBean = new DataTypeWidgetBean(cwb, matchingModel, entry.getReferencedComponentId(), entry.getUuid(), member.isReleased());
			widgetBean.setSelectedValue(entry.getSerializedValue());
			widgetBean.setSelectedLabel(entry.getLabel());
			beans.add(widgetBean);
			unusedModels.remove(matchingModel);
		}
		
		for (final DataTypeWidgetModel unusedModel : unusedModels) {
			if (!unusedModel.isUnsanctioned()) {
				final DataTypeWidgetBean widgetBean = new DataTypeWidgetBean(cwb, unusedModel, concept.getId());
				if (includeUnsanctioned) {
					beans.add(widgetBean);
				} else {
					if (!StringUtils.isEmpty(widgetBean.getSelectedLabel()) && !StringUtils.isEmpty((String) widgetBean.getSelectedValue())) {
						beans.add(widgetBean);
					}
				}
			}
		}
		
		return beans;
	}
}