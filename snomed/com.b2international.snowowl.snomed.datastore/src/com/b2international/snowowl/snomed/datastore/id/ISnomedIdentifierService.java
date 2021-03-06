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
package com.b2international.snowowl.snomed.datastore.id;

import java.util.Collection;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.cis.SctId;

/**
 * SNOMED CT Identifier service interface. It is capable of generating valid
 * SNOMED CT Identifiers according to the Technical Implementation Guide.
 * 
 * IDs are generated by defining the {@link ComponentCategory} (the component
 * type we need ID for), and an optional namespace parameter. The returned
 * SNOMED CT ID will have a valid check-digit, partition-identifier, and the
 * optional namespace identifier.
 * 
 * @since 4.0
 * @see www.snomed.org/tig?t=trg_app_sctid
 */
public interface ISnomedIdentifierService {

	/**
	 * Generates a single SNOMED CT ID for the defined {@link ComponentCategory}
	 * with the defined extension namespace.
	 * 
	 * @param namespace
	 *            the extension namespace to use when generating the ID
	 * @param category
	 *            the component type to generate ID for
	 * @return a valid SNOMED CT identifier, never <code>null</code>
	 */
	String generate(String namespace, ComponentCategory category);

	/**
	 * Registers a single SNOMED CT ID.
	 * 
	 * @param componnetId
	 *            the ID to register.
	 */
	void register(String componentId);

	/**
	 * Reserves a single SNOMED CT ID for the defined {@link ComponentCategory}
	 * with the defined extension namespace.
	 * 
	 * @param namespace
	 *            the extension namespace to use when generating the ID
	 * @param category
	 *            the component type to generate ID for
	 * @return a valid SNOMED CT identifier, never <code>null</code>
	 */
	String reserve(String namespace, ComponentCategory category);

	/**
	 * Deprecates the given SNOMED CT ID.
	 * 
	 * @param componentId
	 *            the ID to deprecate.
	 */
	void deprecate(String componentId);

	/**
	 * Releases a single SNOMED CT ID.
	 * 
	 * @param componentId
	 *            the ID to release.
	 */
	void release(String componentId);

	/**
	 * Publishes a single SNOMED CT ID.
	 * 
	 * @param componentId
	 *            the ID to publish.
	 */
	void publish(String componentId);

	/**
	 * Gets the {@link SctId} for the given component ID.
	 * 
	 * @param componentId
	 *            the ID of the component.
	 */
	SctId getSctId(String componentId);

	/**
	 * Generates multiple SNOMED CT IDs for the defined
	 * {@link ComponentCategory} with the defined extension namespace.
	 * 
	 * @param namespace
	 *            the extension namespace to use when generating the ID
	 * @param category
	 *            the component type to generate ID for
	 * @param quantity
	 *            the number of IDs to generate.
	 * @return a collection of generated IDs.
	 */
	Collection<String> generate(String namespace, ComponentCategory category, int quantity);

	/**
	 * Registers multiple SNOMED CT IDs.
	 * 
	 * @param componnetIds
	 *            the IDs to register.
	 */
	void register(Collection<String> componentIds);

	/**
	 * Reserves multiple SNOMED CT ID for the defined {@link ComponentCategory}
	 * with the defined extension namespace.
	 * 
	 * @param namespace
	 *            the extension namespace to use when generating the ID
	 * @param category
	 *            the component type to generate ID for
	 * @return a collection of reserved IDs.
	 */
	Collection<String> reserve(String namespace, ComponentCategory category, int quantity);

	/**
	 * Releases the given SNOMED CT IDs.
	 * 
	 * @param componnetIds
	 *            the IDs to release.
	 */
	void release(Collection<String> componentIds);

	/**
	 * Deprecates the given SNOMED CT IDs.
	 * 
	 * @param componentIds
	 *            the IDs to deprecate.
	 */
	void deprecate(Collection<String> componentIds);

	/**
	 * Publishes the given SNOMED CT IDs.
	 * 
	 * @param componentIds
	 *            the IDs to publish.
	 */
	void publish(Collection<String> componentIds);

	/**
	 * Gets the {@link SctId}s for the given component IDs.
	 * 
	 * @param componentIds
	 *            the IDs of the components.
	 */
	Collection<SctId> getSctIds(Collection<String> componentIds);

	/**
	 * Gets all SNOMED identifiers stored in the system.
	 */
	Collection<SctId> getSctIds();

	/**
	 * Tells if the identifier service supports import related requests.
	 */
	boolean importSupported();

}
