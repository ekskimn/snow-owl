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
package com.b2international.snowowl.datastore.server;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.CDOEditingContext;

/**
 * @param <T>
 *            The type of CDOEditingContext that can be created.
 * 
 * @since 2.9
 */
public interface EditingContextFactory {

	/**
	 * Create an {@link CDOEditingContext} on the given {@link IBranchPath}
	 * 
	 * @param branchPath
	 * @return
	 */
	CDOEditingContext createEditingContext(IBranchPath branchPath);

	/**
	 * Returns whether the {@link CDOEditingContext} returned by this factory belongs to the repository identified by the given repositoryId or not.
	 * 
	 * @param repositoryId
	 * @return
	 */
	boolean belongsTo(String repositoryId);

}