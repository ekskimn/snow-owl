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
package com.b2international.snowowl.datastore.quicksearch;

import java.util.Map;

import com.b2international.snowowl.core.quicksearch.QuickSearchContentResult;
import com.b2international.snowowl.datastore.IBranchPathMap;

/**
 * A server-side service interface collecting quick search results for the specified query expression and configuration.
 */
public interface IQuickSearchContentProvider {

	/**
	 * Retrieves quick search results for the specified parameters.
	 * <p>
	 * Note that the returned hit count value may be greater than the size of the corresponding result list.
	 * 
	 * @param queryExpression the query expression for the search process
	 * @param branchPathMap a map of branch paths (keyed by repository identifiers) where the operation should be performed (may not be {@code null})
	 * @param limit the maximum number of the returned objects (may not be negative)
	 * @param configuration a string-keyed map of search options
	 * @return terminology content matching the query expression, and the total number of matching components
	 */
	QuickSearchContentResult getComponents(String queryExpression, IBranchPathMap branchPathMap, int limit, Map<String, Object> configuration);
}
