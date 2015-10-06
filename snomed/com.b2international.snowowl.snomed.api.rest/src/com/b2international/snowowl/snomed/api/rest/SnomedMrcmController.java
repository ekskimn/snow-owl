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
package com.b2international.snowowl.snomed.api.rest;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.impl.SnomedMrcmService;
import com.b2international.snowowl.snomed.api.impl.domain.Predicate;
import com.b2international.snowowl.snomed.api.rest.domain.CollectionResource;
import com.b2international.snowowl.snomed.datastore.snor.PredicateIndexEntry;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @since 4.1
 */
@Api("MRCM")
@RestController
@RequestMapping(value="/mrcm", produces={AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
public class SnomedMrcmController extends AbstractRestService {

	@Autowired 
	private IEventBus bus;
	
	@Autowired 
	private SnomedMrcmService mrcmService;
	
	@ApiOperation(
		value = "Retrieve MRCM relationship rules for a concept.", 
		notes = "Retrieve mrcm relationship rules for a concept. There are other rule types but they are not yet returned here.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK", response=CollectionResource.class)
	})
	@RequestMapping(value="/{conceptId}", method=RequestMethod.GET)
	public List<Predicate> getPredicates(@PathVariable String conceptId) {
		return mrcmService.getPredicates(conceptId);
	}
	
}
