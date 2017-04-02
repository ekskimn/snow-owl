package com.b2international.snowowl.snomed.api.impl.domain;

import java.util.Map;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.snomed.SnomedConstants;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserComponent;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public abstract class AbstractInputCreator {

	private Branch branch;

	public AbstractInputCreator(final Branch branch) {
		this.branch = branch;
	}

	protected String getModuleOrDefault(ISnomedBrowserComponent component) {
		final String moduleId = component.getModuleId();
		return moduleId != null ? moduleId : SnomedConstants.Concepts.MODULE_SCT_CORE;
	}
	
	protected <K, V> Map<K, V> nullToEmptyMap(final Map<K, V> map) {
		return map != null ? map : ImmutableMap.<K, V>of();
	}
	
	protected <K, V> Multimap<K, V> nullToEmptyMultimap(final Multimap<K, V> multimap) {
		return multimap != null ? multimap : ImmutableMultimap.<K, V>of();
	}

	protected String getDefaultNamespace() {
		return SnomedIdentifiers.INT_NAMESPACE;
	}
	
	protected Branch getBranch() {
		return branch;
	}
}
