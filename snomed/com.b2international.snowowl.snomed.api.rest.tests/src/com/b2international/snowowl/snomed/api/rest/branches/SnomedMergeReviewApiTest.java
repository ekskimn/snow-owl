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
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.SYNONYM;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.ACCEPTABLE_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.assertBranchCanBeMerged;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.assertBranchCanBeRebased;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.*;
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
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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
	public void setReviewStale() throws Exception {
		givenBranchWithPath(testBranchPath);

		// Set up a review...
		final String reviewId = andCreatedMergeReview("MAIN", testBranchPath.getPath());
		assertReviewCurrent(reviewId);
		
		// ...then commit to the branch.
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, conceptRequestBody);
		
		// wait 1s before checking review state 
		Thread.sleep(1000);
		
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
	
	@Test
	public void noDescriptionIdsInChangedConceptIdsWhenConceptChangedForMergeReview() {
		
		givenBranchWithPath(testBranchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(testBranchPath, "1");
		givenBranchWithPath(firstTaskPath);
		
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String concept = assertComponentCreated(firstTaskPath, SnomedComponentType.CONCEPT, requestBody);
		
		assertComponentExists(firstTaskPath, SnomedComponentType.CONCEPT, concept);
		assertBranchCanBeMerged(firstTaskPath, "Merging first task into project");
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, concept);
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(testBranchPath, "2");
		givenBranchWithPath(secondTaskPath);
		
		final Map<Object, Object> descriptionForSecondTask = Maps.<Object, Object>newHashMap(givenDescriptionRequestBody(MODULE_SCT_CORE, "New SYN at ", ACCEPTABLE_ACCEPTABILITY_MAP, SYNONYM));
		descriptionForSecondTask.put("conceptId", concept);
		descriptionForSecondTask.put("commitComment", "Created new synonym");
		
		assertComponentCreated(secondTaskPath, SnomedComponentType.DESCRIPTION, descriptionForSecondTask);
		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(testBranchPath, "3");
		givenBranchWithPath(thirdTaskPath);
		
		final Map<Object, Object> descriptionForThirdTask = Maps.<Object, Object>newHashMap(givenDescriptionRequestBody(MODULE_SCT_CORE, "New SYN at third ", ACCEPTABLE_ACCEPTABILITY_MAP, SYNONYM));
		descriptionForThirdTask.put("conceptId", concept);
		descriptionForThirdTask.put("commitComment", "Created new synonym");
		
		assertComponentCreated(thirdTaskPath, SnomedComponentType.DESCRIPTION, descriptionForThirdTask);
		
		assertBranchCanBeMerged(thirdTaskPath, "Merging new description to project");
		
		final String reviewId = andCreatedMergeReview(testBranchPath.getPath(), secondTaskPath.getPath());
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
		
		givenBranchWithPath(testBranchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(testBranchPath, "1");
		givenBranchWithPath(firstTaskPath);
		
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String concept = assertComponentCreated(firstTaskPath, SnomedComponentType.CONCEPT, requestBody);
		
		assertComponentExists(firstTaskPath, SnomedComponentType.CONCEPT, concept);
		assertBranchCanBeMerged(firstTaskPath, "Merging first task into project");
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, concept);
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(testBranchPath, "2");
		givenBranchWithPath(secondTaskPath);
		
		final Map<Object, Object> descriptionForSecondTask = Maps.<Object, Object>newHashMap(givenDescriptionRequestBody(MODULE_SCT_CORE, "New SYN at ", ACCEPTABLE_ACCEPTABILITY_MAP, SYNONYM));
		descriptionForSecondTask.put("conceptId", concept);
		descriptionForSecondTask.put("commitComment", "Created new synonym");
		
		assertComponentCreated(secondTaskPath, SnomedComponentType.DESCRIPTION, descriptionForSecondTask);
		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(testBranchPath, "3");
		givenBranchWithPath(thirdTaskPath);
		
		final Map<Object, Object> descriptionForThirdTask = Maps.<Object, Object>newHashMap(givenDescriptionRequestBody(MODULE_SCT_CORE, "New SYN at third ", ACCEPTABLE_ACCEPTABILITY_MAP, SYNONYM));
		descriptionForThirdTask.put("conceptId", concept);
		descriptionForThirdTask.put("commitComment", "Created new synonym");
		
		assertComponentCreated(thirdTaskPath, SnomedComponentType.DESCRIPTION, descriptionForThirdTask);
		
		assertComponentCanBeDeleted(thirdTaskPath, SnomedComponentType.CONCEPT, concept);
		assertComponentNotExists(thirdTaskPath, SnomedComponentType.CONCEPT, concept);
		
		assertBranchCanBeMerged(thirdTaskPath, "Merging concept deletion to project");
		
		assertComponentNotExists(testBranchPath, SnomedComponentType.CONCEPT, concept);
		
		final String reviewId = andCreatedMergeReview(testBranchPath.getPath(), secondTaskPath.getPath());
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
		
		givenBranchWithPath(testBranchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(testBranchPath, "1");
		givenBranchWithPath(firstTaskPath);
		
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(firstTaskPath, SnomedComponentType.CONCEPT, requestBody);
		assertComponentExists(firstTaskPath, SnomedComponentType.CONCEPT, conceptId);
		
		final Map<Object, Object> descriptionBody = Maps.<Object, Object>newHashMap(givenDescriptionRequestBody(MODULE_SCT_CORE, "Description to change ", ACCEPTABLE_ACCEPTABILITY_MAP, SYNONYM));
		descriptionBody.put("conceptId", conceptId);
		descriptionBody.put("commitComment", "Created new synonym");
		
		String descriptionId = assertComponentCreated(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionBody);
		
		assertComponentExists(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId);
		
		final Map<Object, Object> descriptionUpdateBody = ImmutableMap.<Object, Object>builder()
				.put("acceptability", Collections.emptyMap())
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description")
				.build();
		
		assertComponentCanBeUpdated(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBody);
		assertComponentHasProperty(firstTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, "acceptabilityMap", null);
		
		assertBranchCanBeMerged(firstTaskPath, "Merging first task into project");
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, conceptId);
		assertComponentExists(testBranchPath, SnomedComponentType.DESCRIPTION, descriptionId);


		IBranchPath secondTaskPath = BranchPathUtils.createPath(testBranchPath, "2");
		givenBranchWithPath(secondTaskPath);
		
		final Map<Object, Object> descriptionUpdateBodyUk = ImmutableMap.<Object, Object>builder()
				.put("acceptability", ACCEPTABLE_ACCEPTABILITY_MAP)
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description on second task")
				.build();
		
		assertComponentCanBeUpdated(secondTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBodyUk);

		
		IBranchPath thirdTaskPath = BranchPathUtils.createPath(testBranchPath, "3");
		givenBranchWithPath(thirdTaskPath);
		
		Map<String, Acceptability> usAcceptabilityMapPreferred = ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_US, Acceptability.PREFERRED);
		
		final Map<Object, Object> descriptionUpdateBodyUs = ImmutableMap.<Object, Object>builder()
				.put("acceptability", usAcceptabilityMapPreferred)
				.put("descriptionId", descriptionId)
				.put("commitComment", "Update description on third task")
				.build();
		
		assertComponentCanBeUpdated(thirdTaskPath, SnomedComponentType.DESCRIPTION, descriptionId, descriptionUpdateBodyUs);
		
		assertBranchCanBeMerged(thirdTaskPath, "Merging third task to project");
		
		final String reviewId = andCreatedMergeReview(testBranchPath.getPath(), secondTaskPath.getPath());
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
		
		givenBranchWithPath(testBranchPath); // project branch
		
		IBranchPath firstTaskPath = BranchPathUtils.createPath(testBranchPath, "1");
		givenBranchWithPath(firstTaskPath);
		
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(firstTaskPath, SnomedComponentType.CONCEPT, requestBody);
		assertComponentExists(firstTaskPath, SnomedComponentType.CONCEPT, conceptId);
		
		final Map<?, ?> relationshipRequestBody = givenRelationshipRequestBody(conceptId, Concepts.IS_A, FINDING_CONTEXT, MODULE_SCT_CORE, "New relationship on concept");
		final String relationshipId = assertComponentCreated(firstTaskPath, SnomedComponentType.RELATIONSHIP, relationshipRequestBody);
		
		final Map<String,Object> refSetReq = createRefSetRequestBody(SnomedRefSetType.SIMPLE, SnomedTerminologyComponentConstants.RELATIONSHIP, Concepts.REFSET_SIMPLE_TYPE);
		final String refSetId = assertComponentCreated(firstTaskPath, SnomedComponentType.REFSET, refSetReq);
		assertComponentExists(firstTaskPath, SnomedComponentType.REFSET, refSetId);
		
		final Map<String, Object> memberReq = createRefSetMemberRequestBody(relationshipId, refSetId);
		String memberId = assertComponentCreated(firstTaskPath, SnomedComponentType.MEMBER, memberReq);
		
		assertComponentExists(firstTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()");
		
		assertBranchCanBeMerged(firstTaskPath, "Merging first task into project");
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, conceptId);
		
		IBranchPath secondTaskPath = BranchPathUtils.createPath(testBranchPath, "2");
		givenBranchWithPath(secondTaskPath);

		getComponent(secondTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.then()
			.body("moduleId", CoreMatchers.equalTo(Concepts.MODULE_SCT_CORE));
		
		final Map<?, ?> moduleUpdate = ImmutableMap.of("moduleId", "900000000000012004", "commitComment", "Update member module: " + memberId);

		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(moduleUpdate)
			.when().put("/{path}/{componentType}/{id}", secondTaskPath, SnomedComponentType.MEMBER.toLowerCasePlural(), memberId);
		
		getComponent(secondTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.then()
			.body("moduleId", CoreMatchers.equalTo("900000000000012004"));
			

		IBranchPath thirdTaskPath = BranchPathUtils.createPath(testBranchPath, "3");
		givenBranchWithPath(thirdTaskPath);

		getComponent(thirdTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.then()
			.body("active", CoreMatchers.equalTo(true));
	
		final Map<?, ?> inactivationReq = ImmutableMap.of("active", false, "commitComment", "Inactivate member: " + memberId);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(inactivationReq)
			.when().put("/{path}/{componentType}/{id}", thirdTaskPath.getPath(), SnomedComponentType.MEMBER.toLowerCasePlural(), memberId);

		getComponent(thirdTaskPath, SnomedComponentType.MEMBER, memberId, "referencedComponent()")
			.then()
			.body("active", CoreMatchers.equalTo(false));
		
		
		assertBranchCanBeMerged(thirdTaskPath, "Merging third task to project");
		
		final String reviewId = andCreatedMergeReview(testBranchPath.getPath(), secondTaskPath.getPath());
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
