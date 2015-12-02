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
package com.b2international.snowowl.snomed.datastore.server.request;

import java.util.Collection;
import java.util.List;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSetMembers;
import com.b2international.snowowl.snomed.datastore.SnomedRefSetBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.server.converter.SnomedConverters;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * @since 4.5
 */
final class SnomedRefSetMemberSearchRequest extends SnomedSearchRequest<SnomedReferenceSetMembers> {

	SnomedRefSetMemberSearchRequest() {}
	
	@Override
	protected SnomedReferenceSetMembers doExecute(BranchContext context) {
		final IBranchPath branchPath = context.branch().branchPath();
		final SnomedRefSetBrowser browser = context.service(SnomedRefSetBrowser.class);
		// TODO convert this to proper index query when index API is ready
		// TODO fix collection like parameters
		final Collection<String> referenceSetIds = options().getCollection(SnomedMappings.memberRefSetId().fieldName(), String.class);
		final List<SnomedRefSetMemberIndexEntry> members = FluentIterable
			.from(browser.getAllRefSetIds(branchPath))
			.filter(new Predicate<String>() {
				@Override
				public boolean apply(String refSetId) {
					return referenceSetIds.isEmpty() || referenceSetIds.contains(refSetId);
				}
			})
			.transformAndConcat(new Function<String, Iterable<? extends SnomedRefSetMemberIndexEntry>>() {
				@Override
				public Iterable<? extends SnomedRefSetMemberIndexEntry> apply(String refSetId) {
					return browser.getMembers(branchPath, refSetId);
				}
			})
			.skip(offset())
			.limit(limit())
			.toList();
		
		return SnomedConverters.newMemberConverter(context, expand(), locales()).convert(members, offset(), limit(), -1);
	}

	@Override
	protected Class<SnomedReferenceSetMembers> getReturnType() {
		return SnomedReferenceSetMembers.class;
	}

}
