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
package com.b2international.snowowl.snomed.exporter.server.sandbox;

import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.google.common.base.Preconditions.checkNotNull;

import org.eclipse.emf.cdo.transaction.CDOTransaction;

import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.Query;
import com.b2international.index.query.Query.QueryBuilder;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.cdo.CDOTransactionFunction;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetLookupService;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.b2international.snowowl.snomed.datastore.services.ISnomedConceptNameProvider;
import com.b2international.snowowl.snomed.exporter.server.ComponentExportType;
import com.b2international.snowowl.snomed.exporter.server.SnomedRf2Exporter;
import com.b2international.snowowl.snomed.exporter.server.SnomedRfFileNameBuilder;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.collect.Sets;

/**
 * Base for all SNOMED&nbsp;CT reference set RF2 exporters.
 *
 */
public class SnomedRefSetExporter extends SnomedCoreExporter<SnomedRefSetMemberIndexEntry> implements SnomedRf2Exporter {

	private final String refSetId;

	private SnomedRefSetType type;

	public SnomedRefSetExporter(final SnomedExportContext configuration, final String refSetId, final SnomedRefSetType type) {
		super(configuration, SnomedRefSetMemberIndexEntry.class);
		this.refSetId = checkNotNull(refSetId, "refSetId");
		this.type = checkNotNull(type, "type");
	}

	@Override
	public String transform(final SnomedRefSetMemberIndexEntry doc) {
		final StringBuilder sb = new StringBuilder();
		sb.append(doc.getId());
		sb.append(HT);
		sb.append(formatEffectiveTime(doc.getEffectiveTime()));
		sb.append(HT);
		sb.append(doc.isActive() ? "1" : "0");
		sb.append(HT);
		sb.append(doc.getModuleId());
		sb.append(HT);
		sb.append(doc.getReferenceSetId());
		sb.append(HT);
		sb.append(doc.getReferencedComponentId());
		return sb.toString();
	}
	
	@Override
	public ComponentExportType getType() {
		return ComponentExportType.REF_SET;
	}
	
	@Override
	public String[] getColumnHeaders() {
		return SnomedRf2Headers.SIMPLE_TYPE_HEADER;
	}
	
	@Override
	public String getRelativeDirectory() {
		switch (type) {
			case SIMPLE: //$FALL-THROUGH$
			case ASSOCIATION: //$FALL-THROUGH$
			case CONCRETE_DATA_TYPE: //$FALL-THROUGH$
			case QUERY: //$FALL-THROUGH$
			case ATTRIBUTE_VALUE:  
				return RF2_CONTENT_REFERENCE_SET_RELATIVE_DIR;
			case EXTENDED_MAP: //$FALL-THROUGH$
			case SIMPLE_MAP: //$FALL-THROUGH$
			case COMPLEX_MAP: 
				return RF2_MAP_REFERENCE_SET_RELATIVE_DIR;
			case DESCRIPTION_TYPE: //$FALL-THROUGH$
			case MODULE_DEPENDENCY: 
				return RF2_METADATA_REFERENCE_SET_RELATIVE_DIR;
			case LANGUAGE: 
				return RF2_LANGUAGE_REFERENCE_SET_RELATIVE_DIR;
			default: throw new IllegalArgumentException("Unknown SNOMED CT reference set type: " + type);
		}
	}
	
	@Override
	public String getFileName() {
		final ICDOConnection connection = getServiceForClass(ICDOConnectionManager.class).getByUuid(SnomedDatastoreActivator.REPOSITORY_UUID);
		final IBranchPath branchPath = getExportContext().getCurrentBranchPath();
		final String refSetName = getServiceForClass(ISnomedConceptNameProvider.class).getComponentLabel(branchPath, refSetId);
		return CDOUtils.apply(new CDOTransactionFunction<String>(connection, branchPath) {
			@Override
			protected String apply(final CDOTransaction transaction) {
				final SnomedRefSet refSet = new SnomedRefSetLookupService().getComponent(getRefSetId(), transaction);
				return buildRefSetFileName(refSetName, refSet);
			}
		});
	}
	
	protected String buildRefSetFileName(final String refSetName, final SnomedRefSet refSet) {
		return SnomedRfFileNameBuilder.buildRefSetFileName(getExportContext(), refSetName, refSet);
	}

	@Override
	protected Query<SnomedRefSetMemberIndexEntry> getSnapshotQuery() {
		
		QueryBuilder<SnomedRefSetMemberIndexEntry> builder = Query.builder(SnomedRefSetMemberIndexEntry.class);
		ExpressionBuilder commitTimeConditionBuilder = Expressions.builder();
		commitTimeConditionBuilder.must(SnomedRefSetMemberIndexEntry.Expressions.referenceSetId(Sets.newHashSet(getRefSetId()))).build();
		Query<SnomedRefSetMemberIndexEntry> query = builder.selectAll().where(commitTimeConditionBuilder.build()).limit(getPageSize()).offset(getCurrentOffset()).build();
		return query;
	}
	
	/**Returns with the reference set identifier concept ID.*/
	protected String getRefSetId() {
		return refSetId;
	}
}
