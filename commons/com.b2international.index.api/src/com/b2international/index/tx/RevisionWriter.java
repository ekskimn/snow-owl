/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.tx;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Writer working on top of a {@link RevisionIndex}. A {@link RevisionWriter} is always working on a single {@link RevisionBranch}.
 * 
 * @since 4.7
 */
public interface RevisionWriter {

	void put(long storageKey, Revision object) throws IOException;

	void putAll(Map<Long, Revision> revisionsByStorageKey) throws IOException;

	<T extends Revision> void remove(Class<T> type, long storageKey) throws IOException;

	<T extends Revision> void removeAll(Map<Class<T>, Set<Long>> storageKeysByType) throws IOException;

	void commit(String commitMessage) throws IOException;

	String branch();

}