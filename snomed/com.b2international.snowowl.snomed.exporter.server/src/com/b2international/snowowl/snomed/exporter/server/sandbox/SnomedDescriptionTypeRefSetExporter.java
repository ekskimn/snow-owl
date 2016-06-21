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

import static com.google.common.base.Preconditions.checkNotNull;

import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;

/**
 * SNOMED CT description format type reference set exporter.
 */
public class SnomedDescriptionTypeRefSetExporter extends SnomedRefSetExporter {

	public SnomedDescriptionTypeRefSetExporter(final SnomedExportContext configuration, final String refSetId, final SnomedRefSetType type) {
		super(checkNotNull(configuration, "configuration"), checkNotNull(refSetId, "refSetId"), checkNotNull(type, "type"));
	}
	
	@Override
	public String transform(SnomedRefSetMemberIndexEntry doc) {
		final StringBuilder sb = new StringBuilder();
		sb.append(super.transform(doc));
		sb.append(HT);
		sb.append(doc.getDescriptionFormat());
		sb.append(HT);
		sb.append(doc.getDescriptionLength());
		return sb.toString();
	}
	
	@Override
	public String[] getColumnHeaders() {
		return SnomedRf2Headers.DESCRIPTION_TYPE_HEADER;
	}
}
