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
package com.b2international.snowowl.snomed.mrcm.core.extensions;

import java.util.Collection;

import org.eclipse.emf.spi.cdo.FSMUtil;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.Concept;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Extension point based provider for concept model extensions.
 * 
 */
public class ExtensionBasedConceptModelExtensionProvider extends AbstractConceptExtensionProvider<IConceptModelExtension> implements IConceptModelExtensionProvider {

	/**
	 * Creates a new extension based concept model enhancing provider.
	 */
	public ExtensionBasedConceptModelExtensionProvider() {
		super(IConceptModelExtension.class);
	}

	@Override
	protected String getExtensionPointId() {
		return ExtensionConstants.CONCEPT_MODEL_EXT_POINT_ID;
	}

	@Override
	public Collection<IConceptModelExtension> getModelExtensions(final Concept concept) {
		
		Preconditions.checkNotNull(concept, "Concept reference may not be null.");

		if (concept.cdoView().isDirty()) {
			return getExtensions(concept);
		} else {
			return getModelExtensions(BranchPathUtils.createPath(concept), Long.valueOf(concept.getId()));
		}
	}

	@Override
	public Collection<IConceptModelExtension> getModelExtensions(final IBranchPath branchPath, final long conceptId) {
		
		initializeElements();
		final Collection<IConceptModelExtension> elements = Lists.newArrayList();

		for (final IConceptModelExtension modelExtension : registeredExtensions.values()) {
			if (modelExtension.handlesConceptId(branchPath, conceptId)) {
				elements.add(modelExtension);
			}
		}

		return elements;
	}
}