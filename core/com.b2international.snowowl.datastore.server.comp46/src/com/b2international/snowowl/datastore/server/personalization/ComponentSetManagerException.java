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
package com.b2international.snowowl.datastore.server.personalization;

/**
 */
public class ComponentSetManagerException extends RuntimeException {

	private static final long serialVersionUID = 7379371647778733470L;
	
	public ComponentSetManagerException() {
		super();
	}

	public ComponentSetManagerException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ComponentSetManagerException(final String message) {
		super(message);
	}

	public ComponentSetManagerException(final Throwable cause) {
		super(cause);
	}
}