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

import org.slf4j.Logger;

/**
 */
public interface ISnomedPostProcessorContext {

	/**
	 * @return the SCT editing context for this post-processing run
	 */
	SnomedEditingContext getEditingContext();

	/**
	 * @return the identifier of the user associated with this import
	 */
	String getUserId();

	/**
	 * @return the import logger which accepts diagnostic output
	 */
	Logger getLogger();
}