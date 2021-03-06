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
package com.b2international.snowowl.core.events;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 4.5
 */
public abstract class BaseRepositoryEvent extends BaseEvent {

	private final String repositoryId;

	protected BaseRepositoryEvent(final String repositoryId) {
		this.repositoryId = checkNotNull(repositoryId, "repositoryId");
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	@Override
	protected final String getAddress() {
		return String.format("/%s%s", repositoryId, getPath());
	}

	/**
	 * Returns the path where this event should be sent within the repository.
	 *
	 * @return
	 */
	protected abstract String getPath();

}
