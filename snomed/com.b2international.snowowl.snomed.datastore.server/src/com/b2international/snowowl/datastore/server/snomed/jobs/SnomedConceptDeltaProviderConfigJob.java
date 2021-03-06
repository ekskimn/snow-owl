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
package com.b2international.snowowl.datastore.server.snomed.jobs;

import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.datastore.server.snomed.SnomedDatastoreServerActivator;
import com.b2international.snowowl.datastore.server.snomed.delta.SnomedConceptDeltaProvider;
import com.b2international.snowowl.datastore.serviceconfig.AbstractServerServiceConfigJob;
import com.b2international.snowowl.datastore.serviceconfig.ServiceConfigJob;
import com.b2international.snowowl.snomed.datastore.delta.ISnomedConceptDeltaProvider;

/**
 * Job for registering the SNOMED CT component delta provider in the application context.
 *  
 * @see ServiceConfigJob
 * @see ISnomedConceptDeltaProvider
 */
public class SnomedConceptDeltaProviderConfigJob extends AbstractServerServiceConfigJob<ISnomedConceptDeltaProvider> {

	public SnomedConceptDeltaProviderConfigJob() {
		super("SNOMED CT component delta provider configuration...", SnomedDatastoreServerActivator.PLUGIN_ID);
	}

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.AbstractServerServiceConfigJob#getServiceClass()
	 */
	@Override
	protected Class<ISnomedConceptDeltaProvider> getServiceClass() {
		return ISnomedConceptDeltaProvider.class;
	}

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.AbstractServerServiceConfigJob#createServiceImplementation()
	 */
	@Override
	protected SnomedConceptDeltaProvider createServiceImplementation() throws SnowowlServiceException {
		return new SnomedConceptDeltaProvider();
	}
}