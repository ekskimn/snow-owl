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
package com.b2international.snowowl.datastore.server.index;

import java.io.IOException;
import java.util.List;

import com.b2international.snowowl.core.api.BranchPath;
import com.b2international.snowowl.core.branch.Branch;

/**
 * Represents an {@link IndexDirectory} initializer.
 */
public interface IDirectoryManager {

	/**
	 * Initializes the the specified {@link IndexBranchService} if it represents the MAIN branch and its directory does 
	 * not exist on the file system.
	 * 
	 * @param service the service which is initialized first time
	 */
	void firstStartup(IndexBranchService service);

	/** 
	 * Creates a new {@link IndexDirectory} instance for the specified sequence of CDO branch identifiers.
	 *  
	 * @param cdoBranchPath the sequence of CDO branch IDs to follow
	 * @param readOnly {@code true} if an implementation should be returned which is not writable, {@code false} otherwise
	 * @return the Directory for the given CDO branch sequence
	 */
	IndexDirectory openDirectory(BranchPath cdoBranchPath, boolean readOnly) throws IOException;

	/** 
	 * Creates a new writable {@link IndexDirectory} instance for the specified branch.
	 *  
	 * @param the branch to create an index directory for (uses CDO and index-specific metadata on the branch)
	 * @return the index directory for the given branch
	 */
	IndexDirectory openDirectory(Branch branch) throws IOException;

	/**
	 * Collects a list of absolute file paths that contain index data for the specified branch path. Note that the list
	 * may not be complete to reproduce an index by itself in case of "layered" indexes.
	 * @return the list of files which carry data for this update, or an empty list (never {@code null})
	 */
	List<String> listFiles(BranchPath cdoBranchPath) throws IOException;
}
