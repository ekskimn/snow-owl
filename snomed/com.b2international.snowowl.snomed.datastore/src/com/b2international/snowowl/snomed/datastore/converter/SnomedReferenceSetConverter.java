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
package com.b2international.snowowl.snomed.datastore.converter;

import java.util.List;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.commons.options.Options;
import com.b2international.snowowl.core.CoreTerminologyBroker;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSet;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSetImpl;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSets;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetIndexEntry;
import com.b2international.snowowl.snomed.datastore.request.SnomedRefSetMemberSearchRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.datastore.services.AbstractSnomedRefSetMembershipLookupService;

/**
 * @since 4.5
 */
final class SnomedReferenceSetConverter extends BaseSnomedComponentConverter<SnomedRefSetIndexEntry, SnomedReferenceSet, SnomedReferenceSets> {
	
	protected SnomedReferenceSetConverter(BranchContext context, Options expand, List<ExtendedLocale> locales, AbstractSnomedRefSetMembershipLookupService refSetMembershipLookupService) {
		super(context, expand, locales, refSetMembershipLookupService);
	}

	@Override
	protected SnomedReferenceSets createCollectionResource(List<SnomedReferenceSet> results, int offset, int limit, int total) {
		return new SnomedReferenceSets(results, offset, limit, total);
	}
	
	@Override
	protected void expand(List<SnomedReferenceSet> results) {
		expandMembers(results);
	}

	private void expandMembers(List<SnomedReferenceSet> results) {
		if (expand().containsKey("members")) {
			Options expandOptions = expand().get("members", Options.class);
			
			for (SnomedReferenceSet refSet : results) {
				SnomedRefSetMemberSearchRequestBuilder req = SnomedRequests.prepareSearchMember()
						.filterByRefSet(refSet.getId())
						.setLocales(locales())
						.setExpand(expandOptions.get("expand", Options.class));
				
				if (expandOptions.containsKey("offset")) {
					req.setOffset(expandOptions.get("offset", Integer.class));
				}
				
				if (expandOptions.containsKey("limit")) {
					req.setLimit(expandOptions.get("limit", Integer.class));
				}

				((SnomedReferenceSetImpl) refSet).setMembers(req.build().execute(context()));
			}
		}
	}
	
	@Override
	public SnomedReferenceSet toResource(SnomedRefSetIndexEntry entry) {
		final SnomedReferenceSetImpl refset = new SnomedReferenceSetImpl();
		refset.setId(entry.getId());
		refset.setEffectiveTime(EffectiveTimes.toDate(entry.getEffectiveTimeAsLong()));
		refset.setActive(entry.isActive());
		refset.setReleased(entry.isReleased());
		refset.setModuleId(entry.getModuleId());
		refset.setIconId(entry.getIconId());
		final short referencedComponentType = entry.getReferencedComponentType();
		refset.setReferencedComponent(getReferencedComponentType(referencedComponentType));
		refset.setType(entry.getType());
		return refset;
	}

	private String getReferencedComponentType(final short referencedComponentType) {
		return CoreTerminologyBroker.getInstance().getComponentInformation(referencedComponentType).getId();
	}
}