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

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSets;
import com.b2international.snowowl.snomed.datastore.converter.SnomedConverters;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedQueryBuilder;
import com.google.common.collect.ImmutableList;

/**
 * @since 4.5
 */
final class SnomedRefSetSearchRequest extends SnomedSearchRequest<SnomedReferenceSets> {

	@Override
	protected SnomedReferenceSets doExecute(BranchContext context) throws IOException {
		final IndexSearcher searcher = context.service(IndexSearcher.class);
		final SnomedQueryBuilder queryBuilder = SnomedMappings.newQuery().refSet();
		
		final Query query = createConstantScoreQuery(queryBuilder.matchAll());
		final int totalHits = getTotalHits(searcher, query);
		
		if (limit() < 1 || totalHits < 1) {
			return new SnomedReferenceSets(offset(), limit(), totalHits);
		}
		
		final TopDocs topDocs = searcher.search(query, null, numDocsToRetrieve(searcher, totalHits), Sort.INDEXORDER, false, false);
		if (topDocs.scoreDocs.length < 1) {
			return new SnomedReferenceSets(offset(), limit(), topDocs.totalHits);
		}

		final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		final ImmutableList.Builder<SnomedRefSetIndexEntry> refSetBuilder = ImmutableList.builder();
		
		for (int i = offset(); i < scoreDocs.length && i < offset() + limit(); i++) {
			Document doc = searcher.doc(scoreDocs[i].doc); // TODO: should expand & filter drive fieldsToLoad? Pass custom fieldValueLoader?
			SnomedRefSetIndexEntry indexEntry = SnomedRefSetIndexEntry.builder(doc).build();
			refSetBuilder.add(indexEntry);
		}

		List<SnomedRefSetIndexEntry> referenceSets = refSetBuilder.build();
		return SnomedConverters.newRefSetConverter(context, expand(), locales()).convert(referenceSets, offset(), limit(), topDocs.totalHits);
	}
	
	@Override
	protected Class<SnomedReferenceSets> getReturnType() {
		return SnomedReferenceSets.class;
	}
}