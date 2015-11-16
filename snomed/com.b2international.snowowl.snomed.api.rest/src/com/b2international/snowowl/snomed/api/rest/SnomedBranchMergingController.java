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

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.commons.collections.Procedure;
import com.b2international.snowowl.core.exceptions.ApiValidation;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.core.exceptions.ConflictException;
import com.b2international.snowowl.core.exceptions.InvalidStateException;
import com.b2international.snowowl.datastore.branch.Branch;
import com.b2international.snowowl.datastore.branch.Branch.BranchState;
import com.b2international.snowowl.datastore.server.events.BranchReply;
import com.b2international.snowowl.datastore.server.events.MergeEvent;
import com.b2international.snowowl.datastore.server.review.MergeReview;
import com.b2international.snowowl.datastore.server.review.ReviewStatus;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdateResult;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConceptUpdateResult;
import com.b2international.snowowl.snomed.api.rest.domain.MergeRequest;
import com.b2international.snowowl.snomed.api.rest.domain.RestApiError;
import com.b2international.snowowl.snomed.api.rest.util.Responses;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @since 4.1
 */
@Api("Branches")
@RestController
@RequestMapping(value="/merges", produces={AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE})
public class SnomedBranchMergingController extends AbstractRestService {

	@Autowired
	private IEventBus bus;
	
	@Autowired
	private SnomedBranchMergeReviewController mergeReviewController;
	
	@Autowired
	protected ISnomedBrowserService browserService;
	
	@ApiOperation(
			value = "Merge branches", 
			notes = "Merge source branch into a target branch in SNOMED-CT repository.")
		@ApiResponses({
			@ApiResponse(code = 200, message = "OK"),
			@ApiResponse(code = 400, message = "Bad request", response=RestApiError.class),
			@ApiResponse(code = 404, message = "Source or Target branch is not found", response=RestApiError.class),
			@ApiResponse(code = 409, message = "Merge conflict", response=RestApiError.class)
		})
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public DeferredResult<Branch> merge(@RequestBody MergeRequest request) throws Exception {
		ApiValidation.checkInput(request);
		MergeEvent mergeEvent = request.toEvent(repositoryId);
		return doMerge(mergeEvent);
	}
	
	@ApiOperation(
			value = "Merge branches and apply stored changes", 
			notes = "Merge source branch into a target branch in SNOMED-CT repository and then apply concept changes stored against the specified merge review.")
		@ApiResponses({
			@ApiResponse(code = 204, message = "No Content"),
			@ApiResponse(code = 400, message = "Bad request", response=RestApiError.class),
			@ApiResponse(code = 404, message = "Source or Target branch is not found", response=RestApiError.class),
			@ApiResponse(code = 409, message = "Merge conflict", response=RestApiError.class)
		})
	@RequestMapping(value="/apply", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public List<ISnomedBrowserConceptUpdateResult> mergeAndApply(@RequestBody MergeRequest mergeRequest, 
			final Principal principal,
			final HttpServletRequest request) throws Exception {
		ApiValidation.checkInput(mergeRequest);
		MergeEvent mergeEvent = mergeRequest.toEvent(repositoryId);
		MergeReview mergeReview = validateMergeReview(mergeEvent);
		
		DeferredResult<Branch> mergeResult = doMerge(mergeEvent);
		
		//Check branch is ready to have changes applied to it.
		Branch branch = (Branch)getResult(mergeResult);
		BranchState state = branch.state();
		if (state.equals(BranchState.BEHIND) || state.equals(BranchState.STALE)) {
			throw new InvalidStateException("Unacceptable branch state %s after merge. Cannot apply stored changes.", state);
		}
		final String userId = principal.getName();
		return browserService.replayConceptUpdates(mergeReview.id(), mergeReview.getTargetPath(), userId, Collections.list(request.getLocales()));
	}
	
	private DeferredResult<Branch> doMerge(MergeEvent mergeEvent) throws Exception {
		final DeferredResult<Branch> result = new DeferredResult<>();
		mergeEvent.send(bus, BranchReply.class)
			.then(new Procedure<BranchReply>() { @Override protected void doApply(BranchReply reply) {
				result.setResult(reply.getBranch());
			}})
			.fail(new Procedure<Throwable>() { @Override protected void doApply(Throwable throwable) {
				result.setErrorResult(throwable);
			}});
		return result;
	}

	private MergeReview validateMergeReview(MergeEvent mergeEvent) throws Exception {
		MergeReview mergeReview = null;
		String mergeReviewId = mergeEvent.getMergeReviewId();
		if (mergeReviewId != null && !mergeReviewId.isEmpty()) {
			DeferredResult<MergeReview> deferredMergeReview = mergeReviewController.getMergeReview(mergeReviewId);
			mergeReview = (MergeReview) getResult(deferredMergeReview);
			if (!mergeReview.getStatus().equals(ReviewStatus.CURRENT)) {
				throw new ConflictException ("Unable to merge with merge review at status " + mergeReview.getStatus());
			}
			//Now make sure the merge we're being asked to do matches the source and target of the merge review specified
			if (!mergeEvent.getSource().equals(mergeReview.getSourcePath())) {
				throw new BadRequestException ("Merge source (%s) does not match merge review source (%s)", mergeEvent.getSource(), mergeReview.getSourcePath());
			}
			
			if (!mergeEvent.getTarget().equals(mergeReview.getTargetPath())) {
				throw new BadRequestException ("Merge target (%s) does not match merge review target (%s)", mergeEvent.getTarget(), mergeReview.getTargetPath());
			}
		} else {
			throw new BadRequestException ("Merge Review Id required");
		}
		return mergeReview;
	}
	
}
