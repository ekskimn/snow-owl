package com.b2international.snowowl.snomed.api.impl.domain;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserComponentWithId;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentCreateRequest;
import com.b2international.snowowl.snomed.datastore.server.request.BaseSnomedComponentUpdateRequest;

public interface ComponentInputCreator<I extends BaseSnomedComponentCreateRequest, U extends BaseSnomedComponentUpdateRequest, T extends ISnomedBrowserComponentWithId> {

	I createInput(T newComponent, InputFactory inputFactory);

	U createUpdate(T existingComponent, T updatedComponent);
	
	boolean canCreateInput(Class<? extends BaseSnomedComponentCreateRequest> inputType);

	boolean canCreateUpdate(Class<? extends BaseSnomedComponentUpdateRequest> updateType);
}
