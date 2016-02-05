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
package com.b2international.snowowl.snomed.datastore.serviceconfig;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.datastore.serviceconfig.ClientServiceConfigJob;
import com.b2international.snowowl.snomed.datastore.SnomedConceptLabelProviderClientService;
import com.b2international.snowowl.snomed.datastore.SnomedConceptLabelProviderClientServiceImpl;
import com.b2international.snowowl.snomed.datastore.SnomedConceptLabelProviderService;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;

/**
 * Job for registering the {@link SnomedConceptLabelProviderClientService} into the {@link ApplicationContext application context}.
 * 
 */
public class SnomedConceptLabelProviderClientServiceConfigJob extends ClientServiceConfigJob<SnomedConceptLabelProviderService, SnomedConceptLabelProviderClientService> {

	private static final String JOB_NAME = "SNOMED CT concept label provider client service configuration...";
	
	public SnomedConceptLabelProviderClientServiceConfigJob() {
		super(JOB_NAME, SnomedDatastoreActivator.PLUGIN_ID);
	}
	
	@Override
	protected Class<SnomedConceptLabelProviderService> getServiceClass() {
		return SnomedConceptLabelProviderService.class;
	}

	protected Class<SnomedConceptLabelProviderClientService> getTrackingClass() {
		return SnomedConceptLabelProviderClientService.class;
	}

	@Override
	protected SnomedConceptLabelProviderClientService createTrackingService(final SnomedConceptLabelProviderService branchAwareService) {
		return new SnomedConceptLabelProviderClientServiceImpl(branchAwareService);
	}
}