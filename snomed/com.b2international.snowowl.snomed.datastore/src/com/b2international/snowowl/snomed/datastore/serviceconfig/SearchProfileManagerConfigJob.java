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

import com.b2international.snowowl.datastore.DatastoreActivator;
import com.b2international.snowowl.datastore.serviceconfig.AbstractClientServiceConfigJob;
import com.b2international.snowowl.snomed.datastore.index.interest.ISearchProfileManager;

/**
 */
public class SearchProfileManagerConfigJob extends AbstractClientServiceConfigJob<ISearchProfileManager> {

	private static final String JOB_NAME = "Search profile service configuration...";

	public SearchProfileManagerConfigJob() {
		super(JOB_NAME, DatastoreActivator.PLUGIN_ID);
	}

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.serviceconfig.AbstractClientServiceConfigJob#getServiceClass()
	 */
	@Override
	protected Class<ISearchProfileManager> getServiceClass() {
		return ISearchProfileManager.class;
	}
}