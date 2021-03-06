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

import org.eclipse.emf.ecore.EPackage;

import com.b2international.snowowl.core.annotations.Client;
import com.b2international.snowowl.datastore.ActiveBranchPathAwareService;
import com.b2international.snowowl.snomed.SnomedPackage;
import com.b2international.snowowl.snomed.mrcm.core.widget.model.ConceptWidgetModel;

/**
 */
@Client
public class ClientWidgetModelProvider extends ActiveBranchPathAwareService implements IClientWidgetModelProvider {

	private final IWidgetModelProvider wrappedService;

	public ClientWidgetModelProvider(final IWidgetModelProvider wrappedService) {
		this.wrappedService = checkNotNull(wrappedService, "wrappedService");
	}

	@Override
	public ConceptWidgetModel createConceptWidgetModel(final String conceptId, final String ruleRefSetId) {
		return wrappedService.createConceptWidgetModel(getBranchPath(), conceptId, ruleRefSetId);
	}
	
	@Override
	public ConceptWidgetModel createConceptWidgetModel(final Iterable<String> ruleParentIds, final String ruleRefSetId) {
		return wrappedService.createConceptWidgetModel(getBranchPath(), ruleParentIds, ruleRefSetId);
	}

	@Override
	protected EPackage getEPackage() {
		return SnomedPackage.eINSTANCE;
	}
}