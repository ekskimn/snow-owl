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

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.SYNONYM;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.review.ReviewStatus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedBranchingRestRequests;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentRestRequests;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.api.rest.SnomedMergingRestRequests;
import com.b2international.snowowl.snomed.api.rest.SnomedRestFixtures;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
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
		whenCreatingMergeReview(branchPath.getPath(), Branch.MAIN_PATH)
		.then()
			.statusCode(400);
	}
	
	@Test
	public void createReview() {
		SnomedBranchingRestRequests.createBranch(branchPath);
		final String reviewId = andCreatedMergeReview(branchPath.getParentPath(), branchPath.getPath());
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
		SnomedBranchingRestRequests.createBranch(branchPath);
		
		final IBranchPath setupBranch = BranchPathUtils.createPath(branchPath, "a"); 
		SnomedBranchingRestRequests.createBranchRecursively(setupBranch);
		
		// Create new inferred relationship on "Finding context"
		SnomedRestFixtures.createNewRelationship(setupBranch, FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, CharacteristicType.INFERRED_RELATIONSHIP);
		
		
//		// Another inferred relationship goes on the parent branch
		SnomedRestFixtures.createNewRelationship(setupBranch.getParent(), FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, CharacteristicType.INFERRED_RELATIONSHIP);
//		assertComponentCreated(setupBranch.getParent(), SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
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
		final IBranchPath setupBranch = BranchPathUtils.createPath(branchPath, "a");
		SnomedBranchingRestRequests.createBranchRecursively(setupBranch);
		
		// Create new stated relationship on "Finding context"
		SnomedRestFixtures.createNewRelationship(setupBranch, FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, CharacteristicType.STATED_RELATIONSHIP);
		
		
		// Another stated relationship goes on the parent branch
		SnomedRestFixtures.createNewRelationship(setupBranch.getParent(), FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, CharacteristicType.STATED_RELATIONSHIP);
		
		// See what happened on the sibling branch before merging changes to its parent
		String reviewId = andCreatedMergeReview(setupBranch.getPath(), setupBranch.getParentPath());
		assertReviewCurrent(reviewId);
		
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then().assertThat().statusCode(200)
		.and()
		.extract()
		.body()
		.as(JsonNode.class);
		
		
		assertTrue(reviewDetails.isArray());
		assertEquals(1, reviewDetails.size());
	}
	
	@Test
	public void createConflictingStatedAndInferredMergeReview() {
		SnomedBranchingRestRequests.createBranch(branchPath);
		final IBranchPath setupBranch = BranchPathUtils.createPath(branchPath, "a");
		SnomedBranchingRestRequests.createBranchRecursively(setupBranch);
		
		// Create new stated relationship on "Finding context"
		SnomedRestFixtures.createNewRelationship(setupBranch, FINDING_CONTEXT, Concepts.IS_A, Concepts.ROOT_CONCEPT, CharacteristicType.STATED_RELATIONSHIP);
		
		
		// Another inferred relationship goes on the parent branch
		SnomedRestFixtures.createNewRelationship(setupBranch.getParent(), FINDING_CONTEXT, Concepts.IS_A, Concepts.MODULE_ROOT, CharacteristicType.INFERRED_RELATIONSHIP);
		
		String reviewId = andCreatedMergeReview(setupBranch.getPath(), setupBranch.getParentPath());
		// See what happened on the sibling branch before merging changes to its parent
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
	public void setReviewStale() throws Exception {
		SnomedBranchingRestRequests.createBranch(branchPath);

		// Set up a review...
		String reviewId = andCreatedMergeReview(branchPath.getParentPath(), branchPath.getPath());
		assertReviewCurrent(reviewId);
		
		
		// ...then commit to the branch.
		SnomedRestFixtures.createNewConcept(branchPath);
		// wait 1s before checking review state 
		Thread.sleep(1000);
		
		whenRetrievingMergeReview(reviewId)
		.then()
			.statusCode(200)
			.body("status", equalTo(ReviewStatus.STALE.toString()));		
	}
	
	@Test
	public void setReviewStaleAfterParentRebase() {
		SnomedBranchingRestRequests.createBranch(branchPath);
		// Create all branches down to MAIN/test/A
		IBranchPath nestedBranchPath =  BranchPathUtils.createPath(branchPath, "A");
		SnomedBranchingRestRequests.createBranchRecursively(nestedBranchPath);
		

		// Set up review for a rebase of MAIN/test/A
		final String reviewId = andCreatedMergeReview(branchPath.getPath(), nestedBranchPath.getPath());
		assertReviewCurrent(reviewId);

		// Rebase branchPath on MAIN
		SnomedMergingRestRequests.createMerge(nestedBranchPath, branchPath, "Rebasing branchPath on MAIN", reviewId);

		// Create a new concept on MAIN
		SnomedRestFixtures.createNewConcept(nestedBranchPath);

		whenRetrievingMergeReview(reviewId)
		.then()
			.statusCode(200)
			.body("status", equalTo(ReviewStatus.STALE.toString()));		
	}
	
	@Test
	public void noDescriptionIdsInChangedConceptIdsWhenConceptChangedForMergeReview() {
		SnomedBranchingRestRequests.createBranch(branchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(branchPath, "1");
		SnomedBranchingRestRequests.createBranch(firstTaskPath);
		
		String concept = SnomedRestFixtures.createNewConcept(firstTaskPath);
		SnomedRestFixtures.merge(firstTaskPath, firstTaskPath.getParent(), "Merging first task into project");
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.CONCEPT, concept).statusCode(200);
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(branchPath, "2");
		SnomedBranchingRestRequests.createBranch(secondTaskPath);
		SnomedRestFixtures.createNewDescription(secondTaskPath, concept, SYNONYM);
		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(branchPath, "3");
		SnomedBranchingRestRequests.createBranch(thirdTaskPath);
		SnomedRestFixtures.createNewDescription(thirdTaskPath, concept, SYNONYM);
		
		SnomedRestFixtures.merge(thirdTaskPath, thirdTaskPath.getParent(), "Merging new description to project");
		
		final String reviewId = andCreatedMergeReview(branchPath.getPath(), secondTaskPath.getPath());
		assertReviewCurrent(reviewId);
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then()
			.statusCode(200)
			.and()
			.extract()
			.body()
			.as(JsonNode.class);
		
		assertTrue(reviewDetails.isArray());
		assertEquals(1, reviewDetails.size());
		
		JsonNode jsonNode = reviewDetails.get(0).get("autoMergedConcept");
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().header("Accept-Language", "en-GB")
			.content(jsonNode)
			.contentType(ContentType.JSON)
			.when()
			.post("/merge-reviews/{id}/{conceptId}", reviewId, concept)
			.then()
			.statusCode(200);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().header("Accept-Language", "en-GB")
			.when()
			.post("/merge-reviews/{id}/apply", reviewId)
			.then()
			.statusCode(204);
		
	}
	
	@Test
	public void noDescriptionIdsInChangedConceptIdsWhenConceptDeletedForMergeReview() {
		
		SnomedBranchingRestRequests.createBranch(branchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(branchPath, "1");
		SnomedBranchingRestRequests.createBranch(firstTaskPath);
		
		String concept = SnomedRestFixtures.createNewConcept(firstTaskPath);
		SnomedRestFixtures.merge(firstTaskPath, firstTaskPath.getParent(), "Merging first task into project");
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.CONCEPT, concept).statusCode(200);
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(branchPath, "2");
		SnomedBranchingRestRequests.createBranch(secondTaskPath);
		
		SnomedRestFixtures.createNewDescription(secondTaskPath, concept, SYNONYM);
		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(branchPath, "3");
		SnomedBranchingRestRequests.createBranch(thirdTaskPath);
		SnomedRestFixtures.createNewDescription(thirdTaskPath, concept, SYNONYM);
		
		
		SnomedComponentRestRequests.deleteComponent(thirdTaskPath, SnomedComponentType.CONCEPT, concept, true);
		SnomedComponentRestRequests.getComponent(thirdTaskPath, SnomedComponentType.CONCEPT, concept).statusCode(404);
		
		SnomedRestFixtures.merge(thirdTaskPath, thirdTaskPath.getParent(), "Merging concept deletion to project");
		
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.CONCEPT, concept).statusCode(404);
		
		final String reviewId = andCreatedMergeReview(branchPath.getPath(), secondTaskPath.getPath());
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
	public void noDescriptionIdsInChangedConceptIdsForAcceptedMergeReview() {
		
		SnomedBranchingRestRequests.createBranch(branchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(branchPath, "1");
		SnomedBranchingRestRequests.createBranch(firstTaskPath);
		
		String conceptId = SnomedRestFixtures.createNewConcept(firstTaskPath);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.CONCEPT, conceptId).statusCode(200);
		
		
		
		String descriptionId = SnomedRestFixtures.createNewDescription(firstTaskPath, conceptId, SYNONYM);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId).statusCode(200);
		
		
		final Map<Object, Object> descriptionUpdateBody = ImmutableMap.<Object, Object>builder()
				.put("acceptability", Collections.emptyMap())
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description")
				.build();
		
		
		SnomedComponentRestRequests.updateComponent(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBody);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId).body("acceptability", equalTo(Collections.emptyMap()));
		
		SnomedRestFixtures.merge(firstTaskPath, firstTaskPath.getParent(), "Merging first task into project");
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.CONCEPT, conceptId).statusCode(200);
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.DESCRIPTION, descriptionId).statusCode(200);
		
		

		IBranchPath secondTaskPath = BranchPathUtils.createPath(branchPath, "2");
		SnomedBranchingRestRequests.createBranch(secondTaskPath);
		
		final Map<Object, Object> descriptionUpdateBodyUk = ImmutableMap.<Object, Object>builder()
				.put("acceptability", SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP)
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description on second task")
				.build();
		
		SnomedComponentRestRequests.updateComponent(secondTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBodyUk);
		
		
		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(branchPath, "3");
		SnomedBranchingRestRequests.createBranch(thirdTaskPath);
		
		Map<String, Acceptability> usAcceptabilityMapPreferred = ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_US, Acceptability.PREFERRED);
		
		final Map<Object, Object> descriptionUpdateBodyUs = ImmutableMap.<Object, Object>builder()
				.put("acceptability", usAcceptabilityMapPreferred)
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description on third task")
				.build();
		
		SnomedComponentRestRequests.updateComponent(thirdTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBodyUs);
		
		SnomedRestFixtures.merge(thirdTaskPath, thirdTaskPath.getParent(), "Merging third task to project");
		
		final String reviewId = andCreatedMergeReview(branchPath.getPath(), secondTaskPath.getPath());
		assertReviewCurrent(reviewId);
		
		JsonNode reviewDetails = whenRetrievingMergeReviewDetails(reviewId).then()
			.statusCode(200)
			.and()
			.extract()
			.body()
			.as(JsonNode.class);
		
		assertTrue(reviewDetails.isArray());
		assertEquals(1, reviewDetails.size());
		
		JsonNode jsonNode = reviewDetails.get(0).get("autoMergedConcept");
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().header("Accept-Language", "en-GB")
			.content(jsonNode)
			.contentType(ContentType.JSON)
			.when()
			.post("/merge-reviews/{id}/{conceptId}", reviewId, conceptId)
			.then()
			.statusCode(200);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().header("Accept-Language", "en-GB")
			.when()
			.post("/merge-reviews/{id}/apply", reviewId)
			.then()
			.statusCode(204);
		
	}
	
	@Test
	public void noRelationshipIdsInChangedConceptIdsForMergeReview() {
		
		SnomedBranchingRestRequests.createBranch(branchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(branchPath, "1");
		SnomedBranchingRestRequests.createBranch(firstTaskPath);
		
		String conceptId = SnomedRestFixtures.createNewConcept(firstTaskPath);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.CONCEPT, conceptId).statusCode(200);
		
		final String relationshipId = SnomedRestFixtures.createNewRelationship(firstTaskPath, conceptId, Concepts.IS_A, FINDING_CONTEXT, CharacteristicType.STATED_RELATIONSHIP);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.RELATIONSHIP, relationshipId).statusCode(200);
		
		final String refSetId = SnomedRestFixtures.createNewRefSet(firstTaskPath, SnomedRefSetType.SIMPLE);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.REFSET, refSetId).statusCode(200);
		
		
		
		final String memberId = SnomedRestFixtures.createNewRefSetMember(firstTaskPath, conceptId, refSetId);
		SnomedComponentRestRequests.getComponent(firstTaskPath, SnomedComponentType.MEMBER, memberId).statusCode(200);
		

		SnomedRestFixtures.merge(firstTaskPath, firstTaskPath.getParent(), "Merging first task into project");
		SnomedComponentRestRequests.getComponent(branchPath, SnomedComponentType.CONCEPT, conceptId).statusCode(200);
		
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(branchPath, "2");
		SnomedBranchingRestRequests.createBranch(secondTaskPath);

		SnomedComponentRestRequests.getComponent(secondTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
				.body("moduleId", CoreMatchers.equalTo(Concepts.MODULE_SCT_CORE));
		
		
		final Map<?, ?> moduleUpdate = ImmutableMap.of("moduleId", "900000000000012004", "commitComment", "Update member module: " + memberId);

		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(moduleUpdate)
			.when().put("/{path}/{componentType}/{id}", secondTaskPath, SnomedComponentType.MEMBER.toLowerCasePlural(), memberId);
		
		
		SnomedComponentRestRequests.getComponent(secondTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.body("moduleId", CoreMatchers.equalTo("900000000000012004"));

		IBranchPath thirdTaskPath = BranchPathUtils.createPath(branchPath, "3");
		SnomedBranchingRestRequests.createBranch(thirdTaskPath);

		
		SnomedComponentRestRequests.getComponent(thirdTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.body("active", CoreMatchers.equalTo(true));
	
		final Map<?, ?> inactivationReq = ImmutableMap.of("active", false, "commitComment", "Inactivate member: " + memberId);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(inactivationReq)
			.when().put("/{path}/{componentType}/{id}", thirdTaskPath.getPath(), SnomedComponentType.MEMBER.toLowerCasePlural(), memberId);

		
		SnomedComponentRestRequests.getComponent(thirdTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.body("active", CoreMatchers.equalTo(false));
		
		SnomedRestFixtures.merge(thirdTaskPath, thirdTaskPath.getParent(), "Merging third task to project");
		
		final String reviewId = andCreatedMergeReview(branchPath.getPath(), secondTaskPath.getPath());
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
}
