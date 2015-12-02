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
package com.b2international.snowowl.datastore.server.internal;

import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.setup.Environment;

/**
 * @since 4.5
 */
public final class RepositoryBuilder {
	
	private String repositoryId;
	private int numberOfWorkers = 3 * Runtime.getRuntime().availableProcessors();

	RepositoryBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public RepositoryBuilder setNumberOfWorkers(int numberOfWorkers) {
		this.numberOfWorkers = numberOfWorkers;
		return this;
	}
	
	public Repository build(Environment env) {
		return new CDOBasedRepository(repositoryId, numberOfWorkers, env);
	}

}
