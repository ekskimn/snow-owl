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
package com.b2international.snowowl.snomed.datastore.request;


import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.group;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.unionGroup;

import java.io.IOException;
import java.util.Collection;

import com.b2international.index.Hits;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.Query;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.converter.SnomedConverters;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.google.common.base.Function;

/**
 * @since 4.5
 */
final class SnomedRelationshipSearchRequest extends SnomedSearchRequest<SnomedRelationships> {

	enum OptionKey {
		SOURCE,
		TYPE,
		DESTINATION,
		CHARACTERISTIC_TYPE, 
		GROUP_MIN,
		GROUP_MAX,
		UNION_GROUP,
		MODIFIER
	}
	
	SnomedRelationshipSearchRequest() {}

	@Override
	protected SnomedRelationships doExecute(BranchContext context) throws IOException {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		
		final ExpressionBuilder queryBuilder = Expressions.builder();
		addActiveClause(queryBuilder);
		addModuleClause(queryBuilder);
		addComponentIdFilter(queryBuilder);
		addEffectiveTimeClause(queryBuilder);
		addEclFilter(context, queryBuilder, OptionKey.SOURCE, new Function<Collection<String>, Expression>() {
			@Override
			public Expression apply(Collection<String> input) {
				return SnomedRelationshipIndexEntry.Expressions.sourceIds(input);
			}
		});
		addEclFilter(context, queryBuilder, OptionKey.TYPE, new Function<Collection<String>, Expression>() {
			@Override
			public Expression apply(Collection<String> input) {
				return SnomedRelationshipIndexEntry.Expressions.typeIds(input);
			}
		});
		addEclFilter(context, queryBuilder, OptionKey.DESTINATION, new Function<Collection<String>, Expression>() {
			@Override
			public Expression apply(Collection<String> input) {
				return SnomedRelationshipIndexEntry.Expressions.destinationIds(input);
			}
		});
		addEclFilter(context, queryBuilder, OptionKey.CHARACTERISTIC_TYPE, new Function<Collection<String>, Expression>() {
			@Override
			public Expression apply(Collection<String> input) {
				return SnomedRelationshipIndexEntry.Expressions.characteristicTypeIds(input);
			}
		});
		addEclFilter(context, queryBuilder, OptionKey.MODIFIER, new Function<Collection<String>, Expression>() {
			@Override
			public Expression apply(Collection<String> input) {
				return SnomedRelationshipIndexEntry.Expressions.modifierIds(input);
			}
		});
		
		if (containsKey(OptionKey.GROUP_MIN) || containsKey(OptionKey.GROUP_MAX)) {
			final int from = containsKey(OptionKey.GROUP_MIN) ? get(OptionKey.GROUP_MIN, Integer.class) : 0;
			final int to = containsKey(OptionKey.GROUP_MAX) ? get(OptionKey.GROUP_MAX, Integer.class) : Integer.MAX_VALUE;
			queryBuilder.must(group(from, to));
		}
		
		if (containsKey(OptionKey.UNION_GROUP)) {
			queryBuilder.must(unionGroup(get(OptionKey.UNION_GROUP, Integer.class)));
		}
		
		final Hits<SnomedRelationshipIndexEntry> hits = searcher.search(Query.selectPartial(SnomedRelationshipIndexEntry.class, fields())
				.where(queryBuilder.build())
				.offset(offset())
				.limit(limit())
				.build());
		final int totalHits = hits.getTotal();
		
		if (limit() < 1 || totalHits < 1) {
			return new SnomedRelationships(offset(), limit(), totalHits);
		}
		
		return SnomedConverters.newRelationshipConverter(context, expand(), locales()).convert(hits.getHits(), offset(), limit(), totalHits);
	}
	
	@Override
	protected SnomedRelationships createEmptyResult(int offset, int limit) {
		return new SnomedRelationships(offset, limit, 0);
	}

	@Override
	protected Class<SnomedRelationships> getReturnType() {
		return SnomedRelationships.class;
	}
}
