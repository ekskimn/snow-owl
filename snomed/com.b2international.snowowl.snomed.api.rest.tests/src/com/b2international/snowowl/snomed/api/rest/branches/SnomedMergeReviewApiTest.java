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
package com.b2international.snowowl.snomed.api.rest.branches;

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.assertBranchCanBeRebased;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenConceptRequestBody;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenRelationshipRequestBody;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.datastore.review.ReviewStatus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

/**
 * @since 4.6
 */
public class SnomedMergeReviewApiTest extends AbstractSnomedApiTest {

	private static final Set<String> FINISH_STATES = ImmutableSet.of(
			ReviewStatus.CURRENT.toString(), 
			ReviewStatus.FAILED.toString(), 
			ReviewStatus.STALE.toString());

	private static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(1L);
	private static final long POLL_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);
	
	private static final String FINDING_CONTEXT = "408729009";
	
	private Response whenCreatingMergeReview(String source, String target) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
		.with()
			.contentType(ContentType.JSON)
			.body(ImmutableMap.builder().put("source", source).put("target", target).build())
		.when()
			.post("/merge-reviews");
	}

	private String andCreatedMergeReview(String source, String target) {
		final String location = whenCreatingMergeReview(source, target)
		.then()
			.statusCode(201)
			.header("Location", notNullValue())
			.extract().header("Location");

		return lastPathSegment(location);
	}
	
	private Response whenRetrievingMergeReview(String reviewId) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
		.when()
			.get("/merge-reviews/{id}", reviewId);
	}
	
	private Response whenRetrievingMergeReviewDetails(String reviewId) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().header("Accept-Language", "en-GB")
				.when()
				.get("/merge-reviews/{id}/details", reviewId);
	}
	
	@Test
	public void createMergeReviewEmptyFields() {
		whenCreatingMergeReview("", "")
		.then()
			.statusCode(400)
			.body("message", equalTo("2 validation errors"))
			.body("violations", hasItem("'source' may not be empty (was '')"))
			.body("violations", hasItem("'target' may not be empty (was '')"));
	}
	
	@Test
	public void createMergeReviewNonExistentBranch() {
		whenCreatingMergeReview(testBranchPath.getPath(), Branch.MAIN_PATH)
		.then()
			.statusCode(400);
	}
	
	@Test
	public void createReview() {
		givenBranchWithPath(testBranchPath);
		final String reviewId = andCreatedMergeReview("MAIN", testBranchPath.getPath());
		whenRetrievingMergeReview(reviewId)
		.then()
			.statusCode(200)
			.body("status", CoreMatchers.anyOf(equalTo(ReviewStatus.CURRENT.toString()), equalTo(ReviewStatus.PENDING.toString())));
		
		assertReviewCurrent(reviewId);
	}

	private void assertReviewCurrent(final String reviewId) {
		final long endTime = System.currentTimeMillis() + POLL_TIMEOUT;
		long currentTime;
		String currentStatus;

		do {

			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (final InterruptedException e) {
				fail(e.toString());
			}

			currentStatus = whenRetrievingMergeReview(reviewId)
			.then()
				.statusCode(200)
				.extract().body().path("status");

			currentTime = System.currentTimeMillis();

		} while (!FINISH_STATES.contains(currentStatus) && currentTime < endTime);

		assertEquals("End state should be CURRENT.", currentStatus, ReviewStatus.CURRENT.toString());
	}
	
	@Test
	public void createEmptyMergeReview() {
		givenBranchWithPath(testBranchPath);
		final IBranchPath setupBranch = createNestedBranch(testBranchPath, "a");
		
		// Create new inferred relationship on "Finding context"
		Map<?, ?> relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, 
				MODULE_SCT_CORE, 
				CharacteristicType.INFERRED_RELATIONSHIP,
				"New inferred relationship on child");
		
		assertComponentCreated(setupBranch, SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// Another inferred relationship goes on the parent branch
		relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, 
				MODULE_SCT_CORE, 
				CharacteristicType.INFERRED_RELATIONSHIP,
				"New inferred relationship on parent");
		
		assertComponentCreated(setupBranch.getParent(), SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// See what happened on the sibling branch before merging changes to its parent
		final String reviewId = andCreatedMergeReview(setupBranch.getPath(), setupBranch.getParentPath());
		assertReviewCurrent(reviewId);
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then()
			.statusCode(200)
			.and()
			.extract()
			.body()
			.as(JsonNode.class);
		
		assertTrue(reviewDetails.isArray());
		assertEquals(0, reviewDetails.size());
	}
	
	@Test
	public void createConflictingStatedMergeReview() {
		givenBranchWithPath(testBranchPath);
		final IBranchPath setupBranch = createNestedBranch(testBranchPath, "a");
		
		// Create new stated relationship on "Finding context"
		Map<?, ?> relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, 
				MODULE_SCT_CORE,
				"New stated relationship on child");
		
		assertComponentCreated(setupBranch, SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// Another stated relationship goes on the parent branch
		relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, 
				MODULE_SCT_CORE,
				"New stated relationship on parent");
		
		assertComponentCreated(setupBranch.getParent(), SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// See what happened on the sibling branch before merging changes to its parent
		final String reviewId = andCreatedMergeReview(setupBranch.getPath(), setupBranch.getParentPath());
		assertReviewCurrent(reviewId);
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then()
			.statusCode(200)
			.and()
			.extract()
			.body()
			.as(JsonNode.class);
		
		assertTrue(reviewDetails.isArray());
		assertEquals(1, reviewDetails.size());
	}
	
	@Test
	public void createConflictingStatedAndInferredMergeReview() {
		givenBranchWithPath(testBranchPath);
		final IBranchPath setupBranch = createNestedBranch(testBranchPath, "a");
		
		// Create new stated relationship on "Finding context"
		Map<?, ?> relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, 
				MODULE_SCT_CORE,
				"New stated relationship on child");
		
		assertComponentCreated(setupBranch, SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// Another inferred relationship goes on the parent branch
		relationshipRequestBody = givenRelationshipRequestBody(FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, 
				MODULE_SCT_CORE,
				CharacteristicType.INFERRED_RELATIONSHIP,
				"New inferred relationship on parent");
		
		assertComponentCreated(setupBranch.getParent(), SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		// See what happened on the sibling branch before merging changes to its parent
		final String reviewId = andCreatedMergeReview(setupBranch.getPath(), setupBranch.getParentPath());
		assertReviewCurrent(reviewId);
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then()
			.statusCode(200)
			.and()
			.extract()
			.body()
			.as(JsonNode.class);
		
		// Concept will still be returned as requiring manual merge.
		assertTrue(reviewDetails.isArray());
		assertEquals(0, reviewDetails.size());
	}
	
	@Test
	public void setReviewStale() {
		givenBranchWithPath(testBranchPath);

		// Set up a review...
		final String reviewId = andCreatedMergeReview("MAIN", testBranchPath.getPath());
		assertReviewCurrent(reviewId);
		
		// ...then commit to the branch.
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, conceptRequestBody);
		
		whenRetrievingMergeReview(reviewId)
		.then()
			.statusCode(200)
			.body("status", equalTo(ReviewStatus.STALE.toString()));		
	}
	
	@Test
	public void setReviewStaleAfterParentRebase() {
		givenBranchWithPath(testBranchPath);
		// Create all branches down to MAIN/test/A
		IBranchPath nestedBranchPath = createNestedBranch(testBranchPath, "A");
		
		// Create a new concept on MAIN
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(testBranchPath.getParent(), SnomedComponentType.CONCEPT, conceptRequestBody);

		// Set up review for a rebase of MAIN/test/A
		final String reviewId = andCreatedMergeReview(testBranchPath.getPath(), nestedBranchPath.getPath());
		assertReviewCurrent(reviewId);

		// Rebase testBranchPath on MAIN
		assertBranchCanBeRebased(testBranchPath, "Rebasing testBranchPath on MAIN");

		whenRetrievingMergeReview(reviewId)
		.then()
			.statusCode(200)
			.body("status", equalTo(ReviewStatus.STALE.toString()));		
	}
}
