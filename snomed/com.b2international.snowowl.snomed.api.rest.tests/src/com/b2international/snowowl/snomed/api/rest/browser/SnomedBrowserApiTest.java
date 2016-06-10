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
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenDeletingBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenCreatingVersion;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyIsNull;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentCreatedWithStatus;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentNotCreated;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.assertComponentUpdatedWithStatus;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.createDescriptions;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.createIsaRelationship;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.generateComponentId;
import static com.b2international.snowowl.snomed.api.rest.browser.SnomedBrowserApiAssert.givenConceptRequestBody;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.google.common.collect.ImmutableList;
import com.jayway.restassured.response.ValidatableResponse;

/**
 * @since 4.5
 */
public class SnomedBrowserApiTest extends AbstractSnomedApiTest {

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

		// We get a 400, bad request, because at least one is-a relationship is required
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

//		FIXME: Creating a version is failing with a strange error unless this is the only unit test running. Looking for help with this.
//		final String effectiveDate = "20160501";
//		whenCreatingVersion("2016-05-01", effectiveDate).then().assertThat().statusCode(201);
//		
//		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "effectiveTime", effectiveDate);
//		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "released", true);
//		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "descriptions.released[0]", true);
//		assertConceptIndexedBrowserPropertyEquals(path, conceptId, "relationships.released[0]", true);
	}

	private void createConcept(final String conceptId) {
		final Date creationDate = new Date();
		final String fsn = "New FSN at " + creationDate;
		final ImmutableList<?> descriptions = createDescriptions(fsn, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, creationDate);
		final ImmutableList<?> relationships = createIsaRelationship(ROOT_CONCEPT, MODULE_SCT_CORE, creationDate);
		final Map<?, ?> requestBody = givenConceptRequestBody(conceptId, true, fsn, MODULE_SCT_CORE, descriptions, relationships, creationDate);
		assertComponentCreatedWithStatus(createMainPath(), requestBody, 200);
	}

}
