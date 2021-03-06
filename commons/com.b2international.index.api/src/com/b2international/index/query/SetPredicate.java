/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.query;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * @since 5.0
 */
public abstract class SetPredicate<T> extends Predicate {
	
	private final Set<T> values;

	SetPredicate(String field, Iterable<T> values) {
		super(field);
		this.values = ImmutableSet.copyOf(values);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getField(), values);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			final SetPredicate<?> other = (SetPredicate<?>) obj;
			return Objects.equals(values, other.values);
		}
		return false;
	}
	
	public Set<T> values() {
		return values;
	}
	
	@Override
	public String toString() {
		return String.format("%s IN(%s)", getField(), sublist());
	}
	
	private String sublist() {
		if (values.size() <= 10) {
			return values.toString();
		} else {
			final StringBuilder builder = new StringBuilder("[");
			final List<T> valuesList = Lists.newArrayList(values);
			
			for (int i = 0; i < 10; i++) {
				if (builder.length() != 1) {
					builder.append(", ");
				}
				
				builder.append(valuesList.get(i));
			}
			
			builder.append(String.format("... %d more items", values.size() - 10));
			builder.append("]");
			
			return builder.toString();
		}
	}

}
