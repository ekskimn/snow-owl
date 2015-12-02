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

import java.util.Collection;

import com.b2international.commons.platform.Extensions;
import com.b2international.snowowl.core.ClassLoaderProvider;
import com.b2international.snowowl.datastore.server.RepositoryClassLoaderProvider;
import com.b2international.snowowl.datastore.server.RepositoryClassLoaderProviderRegistry;

/**
 * @since 4.5
 */
public class ExtensionBasedRepositoryClassLoaderProviderRegistry implements RepositoryClassLoaderProviderRegistry {

	private final Collection<RepositoryClassLoaderProvider> extensions;

	public ExtensionBasedRepositoryClassLoaderProviderRegistry() {
		this.extensions = Extensions.getExtensions("com.b2international.snowowl.datastore.server.classLoaderProvider", RepositoryClassLoaderProvider.class);
	}
	
	@Override
	public ClassLoaderProvider get(final String repositoryId) {
		for (RepositoryClassLoaderProvider ext : extensions) {
			if (ext.belongsTo(repositoryId)) {
				return ext;
			}
		}
		throw new UnsupportedOperationException("No repository based class loader provider has been registered for repository: " + repositoryId); 
	}

}
