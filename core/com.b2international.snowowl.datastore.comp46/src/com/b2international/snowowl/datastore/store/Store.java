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
package com.b2international.snowowl.datastore.store;

import java.util.Collection;
import java.util.Map;

import com.b2international.index.Index;
import com.b2international.snowowl.datastore.store.query.Query;


/**
 * @since 4.1
 * @param <T>
 *            - the type of the objects to store
 * @deprecated - as of 5.0, {@link Store} API is deprecated, use the new {@link Index} API instead.
 */
public interface Store<T> {

	void put(String key, T value);
	
	void putAll(Map<String, T> map);
	
	T get(String key);
	
	T remove(String key);
	
	Collection<T> removeAll(Collection<String> keys);
	
	boolean replace(String key, T oldValue, T newValue);
	
	Collection<T> values();
	
	void clear();
	
	Collection<T> search(Query query);
	
	Collection<T> search(Query query, int offset, int limit);
	
	void configureSearchable(String property);

	boolean containsKey(String key);
}