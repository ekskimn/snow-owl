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

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.IS_A;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static org.hamcrest.CoreMatchers.equalTo;

import java.security.acl.LastOwnerException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.api.domain.browser.SnomedBrowserDescriptionType;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.core.domain.RelationshipModifier;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierService;
import com.b2international.snowowl.test.commons.rest.RestExtensions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

/**
 * @since 4.5
 */
public class SnomedBrowserApiAssert {
	
	public static ValidatableResponse assertComponentCreatedWithStatus(final IBranchPath branchPath, 
			final Map<?, ?> requestBody, 
			final int statusCode) {
		return whenCreatingComponent(branchPath, requestBody)
				.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}
	
	private static Response whenCreatingComponent(final IBranchPath branchPath, 
			final Map<?, ?> requestBody) {

		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.and().body(requestBody)
				.when().post("/browser/{path}/concepts", branchPath.getPath());
	}
	
	public static ValidatableResponse assertComponentNotCreated(final IBranchPath branchPath, 
			final Map<?, ?> requestBody) {

		return assertComponentCreatedWithStatus(branchPath, requestBody, 400)
				.and().body("status", equalTo(400));
	}
	
	public static ValidatableResponse assertComponentUpdatedWithStatus(final IBranchPath branchPath, 
			final String conceptId,
			final Map<?, ?> requestBody, 
			final int statusCode) {
		return whenUpdatingComponent(branchPath, conceptId, requestBody)
				.then().log().ifValidationFails().assertThat().statusCode(statusCode);
	}
	
	private static Response whenUpdatingComponent(final IBranchPath branchPath, 
			final String conceptId,
			final Map<?, ?> requestBody) {

		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.and().body(requestBody)
				.when().put("/browser/{path}/concepts/{conceptId}", branchPath.getPath(), conceptId);
	}
	
	public static String assertConceptsUpdateStartsWithStatus(final IBranchPath branchPath, 
			final List<Map<String, Object>> requestBody, 
			final int statusCode) {
		final String location = givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
					.with().contentType(ContentType.JSON)
					.and().body(requestBody)
					.when().post("/browser/{path}/concepts/bulk", branchPath.getPath())
					.then().log().ifValidationFails().assertThat().statusCode(statusCode).extract().header("Location");
		return RestExtensions.lastPathSegment(location);
	}

	public static void assertConceptsBulkJobCompletes(IBranchPath branchPath, String bulkId) throws InterruptedException {
		final GregorianCalendar endCal = new GregorianCalendar();
		endCal.add(Calendar.MINUTE, 1);
		final Date end = endCal.getTime();
		while (end.after(new Date())) {
			final String status = givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
					.with().contentType(ContentType.JSON)
					.when().get("/browser/{path}/concepts/bulk/{bulkId}", branchPath.getPath(), bulkId)
					.then().extract().jsonPath().getString("status");
			if (!"RUNNING".equals(status)) {
				Assert.assertEquals("COMPLETED", status);
				return;
			}
			Thread.sleep(500);
		}
	}
	
	public static Map<String, Object> givenConceptRequestBody(final String conceptId, final boolean active, final String fsn,  
			final String moduleId, final ImmutableList<?> descriptions, final ImmutableList<?> relationships, final Date creationDate) {
	
		return givenConceptRequestBody(conceptId, active, fsn, fsn, moduleId, descriptions, relationships, creationDate);
	}
	
	public static Map<String, Object> givenConceptRequestBody(final String conceptId, final boolean active, final String fsn, final String pt, 
			final String moduleId, final ImmutableList<?> descriptions, final ImmutableList<?> relationships, final Date creationDate) {
		
		final ImmutableMap.Builder<String, Object> conceptBuilder = ImmutableMap.<String, Object>builder()
				.put("fsn", fsn)
				.put("preferredSynonym", pt)
				.put("moduleId", moduleId)
				.put("isLeafInferred", "")
				.put("isLeafStated", "")
				.put("definitionStatus", DefinitionStatus.PRIMITIVE);
		
		if (null != conceptId)
			conceptBuilder.put("conceptId", conceptId);
		
		if (null == descriptions)
			conceptBuilder.put("descriptions", Lists.newArrayList());
		else
			conceptBuilder.put("descriptions", descriptions);
		
		if (null == relationships)
			conceptBuilder.put("relationships", Lists.newArrayList());
		else
			conceptBuilder.put("relationships", relationships);
		
		return conceptBuilder.build();
	}
	
