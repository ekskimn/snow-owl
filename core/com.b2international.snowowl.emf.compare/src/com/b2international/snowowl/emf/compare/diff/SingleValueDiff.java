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
package com.b2international.snowowl.emf.compare.diff;

import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Represents a {@link Diff difference} between two state of 
 * a single valued {@link EStructuralFeature feature}.
 *
 */
public interface SingleValueDiff<F extends EStructuralFeature, V> extends Diff<F, V> {

	/**
	 * Returns with the previous value of the actual feature.
	 * @return the old value.
	 */
	V getOldValue();
	
}