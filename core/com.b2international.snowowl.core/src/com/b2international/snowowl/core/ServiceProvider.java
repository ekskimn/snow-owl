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
package com.b2international.snowowl.core;

import com.google.inject.Provider;

/**
 * @since 4.5
 */
public interface ServiceProvider {

	/**
	 * Returns the given service or throws an exception if none found in the current {@link ApplicationContext}.
	 * 
	 * @param type
	 * @return the currently registered service implementation for the given service interface, never <code>null</code>
	 */
	<T> T service(Class<T> type);

	/**
	 * Returns a {@link Provider} to provide the given type when needed by using {@link #service(Class)}, so the returned {@link Provider} will never
	 * return <code>null</code> instances, instead it throws exception, which may indicate application bootstrapping/initialization problems.
	 * 
	 * @param type
	 * @return
	 */
	<T> Provider<T> provider(Class<T> type);

}
