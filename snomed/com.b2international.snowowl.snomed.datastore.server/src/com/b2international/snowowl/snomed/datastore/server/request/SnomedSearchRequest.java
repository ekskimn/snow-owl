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
package com.b2international.snowowl.snomed.datastore.server.request;

import java.util.List;

import org.apache.lucene.search.Filter;

import com.b2international.commons.functions.StringToLongFunction;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;

/**
 * @since 4.5
 */
public abstract class SnomedSearchRequest<R> extends SearchRequest<R> {

	enum OptionKey {
		
		/**
		 * Language reference sets to use
		 */
		LANGUAGE_REFSET,
		
		/**
		 * Concept status to match
		 */
		ACTIVE,
		
		/**
		 * Concept module ID to match
		 */
		MODULE
	}
	
	protected SnomedSearchRequest() {}
	
	protected List<Long> languageRefSetIds() {
		return getList(OptionKey.LANGUAGE_REFSET, Long.class);
	}
	
	@Override
	protected Filter createComponentIdFilter() {
		return SnomedMappings.id().createTermsFilter(StringToLongFunction.copyOf(componentIds()));
	}
}
