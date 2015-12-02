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
package com.b2international.snowowl.snomed.datastore.server.converter;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.domain.CollectionResource;
import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.core.domain.SnomedComponent;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.b2international.snowowl.snomed.datastore.services.AbstractSnomedRefSetMembershipLookupService;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * @since 4.0
 * @param <T>
 * @param <R>
 * @param <CR>
 */
abstract class BaseSnomedComponentConverter<T extends SnomedIndexEntry, R extends SnomedComponent, CR extends CollectionResource<R>>
		implements ResourceConverter<T, R, CR> {

	protected static final Function<IComponent, String> ID_FUNCTION = new Function<IComponent, String>() {
		@Override
		public String apply(IComponent input) {
			return input.getId();
		}
	};
	
	private final BranchContext context;
	private final List<String> expand;
	private final AbstractSnomedRefSetMembershipLookupService refSetMembershipLookupService;
	private final List<ExtendedLocale> locales;

	protected BaseSnomedComponentConverter(BranchContext context, List<String> expand, List<ExtendedLocale> locales, AbstractSnomedRefSetMembershipLookupService refSetMembershipLookupService) {
		this.context = checkNotNull(context, "context");
		this.expand = expand == null ? Collections.<String> emptyList() : expand;
		this.locales = locales == null ? Collections.<ExtendedLocale>emptyList() : locales;
		this.refSetMembershipLookupService = refSetMembershipLookupService;
	}

	protected final List<String> expand() {
		return expand;
	}

	protected final BranchContext context() {
		return context;
	}
	
	protected final List<ExtendedLocale> locales() {
		return locales;
	}

	@Override
	public final R convert(T component) {
		return convert(Collections.singleton(component), 0, 1, 1).getItems().iterator().next();
	}

	@Override
	public final CR convert(Collection<T> components, int offset, int limit, int total) {
		final List<R> results = FluentIterable.from(components).transform(new Function<T, R>() {
			@Override
			public R apply(T input) {
				return toResource(input);
			}
		}).toList();
		expand(results);
		return createCollectionResource(results, offset, limit, total);
	}

	protected abstract CR createCollectionResource(List<R> results, int offset, int limit, int total);

	/**
	 * Subclasses may override to expand resources based on the {@link #expand()} list.
	 * 
	 * @param results
	 */
	protected void expand(List<R> results) {
	}

	protected abstract R toResource(T entry);

	protected final Date toEffectiveTime(final long effectiveTimeAsLong) {
		return EffectiveTimes.toDate(effectiveTimeAsLong);
	}

	protected final AbstractSnomedRefSetMembershipLookupService getRefSetMembershipLookupService() {
		return refSetMembershipLookupService;
	}

	protected final Multimap<AssociationType, String> toAssociationTargets(final String type, final String id) {
		final ImmutableMultimap.Builder<AssociationType, String> resultBuilder = ImmutableMultimap.builder();

		for (final AssociationType associationType : AssociationType.values()) {
			// TODO: it might be quicker to collect the refset IDs first and retrieve all members with a single call
			final Collection<SnomedRefSetMemberIndexEntry> members = getRefSetMembershipLookupService().getMembers(type,
					ImmutableList.of(associationType.getConceptId()), id);

			for (final SnomedRefSetMemberIndexEntry member : members) {
				// FIXME: inactive inactivation indicators are shown in the desktop form UI
				if (member.isActive()) {
					resultBuilder.put(associationType, member.getTargetComponentId());
				}
			}
		}

		return resultBuilder.build();
	}

}