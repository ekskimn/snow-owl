/*
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.rest.browser;

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyIsNull;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.core.domain.AssociationType;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;
import com.b2international.snowowl.snomed.datastore.SnomedInactivationPlan.InactivationReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.jayway.restassured.response.ExtractableResponse;
import com.jayway.restassured.response.Response;

/**
 * @since 4.5
 */
public class SnomedBrowserApiTest extends AbstractSnomedApiTest {

	private static Map<String, Object> createBrowserConceptRequest() {
		ImmutableMap.Builder<String, Object> conceptBuilder = ImmutableMap.<String, Object>builder()
				.put("fsn", "FSN of new concept")
				.put("preferredSynonym", "PT of new concept")
				.put("moduleId", Concepts.MODULE_SCT_CORE)
				.put("definitionStatus", DefinitionStatus.PRIMITIVE)
				.put("descriptions", createDefaultDescriptions())
				.put("relationships", createIsaRelationship());

		return conceptBuilder.build();
	}
	
	@Test
	public void createConceptWithInferredParentOnly() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate, CharacteristicType.INFERRED_RELATIONSHIP);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 400).and().body("message",
				equalTo("At least one active stated IS A relationship is required."));
	}
	
	@Test
	public void createConceptWithInactiveStatedParent() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate, false, CharacteristicType.STATED_RELATIONSHIP);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 400).and().body("message",
				equalTo("At least one active stated IS A relationship is required."));
	}

	private static List<?> createDefaultDescriptions() {
		Map<?, ?> fsnDescription = ImmutableMap.<String, Object>builder()
				.put("active", true)
				.put("term", "FSN of new concept")
				.put("type", SnomedBrowserDescriptionType.FSN)
				.put("lang", "en")
				.put("moduleId", Concepts.MODULE_SCT_CORE)
				.put("caseSignificance", CaseSignificance.CASE_INSENSITIVE)
				.put("acceptabilityMap", SnomedApiTestConstants.UK_PREFERRED_MAP)
				.build();

		Map<?, ?> ptDescription = ImmutableMap.<String, Object>builder()
				.put("active", true)
				.put("term", "PT of new concept")
				.put("type", SnomedBrowserDescriptionType.SYNONYM)
				.put("lang", "en")
				.put("moduleId", Concepts.MODULE_SCT_CORE)
				.put("caseSignificance", CaseSignificance.CASE_INSENSITIVE)
				.put("acceptabilityMap", SnomedApiTestConstants.UK_PREFERRED_MAP)
				.build();
		return ImmutableList.of(fsnDescription, ptDescription);
	}
	
	@Test
	public void createConceptWithGeneratedId() {
		final String conceptId = generateComponentId(null, ComponentCategory.CONCEPT);
		createConcept(conceptId);
	}

	private static List<?> createIsaRelationship() {
		return createIsaRelationship(Concepts.ROOT_CONCEPT);
	}
	
	private static List<?> createIsaRelationship(String parentId) {
		Map<?, ?> type = ImmutableMap.<String, Object>builder()
				.put("conceptId", Concepts.IS_A)
				.put("fsn", "Is a (attribute)")
				.build();

		Map<?, ?> target = ImmutableMap.<String, Object>builder()
				.put("active", true)
				.put("moduleId", Concepts.MODULE_SCT_CORE)
				.put("conceptId", parentId)
				.put("fsn", "Parent of new concept")
				.put("definitionStatus", DefinitionStatus.PRIMITIVE)
				.build();

		Map<?, ?> isaRelationship = ImmutableMap.<String, Object>builder()
				.put("modifier", RelationshipModifier.EXISTENTIAL)
				.put("groupId", "0")
				.put("characteristicType", CharacteristicType.STATED_RELATIONSHIP)
				.put("active", true)
				.put("type", type)
				.put("moduleId", Concepts.MODULE_SCT_CORE)
				.put("target", target)
				.build();

		final String conceptId = response.extract().jsonPath().getString("conceptId");
		final Map<String, Object> concept = response.and().extract().jsonPath().get();
		concept.put("active", false);
		concept.put("inactivationIndicator", InactivationReason.DUPLICATE.name());
		concept.put("associationTargets", ImmutableMultimap.<String, Object>of(AssociationType.REPLACED_BY.name(), MODULE_SCT_CORE).asMap());

		assertComponentUpdatedWithStatus(createMainPath(), conceptId, concept, 200);
	}
	
	@Test
	public void changeInactivationReason() throws Exception {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<String, Object> createRequestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships,
				creationDate);
		final ValidatableResponse response = assertComponentCreatedWithStatus(createMainPath(), createRequestBody, 200);
		
		final String conceptId = response.extract().jsonPath().getString("conceptId");
		final Map<String, Object> responseBody = response.extract().jsonPath().get();
		
		responseBody.put("active", false);
		responseBody.put("inactivationIndicator", InactivationReason.DUPLICATE.name());
		responseBody.put("associationTargets", ImmutableMultimap.<String, Object>of(AssociationType.REPLACED_BY.name(), MODULE_SCT_CORE).asMap());
		
		final Map<String, Object> responseBody2 = assertComponentUpdatedWithStatus(createMainPath(), conceptId, responseBody, 200)
			.and().body("inactivationIndicator", equalTo(InactivationReason.DUPLICATE.name()))
			.and().extract().jsonPath().get();
		
		responseBody2.put("inactivationIndicator", InactivationReason.AMBIGUOUS.name());
		
		assertComponentUpdatedWithStatus(createMainPath(), conceptId, responseBody2, 200)
			.and().body("inactivationIndicator", equalTo(InactivationReason.AMBIGUOUS.name()));
	}
	
	@Test
	public void reactivateConcept() throws Exception {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<String, Object> createRequestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships,
				creationDate);
		final ValidatableResponse response = assertComponentCreatedWithStatus(createMainPath(), createRequestBody, 200);
		
		final String conceptId = response.extract().jsonPath().getString("conceptId");
		final Map<String, Object> responseBody = response.extract().jsonPath().get();
		
		responseBody.put("active", false);
		responseBody.put("inactivationIndicator", InactivationReason.DUPLICATE.name());
		responseBody.put("associationTargets", ImmutableMultimap.<String, Object>of(AssociationType.REPLACED_BY.name(), MODULE_SCT_CORE).asMap());
		
		final Map<String, Object> responseBody2 = assertComponentUpdatedWithStatus(createMainPath(), conceptId, responseBody, 200)
				.and().body("inactivationIndicator", equalTo(InactivationReason.DUPLICATE.name()))
				.and().extract().jsonPath().get();
		
		responseBody2.put("active", true);
		
		final List<Map<String, Object>> relationshipsResponse = (List<Map<String, Object>>) responseBody2.get("relationships");
		relationshipsResponse.get(0).put("active", true);
		
		assertComponentUpdatedWithStatus(createMainPath(), conceptId, responseBody2, 200)
				.and().body("inactivationIndicator", nullValue())
				.and().body("associationTargets", nullValue())
				.and().body("active", equalTo(true))
				.and().body("descriptions[0].active", equalTo(true))
				.and().body("descriptions[0].inactivationIndicator", nullValue())
				.and().body("descriptions[1].active", equalTo(true))
				.and().body("descriptions[1].inactivationIndicator", nullValue())
				.and().body("relationships[0].active", equalTo(true));
	}

		final Map<String, Object> concept = response.and().extract().jsonPath().get();
		concept.remove("relationships");

		// Removing all relationships without leaving at least one stated IS A is not allowed
		assertComponentUpdatedWithStatus(createMainPath(), concept.get("conceptId").toString(), concept, 400);
	}

	@Test
	public void releasedFlagOnReleasedConceptAndComponents() {
		final String conceptId = "266719004";
		createConcept(conceptId);
		
		final IBranchPath path = createMainPath();
		assertConceptIndexedBrowserPropertyIsNull(path, conceptId, "effectiveTime");
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "released", false);
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "descriptions.released[0]", false);
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "relationships.released[0]", false);

		final String effectiveDate = "20170131";
		whenCreatingVersion("2017-01-31", effectiveDate).then().assertThat().statusCode(201);
		
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "effectiveTime", effectiveDate);
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "released", true);
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "descriptions.released[0]", true);
		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "relationships.released[0]", true);
	}
	
	@Test
	public void bulkUpdateConcepts() throws JsonProcessingException, IOException, InterruptedException {
		
		// Create concepts
		final Map<String, Object> conceptOne = createConceptWithFsn("One");
		final Map<String, Object> fsnOne = getFsn(conceptOne);
		Assert.assertEquals("One", fsnOne.get("term"));
		
		final Map<String, Object> conceptTwo = createConceptWithFsn("Two");
		final Map<String, Object> fsnTwo = getFsn(conceptTwo);
		Assert.assertEquals("Two", fsnTwo.get("term"));
		
		// Prepare updates
		List<Map<String, Object>> concepts = new ArrayList<>();

		fsnOne.put("term", "OneA");
		fsnOne.remove("descriptionId");
		concepts.add(conceptOne);
		
		fsnTwo.put("term", "TwoA");
		fsnTwo.remove("descriptionId");
		concepts.add(conceptTwo);

		// Post updates
		final IBranchPath branchPath = createMainPath();
		final String bulkId = assertConceptsUpdateStartsWithStatus(branchPath, concepts, 201);
		
		// Wait for completion
		assertConceptsBulkJobCompletes(branchPath, bulkId);
		
		// Load concepts and assert updated
		Assert.assertEquals("OneA", getFsn(SnomedBrowserApiAssert.getConcept(branchPath, (String) conceptOne.get("conceptId"))).get("term"));
		Assert.assertEquals("TwoA", getFsn(SnomedBrowserApiAssert.getConcept(branchPath, (String) conceptTwo.get("conceptId"))).get("term"));
	}
	
	@Test
	public void getConceptChildren() throws IOException {
		givenBranchWithPath(testBranchPath);
		
		// Create child of Finding context
		createConcept(testBranchPath, "Special finding context (attribute)", "Special finding context", FINDING_CONTEXT);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().header("Accept-Language", "en-US;q=0.8,en-GB;q=0.6")
				.when().get("/browser/{path}/concepts/{conceptId}/children?form=stated&preferredDescriptionType=FSN", testBranchPath.getPath(), FINDING_CONTEXT)
				.then().assertThat().statusCode(200)
				.and().body("size()", equalTo(1))
				.and().body("[0].fsn", equalTo("Special finding context (attribute)"));
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().header("Accept-Language", "en-US;q=0.8,en-GB;q=0.6")
				.when().get("/browser/{path}/concepts/{conceptId}/children?form=stated&preferredDescriptionType=SYNONYM", testBranchPath.getPath(), FINDING_CONTEXT)
				.then().assertThat().statusCode(200)
				.and().body("size()", equalTo(1))
				.and().body("[0].preferredSynonym", equalTo("Special finding context"));
	}
	
	@Test
	public void searchDescriptions() throws IOException {
		givenBranchWithPath(testBranchPath);
		
		// Create child of Finding context with unique PT and FSN
		createConcept(testBranchPath, "Visotactile finding context (attribute)", "Circulatory finding context", FINDING_CONTEXT);
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().header("Accept-Language", "en-US;q=0.8,en-GB;q=0.6")
				.when().get("/browser/{path}/descriptions?query={query}&preferredDescriptionType=FSN", testBranchPath.getPath(), "visotactile")
				.then().assertThat().statusCode(200)
				.and().body("size()", equalTo(1))
				.and().body("[0].concept.fsn", equalTo("Visotactile finding context (attribute)"));
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().header("Accept-Language", "en-US;q=0.8,en-GB;q=0.6")
				.when().get("/browser/{path}/descriptions?query={query}&preferredDescriptionType=SYNONYM", testBranchPath.getPath(), "circulatory")
				.then().assertThat().statusCode(200)
				.and().body("size()", equalTo(1))
				.and().body("[0].concept.preferredSynonym", equalTo("Circulatory finding context"));
	}

	private Map<String, Object> getFsn(final Map<String, Object> conceptOne) {
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> descs = (List<Map<String, Object>>) conceptOne.get("descriptions");
		for (Map<String, Object> desc : descs) {
			if ("FSN".equals(desc.get("type"))) {
				return desc;
			}
		}
		return null;
	}

	private Map<String, Object> createConceptWithFsn(String fsn) throws IOException {
		return createConcept(createMainPath(), fsn, fsn, ROOT_CONCEPT);
	}
	
	private Map<String, Object> createConcept(IBranchPath branchPath, String fsn, String pt, String parentId) throws IOException {
		Date creationDate = new Date();
		final ImmutableList<?> descriptions = createDescriptions(fsn, pt, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(parentId, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, pt, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		final ExtractableResponse<Response> extract = assertComponentCreatedWithStatus(branchPath, requestBody, 200).extract();
		return objectMapper.readValue(extract.body().asString(), new TypeReference<Map<String, Object>>(){});
	}
	
	private ValidatableResponse createConcept(final String conceptId) {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(conceptId, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		return assertComponentCreatedWithStatus(createMainPath(), requestBody, 200);
	}
	
	@SuppressWarnings("unchecked")
	private static List<Object> getListElement(Map<String, Object> concept, String elementName) {
		Object listElement = concept.get(elementName);

		if (listElement instanceof List<?>) {
			return (List<Object>) listElement;
		} else {
			return null;
		}
	}

	private static List<?> createNewDescriptions(int quantity) {
		List<Map<String, Object>> results = newArrayList();

		for (int i = 0; i < quantity; i++) {

			Map<String, Object> description = ImmutableMap.<String, Object>builder()
					.put("active", true)
					.put("term", String.format("New extra Synonym %s", i))
					.put("type", SnomedBrowserDescriptionType.SYNONYM)
					.put("lang", "en")
					.put("moduleId", MODULE_SCT_CORE)
					.put("caseSignificance", CaseSignificance.CASE_INSENSITIVE)
					.put("acceptabilityMap", SnomedApiTestConstants.UK_ACCEPTABLE_MAP)
					.build();

			results.add(description);
		}

		return results;
	}

	private static List<?> createNewRelationships(int quantity, String sourceId) {
		List<Map<String, Object>> results = newArrayList();

		for (int i = 0; i < quantity; i++) {

			Map<?, ?> type = ImmutableMap.<String, Object>builder()
					.put("conceptId", Concepts.PART_OF)
					.put("fsn", "Part of (attribute)")
					.build();

			Map<?, ?> target = ImmutableMap.<String, Object>builder()
					.put("active", true)
					.put("moduleId", MODULE_SCT_CORE)
					.put("conceptId", Concepts.NAMESPACE_ROOT)
					.put("fsn", "Destination of new relationship")
					.put("definitionStatus", DefinitionStatus.PRIMITIVE)
					.build();

			Map<String, Object> relationship = ImmutableMap.<String, Object>builder()
					.put("sourceId", sourceId)
					.put("modifier", RelationshipModifier.EXISTENTIAL)
					.put("groupId", "0")
					.put("characteristicType", CharacteristicType.ADDITIONAL_RELATIONSHIP)
					.put("active", true)
					.put("type", type)
					.put("moduleId", MODULE_SCT_CORE)
					.put("target", target)
					.build();

			results.add(relationship);
		}

		return results;
	}

	@Test
	public void createConceptNonExistentBranch() {
		createBrowserConcept(BranchPathUtils.createPath("MAIN/x/y/z"), createBrowserConceptRequest()).statusCode(404);
	}

	@Test
	public void createConceptWithoutParent() {
		Map<?, ?> conceptRequest = newHashMap(createBrowserConceptRequest());
		conceptRequest.remove("relationships");

		createBrowserConcept(branchPath, conceptRequest).statusCode(400)
		.body("message", equalTo("1 validation error"));
	}

	@Test
	public void createConceptWithNonexistentParent() {
		String conceptId = createNewConcept(branchPath);

		deleteComponent(branchPath, SnomedComponentType.CONCEPT, conceptId, false).statusCode(204);

		Map<String, Object> conceptRequest = newHashMap(createBrowserConceptRequest());
		conceptRequest.put("relationships", createIsaRelationship(conceptId));

		createBrowserConcept(branchPath, conceptRequest).statusCode(400);
	}

	@Test
	public void createRegularConcept() {
		createBrowserConcept(branchPath, createBrowserConceptRequest()).statusCode(200);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createConceptWithReservedId() {
		String expectedConceptId = reserveComponentId(null, ComponentCategory.CONCEPT);

		Map<String, Object> conceptRequest = newHashMap(createBrowserConceptRequest());
		conceptRequest.put("conceptId", expectedConceptId);

		Map<String, Object> conceptResponse = createBrowserConcept(branchPath, conceptRequest).statusCode(200)
				.extract().as(Map.class);

		String actualConceptId = (String) conceptResponse.get("conceptId");
		assertEquals(expectedConceptId, actualConceptId);
	}

	@Test
	public void createConceptOnDeletedBranch() {
		deleteBranch(branchPath).statusCode(204);
		createBrowserConcept(branchPath, createBrowserConceptRequest()).statusCode(400);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void inactivateConcept() throws Exception {
		Map<String, Object> conceptRequest = createBrowserConcept(branchPath, createBrowserConceptRequest()).statusCode(200)
				.extract().as(Map.class);

		String conceptId = (String) conceptRequest.get("conceptId");
		conceptRequest.put("active", false);
		updateBrowserConcept(branchPath, conceptId, conceptRequest).statusCode(200);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void removeAllRelationshipsFromConcept() throws Exception {
		Map<String, Object> conceptRequest = createBrowserConcept(branchPath, createBrowserConceptRequest()).statusCode(200)
				.extract().as(Map.class);

		String conceptId = (String) conceptRequest.get("conceptId");
		conceptRequest.remove("relationships");
		updateBrowserConcept(branchPath, conceptId, conceptRequest).statusCode(200);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateConceptWithNewComponents() {
		Map<String, Object> conceptRequest = createBrowserConcept(branchPath, createBrowserConceptRequest()).statusCode(200)
				.extract().as(Map.class);

		String conceptId = (String) conceptRequest.get("conceptId");

		List<Object> descriptionsList = getListElement(conceptRequest, "descriptions");
		descriptionsList.addAll(createNewDescriptions(5));

		List<Object> relationshipsList = getListElement(conceptRequest, "relationships");
		relationshipsList.addAll(createNewRelationships(5, conceptId));

		Map<String, Object> updatedConcept = updateBrowserConcept(branchPath, conceptId, conceptRequest).statusCode(200)
				.extract().as(Map.class);

		List<Object> updatedDescriptions = getListElement(updatedConcept, "descriptions");
		assertEquals(7, updatedDescriptions.size());

		List<Object> updatedRelationships = getListElement(updatedConcept, "relationships");
		assertEquals(6, updatedRelationships.size());
	}
}
