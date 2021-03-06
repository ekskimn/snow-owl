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
package com.b2international.snowowl.snomed.datastore;

import java.util.Iterator;

import com.b2international.snowowl.core.api.browser.IClientTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.mrcm.EnumeratedConceptSetDefinition;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class EnumeratedConceptSetProcessor extends ConceptSetProcessor<EnumeratedConceptSetDefinition> {

	private final IClientTerminologyBrowser<SnomedConceptDocument, String> terminologyBrowser;
	
	public EnumeratedConceptSetProcessor(EnumeratedConceptSetDefinition conceptSetDefinition, IClientTerminologyBrowser<SnomedConceptDocument, String> terminologyBrowser2) {
		super(conceptSetDefinition);
		this.terminologyBrowser = terminologyBrowser2;
	}
	
	@Override
	public Iterator<SnomedConceptDocument> getConcepts() {
		return Iterators.transform(conceptSetDefinition.getConceptIds().iterator(), new IdToConceptMiniFunction());
	}
	
	private final class IdToConceptMiniFunction implements Function<String, SnomedConceptDocument> {
		@Override public SnomedConceptDocument apply(String input) {
			return terminologyBrowser.getConcept(input);
		}
	}
}