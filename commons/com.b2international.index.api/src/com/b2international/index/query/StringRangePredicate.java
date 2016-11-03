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

import java.util.Objects;

/**
 * @since 4.7
 */
public class StringRangePredicate extends Predicate {

	private final String from;
	private final String to;

	StringRangePredicate(String field, String from, String to) {
		super(field);
		this.from = from;
		this.to = to;
	}
	
	public String from() {
		return from;
	}
	
	public String to() {
		return to;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getField(), from(), to());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			final StringRangePredicate other = (StringRangePredicate) obj;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s is gte(%s) and lte(%s)", getField(), from, to);
	}
	
}
