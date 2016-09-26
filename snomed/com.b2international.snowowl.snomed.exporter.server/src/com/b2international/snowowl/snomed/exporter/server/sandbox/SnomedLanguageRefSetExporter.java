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

import java.util.Set;

import org.apache.lucene.document.Document;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.exporter.server.SnomedRfFileNameBuilder;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;

/**
 * Exporter for language type reference sets.
 */
public class SnomedLanguageRefSetExporter extends SnomedRefSetExporter implements SnomedFileSwitchingExporter {

	private static final Set<String> FIELDS_TO_LOAD = SnomedMappings.fieldsToLoad().fields(COMMON_FIELDS_TO_LOAD).memberAcceptabilityId().build();
	private final IEventBus eventBus;
	
	public SnomedLanguageRefSetExporter(final SnomedExportConfiguration configuration, final String refSetId, final SnomedRefSetType type) {
		super(checkNotNull(configuration, "configuration"), checkNotNull(refSetId, "refSetId"), checkNotNull(type, "type"));
		ApplicationContext applicationContext = ApplicationContext.getInstance();
		eventBus = applicationContext.getService(IEventBus.class);
	}
	
	@Override
	public Set<String> getFieldsToLoad() {
		return FIELDS_TO_LOAD;
	}

	@Override
	public String transform(Document doc) {
		final StringBuilder sb = new StringBuilder();
		sb.append(super.transform(doc));
		sb.append(HT);
		sb.append(SnomedMappings.memberAcceptabilityId().getValueAsString(doc));
		return sb.toString();
	}
	
	@Override
	public String[] getColumnHeaders() {
		return SnomedRf2Headers.LANGUAGE_TYPE_HEADER;
	}
	
	@Override
	public String getFileName(String[] rows) {
		// der2_cRefset_LanguageDelta-en_INT_20160731.txt
		SnomedExportConfiguration configuration = getConfiguration();
		String referencedComponentId = rows[5];
		String languageCode = getLanguageCode(referencedComponentId, configuration);
		return new StringBuilder("der2_cRefset_Language")
			.append(String.valueOf(configuration.getContentSubType()))
			.append("-")
			.append(languageCode)
			.append("_")
			.append(configuration.getClientNamespace())
			.append("_")
			.append(SnomedRfFileNameBuilder.getReleaseDate(configuration))
			.append(".txt")
			.toString();
	}

	private String getLanguageCode(String referencedComponentId, SnomedExportConfiguration configuration) {
		ISnomedDescription description = SnomedRequests.prepareGetDescription().setComponentId(referencedComponentId).build(configuration.getCurrentBranchPath().getPath()).executeSync(eventBus);
		String languageCode = description.getLanguageCode();
		return languageCode;
	}
}
