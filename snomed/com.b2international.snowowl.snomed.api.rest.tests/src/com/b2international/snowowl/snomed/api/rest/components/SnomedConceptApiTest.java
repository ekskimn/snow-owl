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
package com.b2international.snowowl.snomed.api.rest.components;

import static com.b2international.snowowl.datastore.BranchPathUtils.createMainPath;
import static com.b2international.snowowl.datastore.BranchPathUtils.createPath;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.IS_A;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.INVALID_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenDeletingBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanBeDeleted;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanBeUpdated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreatedWithStatus;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentNotExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentHasProperty;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentNotCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenConceptRequestBody;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenRelationshipRequestBody;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.core.domain.InactivationIndicator;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jayway.restassured.response.ResponseBodyExtractionOptions;

/**
 * @since 2.0
 */
public class SnomedConceptApiTest extends AbstractSnomedApiTest {

	// Values below were picked from the minified dataset, representing an inactive concept
	private static final String INACTIVE_CONCEPT_ID = "118225008";
	private static final InactivationIndicator INACTIVE_CONCEPT_REASON = InactivationIndicator.AMBIGUOUS;
	private static final List<String> INACTIVE_CONCEPT_EQUIVALENTS = ImmutableList.of("118222006", "250171008", "413350009");
	
	@Test
	public void createConceptNonExistentBranch() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreatedWithStatus(createPath("MAIN/1998-01-31"), SnomedComponentType.CONCEPT, requestBody, 404)
		.and().body("status", equalTo(404));
	}

	@Test
	public void createConceptWithoutParent() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, "", MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);		
		assertComponentCreatedWithStatus(createMainPath(), SnomedComponentType.CONCEPT, requestBody, 400)
		.and().body("message", equalTo("1 validation error"))
		.and().body("violations", hasItem("'parentId' may not be empty (was '')"));
	}

	@Test
	public void createConceptWithNonexistentParent() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, "1000", MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentNotCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConceptWithNonexistentLanguageRefSet() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, INVALID_ACCEPTABILITY_MAP, false);
		assertComponentNotCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConceptWithNonexistentModule() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, "1", PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentNotCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConceptWithoutCommitComment() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, true);
		assertComponentNotCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConcept() {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConceptWithGeneratedId() {
		final ISnomedIdentifierService identifierService = ApplicationContext.getInstance().getServiceChecked(ISnomedIdentifierService.class);
		final String conceptId = identifierService.reserve(null, ComponentCategory.CONCEPT);
		final Map<?, ?> requestBody = givenConceptRequestBody(conceptId, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);		
		final String createdId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);
		assertEquals("Pre-generated and returned concept ID should match.", conceptId, createdId);
	}

	@Test
	public void createConceptOnBranch() {
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, requestBody);
	}

	@Test
	public void createConceptWithGeneratedIdOnBranch() {
		givenBranchWithPath(testBranchPath);
		final ISnomedIdentifierService identifierService = ApplicationContext.getInstance().getServiceChecked(ISnomedIdentifierService.class);
		final String conceptId = identifierService.reserve(null, ComponentCategory.CONCEPT);
		final Map<?, ?> requestBody = givenConceptRequestBody(conceptId, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String createdId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, requestBody);
		assertEquals("Pre-generated and returned concept ID should match.", conceptId, createdId);
	}

	@Test
	public void createConceptOnDeletedBranch() {
		givenBranchWithPath(testBranchPath);
		whenDeletingBranchWithPath(testBranchPath);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);		
		assertComponentNotCreated(testBranchPath, SnomedComponentType.CONCEPT, requestBody);
	}
	
	@Test
	public void createConceptISACycle_Simple() throws Exception {
		final Map<?, ?> body = givenConceptRequestBody(null, DISEASE, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, body);
		// try creating a relationship between the ROOT_CONCEPT and the newConceptId
		final Map<?, ?> newRelationshipBody = givenRelationshipRequestBody(DISEASE, IS_A, conceptId, MODULE_SCT_CORE, "Trying to create a 1 long ISA cycle");
		assertComponentNotCreated(createMainPath(), SnomedComponentType.RELATIONSHIP, newRelationshipBody);
	}
	
	@Test
	public void createConceptISACycle_Long() throws Exception {
		final Map<?, ?> body = givenConceptRequestBody(null, DISEASE, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, body);
		
		final Map<?, ?> body2 = givenConceptRequestBody(null, conceptId, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, body2);
		
		final Map<?, ?> newRelationshipBody = givenRelationshipRequestBody(DISEASE, IS_A, conceptId, MODULE_SCT_CORE, "Trying to create a 2 long ISA cycle");
		assertComponentNotCreated(createMainPath(), SnomedComponentType.RELATIONSHIP, newRelationshipBody);
	}
	
	@Test
	public void inactivateConcept() throws Exception {
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> body = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String componentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		final Map<String, Object> inactivationBody = newHashMap();
		inactivationBody.put("active", false);
		inactivationBody.put("commitComment", "Inactivated " + componentId);
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, componentId, inactivationBody);
		assertComponentHasProperty(testBranchPath, SnomedComponentType.CONCEPT, componentId, "active", false);
	}
	
	@Test
	public void reactivateConcept() throws Exception {
		// create two concepts, add an additional relationship pointing from one to the other
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> body = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String inactivatableConceptId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		final String sourceConceptId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		final Map<?, ?> relationshipReq = givenRelationshipRequestBody(sourceConceptId, Concepts.MORPHOLOGY, inactivatableConceptId, Concepts.MODULE_SCT_CORE, "New relationship");
		final String relationshipId = assertComponentCreated(testBranchPath, SnomedComponentType.RELATIONSHIP, relationshipReq);
		
		// inactivate the concept with the relationship is pointing to
		final Map<String, Object> inactivationBody = newHashMap();
		inactivationBody.put("active", false);
		inactivationBody.put("inactivationIndicator", InactivationIndicator.DUPLICATE);
		inactivationBody.put("associationTargets", ImmutableMap.builder().put(AssociationType.POSSIBLY_EQUIVALENT_TO, newArrayList(sourceConceptId)).build());
		inactivationBody.put("commitComment", "Inactivated " + inactivatableConceptId);
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, inactivatableConceptId, inactivationBody);
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, inactivatableConceptId)
			.and()
			.body("active", equalTo(false))
			.and()
			.body("inactivationIndicator", equalTo(InactivationIndicator.DUPLICATE.toString()))
			.and()
			.body("associationTargets." + AssociationType.POSSIBLY_EQUIVALENT_TO.name(), hasItem(sourceConceptId));
		
		// verify that the inbound relationship is inactive
		assertComponentExists(testBranchPath, SnomedComponentType.RELATIONSHIP, relationshipId).and().body("active", equalTo(false));
		
		// reactivate it
		final Map<String, Object> reactivationBody = newHashMap();
		reactivationBody.put("active", true);
		reactivationBody.put("commitComment", "Reactivated " + inactivatableConceptId);
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, inactivatableConceptId, reactivationBody);
		
		// assert that the concept is active again, it has two active descriptions, no association targets, no indicator, and 1 outbound relationship, and one inbound relationship
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, inactivatableConceptId)
			.and()
			.body("active", equalTo(true))
			.and()
			.body("inactivationIndicator", nullValue())
			.and()
			.body("associationTargets", nullValue());
		
		// verify that the inbound relationship is still inactive, manual reactivation is required
		assertComponentExists(testBranchPath, SnomedComponentType.RELATIONSHIP, relationshipId).and().body("active", equalTo(false));
	}
	
	@Test
	public void restoreEffectiveTimeOnReleasedConcept() throws Exception {
		givenBranchWithPath(testBranchPath);
		
		final Map<?, ?> reactivationBody = ImmutableMap.builder()
				.put("active", true)
				.put("commitComment", "Reactivated " + INACTIVE_CONCEPT_ID)
				.build();
		
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, INACTIVE_CONCEPT_ID, reactivationBody);
		
		final Map<?, ?> inactivationBody = ImmutableMap.builder()
				.put("active", false)
				.put("associationTargets", ImmutableMap.of(AssociationType.POSSIBLY_EQUIVALENT_TO, INACTIVE_CONCEPT_EQUIVALENTS))
				.put("inactivationIndicator", INACTIVE_CONCEPT_REASON.toString())
				.put("commitComment", "Reactivated " + INACTIVE_CONCEPT_ID)
				.build();
		
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, INACTIVE_CONCEPT_ID, inactivationBody);
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, INACTIVE_CONCEPT_ID);
		
		final ResponseBodyExtractionOptions componentMembers = getComponentMembers(testBranchPath, INACTIVE_CONCEPT_ID);
		final Collection<String> memberIds = componentMembers.path("items.id");
		assertEquals(4, memberIds.size());

		final Collection<Boolean> statuses = componentMembers.path("items.active");
		assertThat(statuses, everyItem(is(true)));
		final Collection<String> effectiveTimes = componentMembers.path("items.effectiveTime");
		assertThat(effectiveTimes, everyItem(either(is("20050131")).or(is("20050731"))));
	}
	
	@Test
	public void updateAssociationTarget() throws Exception {
		// create concept and a duplicate
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> body = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String componentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		final String duplicateComponentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		
		// inactivate the duplicate concept and point to the original one
		final Map<String, Object> inactivationBody = newHashMap();
		inactivationBody.put("active", false);
		inactivationBody.put("inactivationIndicator", InactivationIndicator.DUPLICATE);
		inactivationBody.put("associationTargets", ImmutableMap.builder().put(AssociationType.POSSIBLY_EQUIVALENT_TO, newArrayList(componentId)).build());
		inactivationBody.put("commitComment", "Inactivated " + duplicateComponentId);
		
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId, inactivationBody);
		// check if inactivation went through properly
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId)
			.and()
			.body("active", equalTo(false))
			.and()
			.body("inactivationIndicator", equalTo(InactivationIndicator.DUPLICATE.toString()))
			.and()
			.body("associationTargets." + AssociationType.POSSIBLY_EQUIVALENT_TO.name(), hasItem(componentId));
		
		// try to update the association target
		final Map<String, Object> associationTargetUpdateBody = newHashMap();
		associationTargetUpdateBody.put("active", false);
		associationTargetUpdateBody.put("inactivationIndicator", InactivationIndicator.AMBIGUOUS);
		associationTargetUpdateBody.put("associationTargets", ImmutableMap.builder().put(AssociationType.REPLACED_BY, newArrayList(componentId)).build());
		associationTargetUpdateBody.put("commitComment", "Changed association target to be replaced by instead in " + duplicateComponentId);
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId, associationTargetUpdateBody);

		// verify association target and inactivation indicator update
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId)
			.and()
			.body("active", equalTo(false))
			.and()
			.body("inactivationIndicator", equalTo(InactivationIndicator.AMBIGUOUS.toString()))
			.and()
			.body("associationTargets." + AssociationType.REPLACED_BY.name(), hasItem(componentId));
	}
	
	@Test
	public void updateAssociationTargetsWithReuse() throws Exception {
		// create concept and a duplicate
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> body = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String componentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		final String duplicateComponentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, body);
		
		// inactivate the duplicate concept and point to the original one
		final Map<String, Object> inactivationBody = newHashMap();
		inactivationBody.put("active", false);
		inactivationBody.put("inactivationIndicator", InactivationIndicator.DUPLICATE);
		inactivationBody.put("associationTargets", ImmutableMap.builder().put(AssociationType.POSSIBLY_EQUIVALENT_TO, newArrayList(componentId)).build());
		inactivationBody.put("commitComment", "Inactivated " + duplicateComponentId);
		
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId, inactivationBody);
		// check if inactivation went through properly
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId)
				.body("active", equalTo(false))
				.body("inactivationIndicator", equalTo(InactivationIndicator.DUPLICATE.toString()))
				.body("associationTargets." + AssociationType.POSSIBLY_EQUIVALENT_TO.name(), hasItem(componentId));
		
		final Collection<String> memberIds = getComponentMembers(testBranchPath, duplicateComponentId).path("items.id");

		// retrieve association member and store its UUID
		assertEquals(2, memberIds.size());
		
		// try to update the association target, switching the order of targets around
		final Map<String, Object> associationTargetUpdateBody = newHashMap();
		associationTargetUpdateBody.put("active", false);
		associationTargetUpdateBody.put("inactivationIndicator", InactivationIndicator.AMBIGUOUS);
		associationTargetUpdateBody.put("associationTargets", ImmutableMap.builder()
				.put(AssociationType.POSSIBLY_EQUIVALENT_TO, newArrayList(DISEASE, componentId))
				.put(AssociationType.REPLACED_BY, newArrayList(componentId))
				.build());
		associationTargetUpdateBody.put("commitComment", "Changed association targets on " + duplicateComponentId);
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId, associationTargetUpdateBody);
		
		// verify association target and inactivation indicator update
		assertComponentExists(testBranchPath, SnomedComponentType.CONCEPT, duplicateComponentId)
				.body("active", equalTo(false))
				.body("inactivationIndicator", equalTo(InactivationIndicator.AMBIGUOUS.toString()))
				.body("associationTargets." + AssociationType.POSSIBLY_EQUIVALENT_TO.name(), hasItem(componentId))
				.body("associationTargets." + AssociationType.POSSIBLY_EQUIVALENT_TO.name(), hasItem(DISEASE))
				.body("associationTargets." + AssociationType.REPLACED_BY.name(), hasItem(componentId));
		
		final Collection<String> updatedMemberIds = getComponentMembers(testBranchPath, duplicateComponentId).path("items.id");
		// check that the member UUIDs have not been cycled
		assertEquals(4, updatedMemberIds.size());
		assertTrue(updatedMemberIds.containsAll(memberIds));
	}
	
	@Test
	public void createDuplicateConcept() throws Exception {
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, requestBody);

		final Map<Object, Object> dupRequestBody = Maps.<Object, Object>newHashMap(requestBody);
		dupRequestBody.put("id", conceptId);
		dupRequestBody.put("commitComment", "New duplicate concept on MAIN");
		assertComponentCreatedWithStatus(createMainPath(), SnomedComponentType.CONCEPT, dupRequestBody, 409);
	}
	
	@Test
	public void findConceptsById() {
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.when().get("/{path}/concepts?conceptIds=255203001,72670004", createMainPath().getPath())
				.then()
				.log()
				.ifValidationFails()
				.assertThat()
				.statusCode(200)
				.body("total", equalTo(2))
				.body("items.id[0]", equalTo("255203001"))
				.body("items.id[1]", equalTo("72670004"))
				;
	}
	
	@Test
	public void deleteConcept() {
		givenBranchWithPath(testBranchPath);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String conceptId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, requestBody);
		assertComponentCanBeDeleted(testBranchPath, SnomedComponentType.CONCEPT, conceptId);
		assertComponentNotExists(testBranchPath, SnomedComponentType.CONCEPT, conceptId);
	}
	
	@Test
	public void deleteConceptOnNestedBranch() {
		givenBranchWithPath(testBranchPath);

		Map<?, ?> requestBody;
		String parentId = ROOT_CONCEPT;
		
		for (int i = 0; i < 10; i++) {
			requestBody = givenConceptRequestBody(null, parentId, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
			parentId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, requestBody);
		}

		// New component on nested branch resets the container's version to 1 again
		final IBranchPath nestedBranchPath = createNestedBranch(testBranchPath, "A", "B");
		requestBody = givenConceptRequestBody(null, parentId, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(nestedBranchPath, SnomedComponentType.CONCEPT, requestBody);

		// Deleting the last concept in the chain
		assertComponentCanBeDeleted(testBranchPath, SnomedComponentType.CONCEPT, parentId);
		assertComponentNotExists(testBranchPath, SnomedComponentType.CONCEPT, parentId);

		// Should still exist on the nested branch, and be possible to remove
		assertComponentCanBeDeleted(nestedBranchPath, SnomedComponentType.CONCEPT, parentId);
		assertComponentNotExists(nestedBranchPath, SnomedComponentType.CONCEPT, parentId);
	}

	private ResponseBodyExtractionOptions getComponentMembers(IBranchPath branchPath, String componentId) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.when().get("/{path}/{componentType}?referencedComponentId={componentId}", branchPath.getPath(), SnomedComponentType.MEMBER.toLowerCasePlural(), componentId)
			.then().log().ifValidationFails().extract().body();
	}
}
