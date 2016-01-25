/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.b2international.snowowl.snomed.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.browser.BranchSpecificClientTerminologyBrowser;
import com.b2international.snowowl.dsl.SCGStandaloneSetup;
import com.b2international.snowowl.dsl.scg.Attribute;
import com.b2international.snowowl.dsl.scg.Concept;
import com.b2international.snowowl.dsl.scg.Expression;
import com.b2international.snowowl.dsl.scg.Group;
import com.b2international.snowowl.semanticengine.normalform.ScgExpressionNormalFormGenerator;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.impl.SnomedBrowserService.Form;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.datastore.RecursiveTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedClientStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.google.common.base.Stopwatch;

public class SnomedBrowserFormBuilder {

	final Logger logger = LoggerFactory.getLogger(getClass());

	public Map<Form, List<SnomedRelationshipIndexEntry>> toFormRelationships(String conceptId, List<SnomedRelationshipIndexEntry> relationships,
			IBranchPath branchPath, List<String> representationalForms) {

		Map<Form, List<SnomedRelationshipIndexEntry>> formRelationships = new HashMap<>();
		formRelationships.put(Form.STATED, new ArrayList<SnomedRelationshipIndexEntry>());
		formRelationships.put(Form.INFERRED, new ArrayList<SnomedRelationshipIndexEntry>());

		for (SnomedRelationshipIndexEntry relationship : relationships) {
			Form form = relationship.getCharacteristicType() == CharacteristicType.STATED_RELATIONSHIP ? Form.STATED : Form.INFERRED;
			formRelationships.get(form).add(relationship);
		}

		if (representationalForms != null && representationalForms.contains(Form.SHORT_NORMAL.name().toLowerCase().replace("_", "-"))) {
			final Stopwatch stopwatch = Stopwatch.createStarted();
			formRelationships.put(Form.SHORT_NORMAL, getShortNormalForm(conceptId, branchPath));
			logger.info("Short Normal form generation took {}", stopwatch.toString());
		}

		return formRelationships;
	}

	private List<SnomedRelationshipIndexEntry> getShortNormalForm(String conceptId, IBranchPath branchPath) {
		BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> branchSpecificTerminologyBrowser = 
				new BranchSpecificClientTerminologyBrowser<>(getTerminologyBrowser(), branchPath);
		RecursiveTerminologyBrowser<SnomedConceptIndexEntry, String> recursiveTerminologyBrowser = RecursiveTerminologyBrowser.create(branchSpecificTerminologyBrowser);
		final ScgExpressionNormalFormGenerator scgExpressionNormalFormGenerator = 
				new ScgExpressionNormalFormGenerator(recursiveTerminologyBrowser, new SnomedClientStatementBrowser(getStatementBrowser()));
		Expression expression = (Expression) SCGStandaloneSetup.parse(conceptId.toString());
		final Expression shortNormalForm = scgExpressionNormalFormGenerator.getShortNormalForm(expression);

		List<SnomedRelationshipIndexEntry> relationships = new ArrayList<>();
		int groupNumber = 0;
		for (Concept concept : shortNormalForm.getConcepts()) {
			relationships.add(SnomedRelationshipIndexEntry
					.builder()
					.id(UUID.randomUUID().toString())
					.sourceId(conceptId)
					.typeId(Concepts.IS_A)
					.destinationId(concept.getId())
					.group((byte) groupNumber)
					.active(true)
					.moduleId("")
					.characteristicTypeId("")
					.modifierId("")
					.build());
		}
		toRelationships(shortNormalForm.getAttributes(), conceptId, groupNumber, relationships);
		
		for (Group group : shortNormalForm.getGroups()) {
			groupNumber++;
			toRelationships(group.getAttributes(), conceptId, groupNumber, relationships);
		}

		return relationships;
	}

	private void toRelationships(
			final EList<Attribute> attributes,
			String conceptId,
			final int group,
			List<SnomedRelationshipIndexEntry> relationships) {
		for (Attribute attribute : attributes) {
			relationships.add(SnomedRelationshipIndexEntry
				.builder()
				.id(UUID.randomUUID().toString())
				.sourceId(conceptId)
				.typeId(attribute.getName().getId())
				.destinationId(attribute.getValue().toString())
				.group((byte) group)
				.active(true)
				.moduleId("")
				.characteristicTypeId("")
				.modifierId("")
				.build());
		}
	}

	private static SnomedTerminologyBrowser getTerminologyBrowser() {
		return ApplicationContext.getServiceForClass(SnomedTerminologyBrowser.class);
	}

	private static SnomedStatementBrowser getStatementBrowser() {
		return ApplicationContext.getServiceForClass(SnomedStatementBrowser.class);
	}

}
