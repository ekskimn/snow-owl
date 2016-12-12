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

/**
 * @since 4.7
 */
public class LongRangePredicate extends Predicate {

	private final long from;
	private final long to;
	private boolean minInclusive;
	private boolean maxInclusive;

	LongRangePredicate(String field, long from, long to, boolean minInclusive, boolean maxInclusive) {
		super(field);
		this.from = from;
		this.to = to;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}
	
	public long from() {
		return from;
	}
	
	public long to() {
		return to;
	}
	
	public boolean isMinInclusive() {
		return minInclusive;
	}
	
	public boolean isMaxInclusive() {
		return maxInclusive;
	}
	
	@Override
	public String toString() {
		return String.format("%s is %s(%s) and %s(%s)", getField(), isMinInclusive() ? "gte" : "gt", from, isMaxInclusive() ? "lte" : "lt", to);
	}

}
