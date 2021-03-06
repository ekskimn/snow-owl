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

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;

import com.b2international.index.Index;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.api.index.IIndexUpdater;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDORepository;
import com.b2international.snowowl.datastore.server.cdo.ICDOConflictProcessor;

/**
 * @since 4.1
 */
public interface InternalRepository extends Repository {

	ICDOConnection getConnection();

	CDOBranchManager getCdoBranchManager();

	CDOBranch getCdoMainBranch();

	/**
	 * @deprecated As of 4.7 release, {@link IIndexUpdater} and nested index is not supported, use {@link #getIndex()} or {@link #getRevisionIndex()}
	 *             to store revisions or plain objects.
	 */
	IIndexUpdater<?> getIndexUpdater();

	Index getIndex();

	RevisionIndex getRevisionIndex();

	ICDORepository getCdoRepository();

	ICDOConflictProcessor getConflictProcessor();

	long getBaseTimestamp(CDOBranch branch);

	long getHeadTimestamp(CDOBranch branch);

}
