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

import org.eclipse.xtext.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.datastore.CodeSystemVersions;
import com.b2international.snowowl.datastore.ICodeSystemVersion;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.BranchMetadataResolver;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.terminologyregistry.core.request.CodeSystemRequests;

/** 
 * @since 4.5
 * @param <B>
 */
public abstract class BaseSnomedComponentUpdateRequest extends BaseRequest<TransactionContext, Void> {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseSnomedComponentUpdateRequest.class);

	private final String componentId;
	
	private String moduleId;
	private Boolean active;
	
	protected BaseSnomedComponentUpdateRequest(String componentId) {
		this.componentId = componentId;
	}
	
	void setActive(Boolean active) {
		this.active = active;
	}
	
	void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}
	
	/**
	 * @deprecated - visibility will be reduced to protected in 4.6
	 * @return
	 */
	public Boolean isActive() {
		return active;
	}
	
	protected String getModuleId() {
		return moduleId;
	}
	
	protected String getComponentId() {
		return componentId;
	}
	
	@Override
	protected final Class<Void> getReturnType() {
		return Void.class;
	}
	
	public static String getLatestReleaseBranch(TransactionContext context) {
		String codeSystemShortName = BranchMetadataResolver.getEffectiveBranchMetadataValue(context.branch(), "codeSystemShortName");
		codeSystemShortName = Strings.isEmpty(codeSystemShortName) ? SnomedTerminologyComponentConstants.SNOMED_INT_SHORT_NAME : codeSystemShortName;

		final CodeSystemVersions versions = CodeSystemRequests.prepareSearchCodeSystemVersion()
				.all()
				.filterByCodeSystemShortName(codeSystemShortName)
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, Branch.MAIN_PATH)
				.execute(context.service(IEventBus.class))
				.getSync();

		ICodeSystemVersion latestReleaseVersion = null;
		for (ICodeSystemVersion version : versions) {
			if (latestReleaseVersion == null || version.getEffectiveDate() > latestReleaseVersion.getEffectiveDate()) {
				latestReleaseVersion = version;
			}
		}

		if (latestReleaseVersion != null) {
			return latestReleaseVersion.getPath();
		} else {
			LOG.warn("No releases found for codesystem {}.", codeSystemShortName);
			return Branch.MAIN_PATH;
		}
	}

	protected boolean updateModule(final TransactionContext context, final Component component, final String newModuleId) {
		if (null == newModuleId) {
			return false;
		}

		final String currentModuleId = component.getModule().getId();
		if (!currentModuleId.equals(newModuleId)) {
			component.setModule(context.lookup(newModuleId, Concept.class));
			return true;
		} else {
			return false;
		}
	}

	protected boolean updateStatus(final TransactionContext context, final Component component, final Boolean newActive) {
		if (null == newActive) {
			return false;
		}

		if (component.isActive() != newActive) {
			component.setActive(newActive);
			return true;
		} else {
			return false;
		}
	}
}
