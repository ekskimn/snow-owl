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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.commons.collections.Procedure;
import com.b2international.snowowl.core.exceptions.ApiValidation;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ConflictException;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.datastore.review.MergeReview;
import com.b2international.snowowl.datastore.review.ReviewStatus;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.ISnomedMergeReviewService;
import com.b2international.snowowl.snomed.api.domain.mergereview.ISnomedBrowserMergeReviewDetail;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConceptUpdate;
import com.b2international.snowowl.snomed.api.rest.domain.CreateReviewRequest;
import com.b2international.snowowl.snomed.api.rest.domain.RestApiError;
import com.b2international.snowowl.snomed.api.rest.util.DeferredResults;
import com.b2international.snowowl.snomed.api.rest.util.Responses;
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api("Merge Reviews")
@RestController
@RequestMapping(value="/merge-reviews", produces={AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
public class SnomedBranchMergeReviewController extends AbstractSnomedRestService {

	@Autowired
	private IEventBus bus;
	
	@Autowired
	@Value("${codeSystemShortName}")
	protected String codeSystemShortName;
	
	@Autowired
	protected ISnomedMergeReviewService mergeReviewService;

	@ApiOperation(
			value = "Create new merge review", 
			notes = "Creates a new terminology merge review for the SNOMED CT repository.")
	@ApiResponses({
		@ApiResponse(code = 201, message = "Created"),
		@ApiResponse(code = 400, message = "Bad Request", response=RestApiError.class)
	})
	@RequestMapping(method=RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public DeferredResult<ResponseEntity<Void>> createMergeReview(@RequestBody final CreateReviewRequest request) {
		ApiValidation.checkInput(request);
		final DeferredResult<ResponseEntity<Void>> result = new DeferredResult<>();
		final ControllerLinkBuilder linkTo = linkTo(getClass());
		SnomedRequests
			.mergeReview()
			.prepareCreate()
			.setSource(request.getSource())
			.setTarget(request.getTarget())
			.build()
			.execute(bus)
			.then(new Procedure<MergeReview>() { @Override protected void doApply(final MergeReview mergeReview) {
				result.setResult(Responses.created(getLocationHeader(linkTo, mergeReview)).build());
			}})
			.fail(new Procedure<Throwable>() { @Override protected void doApply(final Throwable t) {
				result.setErrorResult(t);
			}});
		return result;
	}
	
	@ApiOperation(
			value = "Retrieve single merge review", 
			notes = "Retrieves an existing terminology merge review with the specified identifier, if it exists.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Merge Review not found", response=RestApiError.class),
	})
	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public DeferredResult<MergeReview> getMergeReview(@PathVariable("id") final String mergeReviewId) {
		return DeferredResults.wrap(SnomedRequests
				.mergeReview()
				.prepareGet(mergeReviewId)
				.execute(bus));
	}

	@ApiOperation(
			value = "Retrieve the details of a merge review", 
			notes = "Retrieves the set of modified concepts for a merge review with the specified identifier, if it exists.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Merge Review not found or changes are not yet available", response=RestApiError.class),
	})
	@RequestMapping(value="/{id}/details", method=RequestMethod.GET)
	public Set<ISnomedBrowserMergeReviewDetail> getMergeReviewDetails(
			
			@PathVariable("id") final String mergeReviewId,
			
			@ApiParam(value="Language codes and reference sets, in order of preference")
			@RequestHeader(value="Accept-Language", defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
			final String languageSetting) throws InterruptedException, ExecutionException {
		
		return mergeReviewService.getMergeReviewDetails(mergeReviewId, getExtendedLocales(languageSetting));
	}
	
	@ApiOperation(
			value = "Stores a concept against a merge review.", 
			notes = "Stores a concept against a merge review for later replay after the merge has completed.  Checks the merge review is still current.")
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Merge Review not found or changes are not yet available", response=RestApiError.class),
	})
	@RequestMapping(value="/{id}/{conceptId}", method=RequestMethod.POST)
	public void storeMergeReviewConcept(@PathVariable("id") final String mergeReviewId, 
			@PathVariable("conceptId") final String conceptId,
			@RequestBody final SnomedBrowserConceptUpdate concept) throws Exception {

		if (!conceptId.equals(concept.getConceptId())) {
			throw new BadRequestException("The concept ID in the request body does not match the ID in the URL.");
		}
		
		MergeReview mergeReview = SnomedRequests
				.mergeReview()
				.prepareGet(mergeReviewId)
				.executeSync(bus);
		
		//We need to pass that information back down the BrowserService as it can't be called directly.
		final ReviewStatus status = mergeReview.status();
		if (!status.equals(ReviewStatus.CURRENT)) {
			throw new ConflictException ("Unable to save concept against merge review at status " + status);
		}
		
		mergeReviewService.persistManualConceptMerge(mergeReview, concept);
	}
	
	@ApiOperation(
			value = "Merge branches and apply stored changes", 
			notes = "Merge source branch into a target branch in SNOMED-CT repository and then apply concept changes stored against the specified merge review.")
	@ApiResponses({
		@ApiResponse(code = 201, message = "No Content"),
		@ApiResponse(code = 404, message = "Source or Target branch is not found", response=RestApiError.class),
		@ApiResponse(code = 409, message = "Merge conflict", response=RestApiError.class)
	})
	@RequestMapping(value="/{id}/apply", method = RequestMethod.POST)
	public ResponseEntity<Void> mergeAndApply(@PathVariable("id") final String mergeReviewId, 
			
			final Principal principal,

			@ApiParam(value="Language codes and reference sets, in order of preference")
			@RequestHeader(value="Accept-Language", defaultValue="en-US;q=0.8,en-GB;q=0.6", required=false) 
			final String languageSetting) throws IOException {

		final String userId = principal.getName();
		final Merge merge = mergeReviewService.mergeAndReplayConceptUpdates(mergeReviewId, userId, getExtendedLocales(languageSetting));
		final URI linkUri = linkTo(SnomedBranchMergingController.class).slash(merge.getId()).toUri();
		return Responses.accepted(linkUri).build();
	}
	
	private URI getLocationHeader(ControllerLinkBuilder linkBuilder, final MergeReview mergeReview) {
		return linkBuilder.slash(mergeReview.id()).toUri();
	}
}
