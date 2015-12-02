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
package com.b2international.snowowl.datastore.server.snomed.index;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.NumericDocValues;

import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;

import bak.pcj.LongCollection;

/**
 * Class for collecting SNOMED CT relationship properties.
 * <p>
 * This class supplies a mapping for relationships, where keys are relationship storage keys,
 * and values are primitive long arrays with the following content:
 * <ul>
 * <li>Relationship characteristic type concept ID</li>
 * <li>Relationship module concept ID</li>
 * <li>Relationship attribute/type concept ID</li>
 * <li>Relationship source concept ID</li>
 * <li>Relationship destination concept ID</li>
 * <li>Relationship modified concept ID</li>
 * </ul>
 */
public class RelationshipPropertyCollector extends ComponentPropertyCollector {

	private static final long UNIVERSAL = Long.parseLong(Concepts.UNIVERSAL_RESTRICTION_MODIFIER);
	private static final long EXISTENTIAL = Long.parseLong(Concepts.EXISTENTIAL_RESTRICTION_MODIFIER);

	private NumericDocValues characteristicTypeIds;
	private NumericDocValues typeIds;
	private NumericDocValues sourceIds;
	private NumericDocValues destinationIds;
	private NumericDocValues modifierFlags;
	private NumericDocValues moduleIds;

	public RelationshipPropertyCollector(final LongCollection acceptedIds) {
		super(checkNotNull(acceptedIds, "acceptedIds"));
	}

	@Override
	protected void initDocValues(final AtomicReader leafReader) throws IOException {
		super.initDocValues(leafReader);
		characteristicTypeIds = SnomedMappings.relationshipCharacteristicType().getDocValues(leafReader);
		moduleIds = SnomedMappings.module().getDocValues(leafReader);
		typeIds = SnomedMappings.relationshipType().getDocValues(leafReader);
		sourceIds = SnomedMappings.relationshipSource().getDocValues(leafReader);
		destinationIds = SnomedMappings.relationshipDestination().getDocValues(leafReader);
		modifierFlags = SnomedMappings.relationshipUniversal().getDocValues(leafReader);
	}

	@Override
	protected boolean isLeafCollectible() {
		return super.isLeafCollectible()
				&& characteristicTypeIds != null 
				&& moduleIds != null
				&& typeIds != null 
				&& sourceIds != null 
				&& destinationIds != null 
				&& modifierFlags != null; 
	}

	@Override
	protected long[] collectProperties(final int docId) {
		return new long[] { 
				characteristicTypeIds.get(docId),
				moduleIds.get(docId),
				typeIds.get(docId), 
				sourceIds.get(docId), 
				destinationIds.get(docId), 
				getModifierId(docId) 
		};
	}

	private long getModifierId(final int docId) {
		return (modifierFlags.get(docId) == 1L) ? UNIVERSAL : EXISTENTIAL;
	}
}
