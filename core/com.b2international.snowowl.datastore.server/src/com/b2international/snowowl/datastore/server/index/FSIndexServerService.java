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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.api.BranchPath;
import com.b2international.snowowl.core.api.index.IIndexEntry;
import com.b2international.snowowl.core.api.index.IndexException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * File system backed index server service.
 */
public abstract class FSIndexServerService<E extends IIndexEntry> extends IndexServerService<E> {

	private final Supplier<IDirectoryManager> directorySupplier = Suppliers.memoize(new Supplier<IDirectoryManager>() {
		@Override public IDirectoryManager get() {
			return new FSDirectoryManager(getRepositoryUuid(), getAbsoluteIndexRoot());
		}
	});

	private final Path indexPath;	
	
	protected FSIndexServerService(final File indexPath, final long timeout) {
		super(timeout);
		this.indexPath = checkNotNull(indexPath, "indexPath").toPath();
	}
	
	private File getAbsoluteIndexRoot() {
		// ".../resources/indexes/snomed"
		return SnowOwlApplication.INSTANCE.getEnviroment().getDataDirectory().toPath()
				.resolve("indexes")
				.resolve(indexPath)
				.toFile();
	}
	
	@Override
	protected IDirectoryManager getDirectoryManager() {
		return directorySupplier.get();
	}
	
	@Override
	public long getBaseGeneration(BranchPath physicalPath) {
		try {
			return getDirectoryManager().openDirectory(physicalPath.parent(), false).getLastBaseIndexCommit(physicalPath).getGeneration();
		} catch (IOException e) {
			throw new IndexException("Could not retrieve base generation for physical path " + physicalPath + ".", e);
		}
	}
}
