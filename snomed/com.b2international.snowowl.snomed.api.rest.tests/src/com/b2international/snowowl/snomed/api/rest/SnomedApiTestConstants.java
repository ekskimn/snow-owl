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
package com.b2international.snowowl.snomed.api.rest;

import java.util.Map;

import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.google.common.collect.ImmutableMap;

/**
 * Holds constants used in multiple test classes.
 * 
 * @since 2.0
 */
public abstract class SnomedApiTestConstants {

	/**
	 * The context-relative base URL for SNOMED CT-related controllers.
	 */
	public static String SCT_API = "/snomed-ct/v2";

	/**
	 * An acceptability map which specifies that the corresponding description is acceptable in the UK language reference set.
	 */
	public static final Map<?, ?> ACCEPTABLE_ACCEPTABILITY_MAP = ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_UK, Acceptability.ACCEPTABLE);

	/**
	 * An acceptability map which specifies that the corresponding description is preferred in the UK language reference set.
	 */
	public static final Map<?, ?> PREFERRED_ACCEPTABILITY_MAP = ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_UK, Acceptability.PREFERRED);

	/**
	 * An acceptability map with an invalid language reference set identifier.
	 */
	public static final Map<?, ?> INVALID_ACCEPTABILITY_MAP = ImmutableMap.of("1", Acceptability.PREFERRED);

	private SnomedApiTestConstants() {
		throw new UnsupportedOperationException("This class is not supposed to be instantiated.");
	}
}
