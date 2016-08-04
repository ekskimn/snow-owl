package com.b2international.snowowl.snomed.api.impl.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserComponentWithId;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class InputFactory {

	private final List<ComponentInputCreator<?, ?, ?>> creators;

	public InputFactory() {
		creators = ImmutableList.<ComponentInputCreator<?, ?, ?>>of(
				new ConceptInputCreator(), 
				new DescriptionInputCreator(), 
				new RelationshipInputCreator());
	}

	public <I extends BaseSnomedComponentCreateRequest> I createComponentInput(final ISnomedBrowserComponentWithId newComponent, final Class<I> inputType) {
		return getInputDelegate(inputType).createInput(newComponent, this);
	}

	public <I extends BaseSnomedComponentCreateRequest> List<I> createComponentInputs(final List<? extends ISnomedBrowserComponentWithId> newComponents, final Class<I> inputType) {
		final List<I> inputs = Lists.newArrayList();
		
		for (final ISnomedBrowserComponentWithId component : newComponents) {
			if (component.getId() == null) {
				inputs.add(createComponentInput(component, inputType));
			}
		}
		
		return inputs;
	}

	public <U extends BaseSnomedComponentUpdateRequest> U createComponentUpdate(
			final ISnomedBrowserConcept existingConcept, 
			final ISnomedBrowserConcept newConcept, 
			final Class<U> updateType) {

		return getUpdateDelegate(updateType).createUpdate(existingConcept, newConcept);
	}

	public <U extends BaseSnomedComponentUpdateRequest> Map<String, U> createComponentUpdates(
			final List<? extends ISnomedBrowserComponentWithId> existingComponents,
			final List<? extends ISnomedBrowserComponentWithId> newComponents, 
			final Class<U> updateType) {

		final Map<String, U> updateMap = Maps.newHashMap();
		
		for (final ISnomedBrowserComponentWithId existingComponent : existingComponents) {
			for (final ISnomedBrowserComponentWithId newComponent : newComponents) {
				if (existingComponent.getId().equals(newComponent.getId())) {
					final U update = getUpdateDelegate(updateType).createUpdate(existingComponent, newComponent);
					if (update != null) {
						updateMap.put(existingComponent.getId(), update);
					}
				}
			}
		}
		
		return updateMap;
	}

	public Set<String> getDeletedComponentIds(final List<? extends ISnomedBrowserComponentWithId> existingComponents, final List<? extends ISnomedBrowserComponentWithId> newComponents) {
		// First assume that all existing components are deleted
		final Set<String> deletedComponentIds = toIdSet(existingComponents);
		final Set<String> newComponentIds = toIdSet(newComponents);
		
		// The ones which are still in newComponentIds weren't deleted, some remove them from the deleted set
		deletedComponentIds.removeAll(newComponentIds);
		return deletedComponentIds;
	}

	private Set<String> toIdSet(final List<? extends ISnomedBrowserComponentWithId> components) {
		final Set<String> ids = Sets.newHashSet();
		
		for (final ISnomedBrowserComponentWithId component : components) {
			ids.add(component.getId());
		}
		
		return ids;
	}

	@SuppressWarnings("unchecked")
	private <I extends BaseSnomedComponentCreateRequest> ComponentInputCreator<I, ?, ISnomedBrowserComponentWithId> getInputDelegate(final Class<I> inputType) {
		for (final ComponentInputCreator<?, ?, ?> creator : creators) {
			if (creator.canCreateInput(inputType)) {
				return (ComponentInputCreator<I, ?, ISnomedBrowserComponentWithId>) creator;
			}
		}
		
		throw new IllegalStateException("No ComponentInputCreator found for input type " + inputType);
	}

	@SuppressWarnings("unchecked")
	private <U extends BaseSnomedComponentUpdateRequest> ComponentInputCreator<?, U, ISnomedBrowserComponentWithId> getUpdateDelegate(final Class<U> updateType) {
		for (final ComponentInputCreator<?, ?, ?> creator : creators) {
			if (creator.canCreateUpdate(updateType)) {
				return (ComponentInputCreator<?, U, ISnomedBrowserComponentWithId>) creator;
			}
		}
		
		throw new IllegalStateException("No ComponentInputCreator found for update type " + updateType);
	}
}
