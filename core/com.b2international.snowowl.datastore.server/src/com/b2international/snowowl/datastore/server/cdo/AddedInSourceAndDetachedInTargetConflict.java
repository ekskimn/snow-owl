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
package com.b2international.snowowl.datastore.server.cdo;

import java.text.MessageFormat;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.spi.cdo.DefaultCDOMerger.Conflict;

/**
 * Reported when a component is added on the source branch, but it references a component (either directly or
 * indirectly) which was detached on the target branch.
 */
public class AddedInSourceAndDetachedInTargetConflict extends Conflict {

	private final CDOID sourceId;
	private final CDOID targetId;

	public AddedInSourceAndDetachedInTargetConflict(final CDOID sourceId, final CDOID targetId) {
		this.sourceId = sourceId;
		this.targetId = targetId;
	}

	@Override
	public CDOID getID() {
		return sourceId;
	}

	public CDOID getSourceId() {
		return sourceId;
	}

	public CDOID getTargetId() {
		return targetId;
	}

	@Override
	public String toString()
	{
		return MessageFormat.format("AddedInSourceAndDetachedInTarget[source={0}, target={1}]", sourceId, targetId);
	}
}
