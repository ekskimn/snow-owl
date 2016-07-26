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
package com.b2international.snowowl.snomed.api.rest.browser;

import static com.b2international.snowowl.datastore.BranchPathUtils.createMainPath;
import static com.b2international.snowowl.datastore.BranchPathUtils.createPath;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenCreatingVersion;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenDeletingBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyIsNull;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentCreatedWithStatus;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentNotCreated;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentUpdatedWithStatus;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertConceptsUpdateStartsWithStatus;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertConceptsBulkJobCompletes;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.createDescriptions;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.createIsaRelationship;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.generateComponentId;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.givenConceptRequestBody;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.jayway.restassured.response.ExtractableResponse;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

/**
 * @since 4.5
 */
public class SnomedBrowserApiTest extends AbstractSnomedApiTest {

	private static final String FINDING_CONTEXT = "408729009";
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Test
	public void createConceptNonExistentBranch() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createPath("MAIN/1998-01-31"), requestBody, 404).and().body("status", equalTo(404));
	}

	@Test
	public void createConceptWithoutParent() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, null, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 400).and().body("message",
				equalTo("At least one isA relationship is required."));
	}

	@Test
	public void createConceptWithNonexistentParent() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(null, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentNotCreated(createMainPath(), requestBody);
	}

	@Test
	public void createConcept() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 200);
	}

	@Test
	public void createConceptWithGeneratedId() {
		final String conceptId = generateComponentId(null, ComponentCategory.CONCEPT);

		createConcept(conceptId);
	}

	@Test
	public void createConceptOnBranch() {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 200);
	}
	
	@Test
	public void createConceptOnDeletedBranch() {
		givenBranchWithPath(testBranchPath);
		whenDeletingBranchWithPath(testBranchPath);

		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(testBranchPath, requestBody, 400);
	}

	@Test
	public void inactivateConcept() throws Exception {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<String, Object> createRequestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships,
				creationDate);
		final ValidatableResponse response = assertComponentCreatedWithStatus(createMainPath(), createRequestBody, 200);

		final Map<String, Object> concept = response.and().extract().jsonPath().get();
		concept.put("active", false);

		assertComponentUpdatedWithStatus(createMainPath(), concept.get("conceptId").toString(), concept, 200);
	}

	@Test
	public void removeAllRelationshipsFromConcept() throws Exception {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<String, Object> createRequestBody = givenConceptRequestBody(null, true, fsn, MODULE_SCT_CORE, descriptions, relationships,
				creationDate);
		final ValidatableResponse response = assertComponentCreatedWithStatus(createMainPath(), createRequestBody, 200);

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
}
