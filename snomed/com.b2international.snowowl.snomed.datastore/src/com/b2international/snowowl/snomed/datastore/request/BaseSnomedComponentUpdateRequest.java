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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.NullBranchPath;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.CodeSystemVersions;
import com.b2international.snowowl.datastore.ICodeSystemVersion;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.Component;
import com.b2international.snowowl.snomed.Concept;
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
	
	protected IBranchPath getLatestReleaseBranch(TransactionContext context) {
		
		IBranchPath latestReleaseParentPath = context.branch().branchPath();
		
		while (!NullBranchPath.INSTANCE.equals(latestReleaseParentPath)) {

			final CodeSystemVersions versions = new CodeSystemRequests(context.id())
					.prepareSearchCodeSystemVersion()
					.setParentPath(latestReleaseParentPath)
					.build(IBranchPath.MAIN_BRANCH)
					.execute(context.service(IEventBus.class))
					.getSync();

			ICodeSystemVersion latestReleaseVersion = null;
			
			for (ICodeSystemVersion version : versions) {
				if (latestReleaseVersion == null || version.getEffectiveDate() > latestReleaseVersion.getEffectiveDate()) {
					latestReleaseVersion = version;
				}
			}
			
			if (latestReleaseVersion != null) {
				return BranchPathUtils.createPath(latestReleaseVersion.getPath());
			}
			
			latestReleaseParentPath = latestReleaseParentPath.getParent();
		}
		
		LOG.warn("Couldn't find latest release branch for path {}, using MAIN as the reference point.", context.branch().path());
		return BranchPathUtils.createMainPath();
	}
		
	protected boolean updateModule(final TransactionContext context, final Component component) {
		if (null == moduleId) {
			return false;
		}

		final String currentModuleId = component.getModule().getId();
		if (!currentModuleId.equals(moduleId)) {
			component.setModule(context.lookup(moduleId, Concept.class));
			return true;
		} else {
			return false;
		}
	}

	protected boolean updateStatus(final TransactionContext context, final Component component) {
		if (null == active) {
			return false;
		}

		if (component.isActive() != active) {
			component.setActive(active);
			return true;
		} else {
			return false;
		}
	}
}