	public static ImmutableList<?> createDescriptions(final String fsn, final String moduleId, final Map<?, ?> fsnAcceptabilityMap,
			final Date creationDate) {
		
		return createDescriptions(fsn, "New PT at " + creationDate, moduleId, fsnAcceptabilityMap, creationDate);
	}

	public static ImmutableList<?> createDescriptions(final String fsn, String pt, final String moduleId, final Map<?, ?> fsnAcceptabilityMap, 
			final Date creationDate) {
		
		final Map<?, ?> fsnDescription = ImmutableMap.<String, Object>builder()
				.put("descriptionId", generateComponentId(null, ComponentCategory.DESCRIPTION))
				.put("effectiveTime", creationDate)
				.put("conceptId", "")
				.put("active", true)
				.put("term", fsn)
				.put("type", SnomedBrowserDescriptionType.FSN)
				.put("lang", "en")
				.put("moduleId", moduleId)
				.put("caseSignificance", CaseSignificance.CASE_INSENSITIVE)
				.put("acceptabilityMap", fsnAcceptabilityMap)
				.build();

		final Map<?, ?> ptDescription = ImmutableMap.<String, Object>builder()
				.put("descriptionId", generateComponentId(null, ComponentCategory.DESCRIPTION))
				.put("effectiveTime", creationDate)
				.put("conceptId", "")
				.put("active", true)
				.put("term", pt)
				.put("type", SnomedBrowserDescriptionType.SYNONYM)
				.put("lang", "en")
				.put("moduleId", moduleId)
				.put("caseSignificance", CaseSignificance.CASE_INSENSITIVE)
				.put("acceptabilityMap", PREFERRED_ACCEPTABILITY_MAP)
				.build();
		
		return ImmutableList.of(fsnDescription, ptDescription);
	}
	
	public static ImmutableList<?> createIsaRelationship(final String parentId, final String moduleId, final Date creationDate) {
		final Map<?, ?> type = ImmutableMap.<String, Object>builder()
				.put("conceptId", IS_A)
				.put("fsn", "Is a (attribute)")
				.build();
		
		final Map<?, ?> target = ImmutableMap.<String, Object>builder()
				.put("effectiveTime", creationDate)
				.put("active", true)
				.put("moduleId", moduleId)
				.put("conceptId", null == parentId ? generateComponentId(null, ComponentCategory.CONCEPT) : parentId)
				.put("fsn", "")
				.put("definitionStatus", DefinitionStatus.PRIMITIVE)
				.build();
		
		final Map<?, ?> isaRelationship = ImmutableMap.<String, Object>builder()
				.put("sourceId", "")
				.put("effectiveTime", creationDate)
				.put("modifier", RelationshipModifier.UNIVERSAL)
				.put("groupId", "0")
				.put("characteristicType", CharacteristicType.STATED_RELATIONSHIP)
				.put("active", true)
				.put("type", type)
				.put("relationshipId", generateComponentId(null, ComponentCategory.RELATIONSHIP))
				.put("moduleId", moduleId)
				.put("target", target)
				.build();

		return ImmutableList.of(isaRelationship);
	}
	
	public static String generateComponentId(final String namespace, final ComponentCategory category) {
		final ISnomedIdentifierService identifierService = ApplicationContext.getInstance().getService(ISnomedIdentifierService.class);
		return identifierService.reserve(namespace, category);
	}
	
	public static Map<String, Object> getConcept(final IBranchPath branchPath, final String conceptId) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.when().get("/browser/{path}/concepts/{conceptId}", branchPath.getPath(), conceptId)
				.then().extract().jsonPath().get();
	}
	
}
