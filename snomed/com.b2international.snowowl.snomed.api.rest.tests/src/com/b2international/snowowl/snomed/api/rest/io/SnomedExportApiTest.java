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
package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.datastore.BranchPathUtils.createMainPath;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.FULLY_SPECIFIED_NAME;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.SYNONYM;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.ACCEPTABLE_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenCreatingVersion;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanBeUpdated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentHasProperty;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertDescriptionExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.createRefSetMemberRequestBody;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenConceptRequestBody;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenRelationshipRequestBody;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.b2international.commons.Pair;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.api.rest.SnomedVersioningApiAssert;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.jayway.restassured.http.ContentType;

/**
 * @since 5.4
 */
public class SnomedExportApiTest extends AbstractSnomedExportApiTest {
	
	@Test
	public void createValidExportConfiguration() {
		final Map<?, ?> config = ImmutableMap.builder()
			.put("type", "DELTA")
			.put("branchPath", "MAIN")
			.build();
		
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo("MAIN"));
	}
	
	@Test
	public void createInvalidExportConfiguration() {
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.build();
			
		assertExportConfigurationFails(config);
	}
	
	@Test
	public void exportUnpublishedDeltaRelationships() throws Exception {
		
		assertNewVersionCreated();
		
		final String statedRelationshipId = createStatedRelationshipOnMain();
		final String inferredRelationshipId = createInferredRelationshipOnMain();
		final String additionalRelationshipId = createAdditionalRelationshipOnMain();

		final String transientEffectiveTime = "20170131";
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", "MAIN")
				.put("transientEffectiveTime", transientEffectiveTime)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo("MAIN"))
			.and().body("transientEffectiveTime", equalTo(transientEffectiveTime));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		final String statedLine = getComponentLine(ImmutableList.<String> of(statedRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.STATED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		final String inferredLine = getComponentLine(ImmutableList.<String> of(inferredRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.INFERRED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		final String additionalLine = getComponentLine(ImmutableList.<String> of(additionalRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, BLEEDING,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.ADDITIONAL_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
		
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(true, statedLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, inferredLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, additionalLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}

	@Test
	public void exportDeltaInDateRangeFromVersionBranch() throws Exception {
		
		assertNewVersionCreated();
		
		final String statedRelationshipId = createStatedRelationshipOnMain();
		final String inferredRelationshipId = createInferredRelationshipOnMain();
		final String additionalRelationshipId = createAdditionalRelationshipOnMain();
		
		final Date newVersionDate = assertNewVersionCreated();
		
		final String versionName = Dates.formatByGmt(newVersionDate);
		final String versionEffectiveDate = Dates.formatByGmt(newVersionDate, DateFormats.SHORT);
		final String versionPath = String.format("%s%s%s", Branch.MAIN_PATH, Branch.SEPARATOR, versionName);
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", versionPath)
				.put("startEffectiveTime", versionEffectiveDate)
				.put("endEffectiveTime", versionEffectiveDate)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(versionPath))
			.and().body("startEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("endEffectiveTime", equalTo(versionEffectiveDate));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String statedLine = getComponentLine(ImmutableList.<String> of(statedRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.STATED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String inferredLine = getComponentLine(ImmutableList.<String> of(inferredRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.INFERRED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String additionalLine = getComponentLine(ImmutableList.<String> of(additionalRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, BLEEDING,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.ADDITIONAL_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
		
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(true, statedLine));
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(false, inferredLine));
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(false, additionalLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, inferredLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, additionalLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(false, statedLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportDeltaInDateRangeAndUnpublishedComponents() throws Exception {
		
		// create new version
		assertNewVersionCreated();
		
		final String statedRelationshipId = createStatedRelationshipOnMain();
		final String inferredRelationshipId = createInferredRelationshipOnMain();
		final String additionalRelationshipId = createAdditionalRelationshipOnMain();
		
		// version new relationships
		Date relationshipVersionDate = assertNewVersionCreated();
		
		// create new concept
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptRequestBody);
		
		// version new concept
		Date conceptVersionDate = assertNewVersionCreated();
		String conceptEffectiveDate = Dates.formatByGmt(conceptVersionDate, DateFormats.SHORT);

		// create new description
		String descriptionTerm = "Exported Description";
		Map<?, ?> descriptionRequestBody = ImmutableMap.builder()
			.put("conceptId", conceptId)
			.put("moduleId", MODULE_SCT_CORE)
			.put("typeId", Concepts.SYNONYM)
			.put("term", descriptionTerm)
			.put("languageCode", "en")
			.put("acceptability", ACCEPTABLE_ACCEPTABILITY_MAP)
			.put("commitComment", "new description")
			.build();
		
		String descriptionId = assertComponentCreated(createMainPath(), SnomedComponentType.DESCRIPTION, descriptionRequestBody);
		
		// do not version description
		
		String versionEffectiveDate = Dates.formatByGmt(relationshipVersionDate, DateFormats.SHORT);
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", Branch.MAIN_PATH)
				.put("startEffectiveTime", versionEffectiveDate)
				.put("endEffectiveTime", versionEffectiveDate)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(Branch.MAIN_PATH))
			.and().body("startEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("endEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("includeUnpublished", equalTo(true));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String statedLine = getComponentLine(ImmutableList.<String> of(statedRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.STATED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String inferredLine = getComponentLine(ImmutableList.<String> of(inferredRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.INFERRED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String additionalLine = getComponentLine(ImmutableList.<String> of(additionalRelationshipId, versionEffectiveDate, "1", MODULE_SCT_CORE, BLEEDING,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.ADDITIONAL_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		
		String conceptLine = getComponentLine(ImmutableList.<String> of(conceptId, conceptEffectiveDate, "1", MODULE_SCT_CORE, 
				DefinitionStatus.PRIMITIVE.getConceptId()));

		String descriptionLine = getComponentLine(ImmutableList.<String> of(descriptionId, "", "1", MODULE_SCT_CORE, conceptId, "en",
				Concepts.SYNONYM, descriptionTerm, CaseSignificance.INITIAL_CHARACTER_CASE_INSENSITIVE.getConceptId()));

		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
		
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(true, statedLine));
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(false, inferredLine));
		fileToLinesMap.put("sct2_StatedRelationship", Pair.of(false, additionalLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, inferredLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(true, additionalLine));
		fileToLinesMap.put("sct2_Relationship", Pair.of(false, statedLine));
		
		fileToLinesMap.put("sct2_Concept", Pair.of(false, conceptLine));
		fileToLinesMap.put("sct2_Description", Pair.of(true, descriptionLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportContentFromVersionBranchFixerTask() throws Exception {
		
		assertNewVersionCreated();
		
		// create concept ref. component
		final Map<?, ?> conceptReq = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String createdConceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptReq);
		
		// create refset
		String refsetName = "ExampleRefset";
		
		final Map<String, Object> fsnDescription = ImmutableMap.<String, Object>builder()
				.put("typeId", FULLY_SPECIFIED_NAME)
				.put("term", refsetName + " (qualifier)")
				.put("languageCode", "en")
				.put("acceptability", PREFERRED_ACCEPTABILITY_MAP)
				.build();
		
		final Map<String, Object> ptDescription = ImmutableMap.<String, Object>builder()
				.put("typeId", SYNONYM)
				.put("term", refsetName)
				.put("languageCode", "en")
				.put("acceptability", PREFERRED_ACCEPTABILITY_MAP)
				.build();

		final Map<String, Object> conceptBuilder = ImmutableMap.<String, Object>builder()
				.put("moduleId", MODULE_SCT_CORE)
				.put("descriptions", ImmutableList.of(fsnDescription, ptDescription))
				.put("parentId", Concepts.REFSET_SIMPLE_TYPE)
				.build();
		
		final Map<String, Object> refSetReq = ImmutableMap.<String, Object>builder()
				.putAll(conceptBuilder)
				.put("commitComment", String.format("New %s type reference set with %s members", SnomedRefSetType.SIMPLE, SnomedTerminologyComponentConstants.CONCEPT))
				.put("type", SnomedRefSetType.SIMPLE)
				.put("referencedComponentType", SnomedTerminologyComponentConstants.CONCEPT)
				.build();
		
		final String createdRefSetId = assertComponentCreated(createMainPath(), SnomedComponentType.REFSET, refSetReq);
		assertComponentExists(createMainPath(), SnomedComponentType.REFSET, createdRefSetId);
		
		// create member
		final Map<String, Object> memberReq = createRefSetMemberRequestBody(createdConceptId, createdRefSetId);
		String memberId = assertComponentCreated(createMainPath(), SnomedComponentType.MEMBER, memberReq);
		
		final Date dateForNewVersion = getDateForNewVersion("SNOMEDCT");
		
		String versionName = Dates.formatByGmt(dateForNewVersion);
		String versionEffectiveDate = Dates.formatByGmt(dateForNewVersion, DateFormats.SHORT);
		
		whenCreatingVersion(versionName, versionEffectiveDate)
			.then().assertThat().statusCode(201);
		
		givenAuthenticatedRequest(ADMIN_API)
			.when().get("/codesystems/SNOMEDCT/versions/{id}", versionName)
			.then().assertThat().statusCode(200);
		
		IBranchPath versionPath = BranchPathUtils.createPath(BranchPathUtils.createMainPath(), versionName);
		String testBranchName = "Fix01";
		IBranchPath taskBranch = BranchPathUtils.createPath(versionPath, testBranchName);
		
		// create fixer branch for version branch
		SnomedBranchingApiAssert.givenBranchWithPath(taskBranch);
		
		// change an existing component
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateForNewVersion);
		calendar.add(Calendar.DATE, 1);
		
		String newEffectiveTime = Dates.formatByGmt(calendar.getTime(), DateFormats.SHORT);
		
		updateMemberEffectiveTime(taskBranch, memberId, newEffectiveTime, true);
		
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "effectiveTime", newEffectiveTime);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "released", true);
		
		// add a new component with the same effective time as the version branch
		
		final Map<String, Object> newMemberReq = createRefSetMemberRequestBody(createdConceptId, createdRefSetId);
		String newMemberId = assertComponentCreated(taskBranch, SnomedComponentType.MEMBER, newMemberReq);
		
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "effectiveTime", null);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "released", false);
		
		updateMemberEffectiveTime(taskBranch, newMemberId, versionEffectiveDate, true);
		
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "effectiveTime", versionEffectiveDate);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "released", true);
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "SNAPSHOT")
				.put("branchPath", taskBranch.getPath())
				.put("startEffectiveTime", versionEffectiveDate)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("SNAPSHOT"))
			.and().body("branchPath", equalTo(taskBranch.getPath()))
			.and().body("startEffectiveTime", equalTo(versionEffectiveDate));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String refsetMemberLine = getComponentLine(ImmutableList.<String>of(memberId, newEffectiveTime, "1", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		String invalidRefsetMemberLine = getComponentLine(ImmutableList.<String>of(memberId, versionEffectiveDate, "1", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		
		String newRefsetMemberLine = getComponentLine(ImmutableList.<String>of(newMemberId, versionEffectiveDate, "1", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
		
		String refsetFileName = "der2_Refset_ExampleRefsetSnapshot";
		
		fileToLinesMap.put(refsetFileName, Pair.of(true, refsetMemberLine));
		fileToLinesMap.put(refsetFileName, Pair.of(true, newRefsetMemberLine));
		fileToLinesMap.put(refsetFileName, Pair.of(false, invalidRefsetMemberLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportContentFromVersionBranchFixerTaskWithTransientEffectiveTime() throws Exception {
		
		assertNewVersionCreated();
		
		// create concept ref. component
		final Map<?, ?> conceptReq = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		final String createdConceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptReq);
		
		// create refset
		String refsetName = "TransientExampleRefset";
		
		final Map<String, Object> fsnDescription = ImmutableMap.<String, Object>builder()
				.put("typeId", FULLY_SPECIFIED_NAME)
				.put("term", refsetName + " (qualifier)")
				.put("languageCode", "en")
				.put("acceptability", PREFERRED_ACCEPTABILITY_MAP)
				.build();
		
		final Map<String, Object> ptDescription = ImmutableMap.<String, Object>builder()
				.put("typeId", SYNONYM)
				.put("term", refsetName)
				.put("languageCode", "en")
				.put("acceptability", PREFERRED_ACCEPTABILITY_MAP)
				.build();

		final Map<String, Object> conceptBuilder = ImmutableMap.<String, Object>builder()
				.put("moduleId", MODULE_SCT_CORE)
				.put("descriptions", ImmutableList.of(fsnDescription, ptDescription))
				.put("parentId", Concepts.REFSET_SIMPLE_TYPE)
				.build();
		
		final Map<String, Object> refSetReq = ImmutableMap.<String, Object>builder()
				.putAll(conceptBuilder)
				.put("commitComment", String.format("New %s type reference set with %s members", SnomedRefSetType.SIMPLE, SnomedTerminologyComponentConstants.CONCEPT))
				.put("type", SnomedRefSetType.SIMPLE)
				.put("referencedComponentType", SnomedTerminologyComponentConstants.CONCEPT)
				.build();
		
		final String createdRefSetId = assertComponentCreated(createMainPath(), SnomedComponentType.REFSET, refSetReq);
		assertComponentExists(createMainPath(), SnomedComponentType.REFSET, createdRefSetId);
		
		// create member
		final Map<String, Object> memberReq = createRefSetMemberRequestBody(createdConceptId, createdRefSetId);
		String memberId = assertComponentCreated(createMainPath(), SnomedComponentType.MEMBER, memberReq);
		
		final Date dateForNewVersion = getDateForNewVersion("SNOMEDCT");
		
		String versionName = Dates.formatByGmt(dateForNewVersion);
		String versionEffectiveDate = Dates.formatByGmt(dateForNewVersion, DateFormats.SHORT);
		
		whenCreatingVersion(versionName, versionEffectiveDate)
			.then().assertThat().statusCode(201);
		
		givenAuthenticatedRequest(ADMIN_API)
			.when().get("/codesystems/SNOMEDCT/versions/{id}", versionName)
			.then().assertThat().statusCode(200);
		
		IBranchPath versionPath = BranchPathUtils.createPath(BranchPathUtils.createMainPath(), versionName);
		String testBranchName = "Fix01";
		IBranchPath taskBranch = BranchPathUtils.createPath(versionPath, testBranchName);
		
		// create fixer branch for version branch
		SnomedBranchingApiAssert.givenBranchWithPath(taskBranch);
		
		// change an existing component

		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "active", true);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "effectiveTime", versionEffectiveDate);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "released", true);
		
		Map<String, Object> memberUpdateRequest = ImmutableMap.<String, Object>builder()
				.put("active", false)
				.put("commitComment", "Inactivate member")
				.build();
		
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(memberUpdateRequest)
			.when().put("/{path}/{componentType}/{id}?force=false", taskBranch.getPath(), SnomedComponentType.MEMBER.toLowerCasePlural(), memberId)
			.then().log().ifValidationFails()
			.statusCode(204);
		
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "active", false);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "effectiveTime", null);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, memberId, "released", true);
		
		// add a new component
		
		final Map<String, Object> newMemberReq = createRefSetMemberRequestBody(createdConceptId, createdRefSetId);
		String newMemberId = assertComponentCreated(taskBranch, SnomedComponentType.MEMBER, newMemberReq);
		
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "effectiveTime", null);
		assertComponentHasProperty(taskBranch, SnomedComponentType.MEMBER, newMemberId, "released", false);
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "SNAPSHOT")
				.put("branchPath", taskBranch.getPath())
				.put("startEffectiveTime", versionEffectiveDate)
				.put("transientEffectiveTime", versionEffectiveDate)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("SNAPSHOT"))
			.and().body("branchPath", equalTo(taskBranch.getPath()))
			.and().body("startEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("transientEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("includeUnpublished", equalTo(true));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String refsetMemberLine = getComponentLine(ImmutableList.<String>of(memberId, versionEffectiveDate, "0", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		String invalidRefsetMemberLine = getComponentLine(ImmutableList.<String>of(memberId, versionEffectiveDate, "1", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		
		String newRefsetMemberLine = getComponentLine(ImmutableList.<String>of(newMemberId, versionEffectiveDate, "1", MODULE_SCT_CORE, createdRefSetId, createdConceptId));
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
		
		String refsetFileName = "der2_Refset_TransientExampleRefset";
		
		fileToLinesMap.put(refsetFileName, Pair.of(true, refsetMemberLine));
		fileToLinesMap.put(refsetFileName, Pair.of(true, newRefsetMemberLine));
		fileToLinesMap.put(refsetFileName, Pair.of(false, invalidRefsetMemberLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportPublishedAndUnpublishedTextDefinitionsIntoSeparateFile() throws Exception {
		
		// create new version
		assertNewVersionCreated();
		
		// create new concept
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptRequestBody);

		// create new text definition
		String textDefinitionTerm = "Text Definition with effective time";
		Map<?, ?> textDefinitionRequestBody = ImmutableMap.builder()
			.put("conceptId", conceptId)
			.put("moduleId", MODULE_SCT_CORE)
			.put("typeId", Concepts.TEXT_DEFINITION)
			.put("term", textDefinitionTerm)
			.put("languageCode", "en")
			.put("acceptability", ACCEPTABLE_ACCEPTABILITY_MAP)
			.put("commitComment", "new text definition")
			.build();
		
		String textDefinitionId = assertComponentCreated(createMainPath(), SnomedComponentType.DESCRIPTION, textDefinitionRequestBody);

		// version new concept
		Date conceptVersionDate = assertNewVersionCreated();
		String conceptEffectiveDate = Dates.formatByGmt(conceptVersionDate, DateFormats.SHORT);

		// create new text definition
		String unpublishedTextDefinitionTerm = "Text Definition without effective time";
		Map<?, ?> unpublishedTextDefinitionRequestBody = ImmutableMap.builder()
			.put("conceptId", conceptId)
			.put("moduleId", MODULE_SCT_CORE)
			.put("typeId", Concepts.TEXT_DEFINITION)
			.put("term", unpublishedTextDefinitionTerm)
			.put("languageCode", "en")
			.put("acceptability", ACCEPTABLE_ACCEPTABILITY_MAP)
			.put("commitComment", "new text definition")
			.build();
		
		String unpublishedTextDefinitionId = assertComponentCreated(createMainPath(), SnomedComponentType.DESCRIPTION, unpublishedTextDefinitionRequestBody);
		
		// do not create new version
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", Branch.MAIN_PATH)
				.put("startEffectiveTime", conceptEffectiveDate)
				.put("endEffectiveTime", conceptEffectiveDate)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(Branch.MAIN_PATH))
			.and().body("startEffectiveTime", equalTo(conceptEffectiveDate))
			.and().body("endEffectiveTime", equalTo(conceptEffectiveDate))
			.and().body("includeUnpublished", equalTo(true));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String textDefinitionLine = getComponentLine(ImmutableList.<String> of(textDefinitionId, conceptEffectiveDate, "1", MODULE_SCT_CORE, conceptId, "en",
				Concepts.TEXT_DEFINITION, textDefinitionTerm, CaseSignificance.INITIAL_CHARACTER_CASE_INSENSITIVE.getConceptId()));
		
		String unpublishedTextDefinitionLine = getComponentLine(ImmutableList.<String> of(unpublishedTextDefinitionId, "", "1", MODULE_SCT_CORE, conceptId, "en",
				Concepts.TEXT_DEFINITION, unpublishedTextDefinitionTerm, CaseSignificance.INITIAL_CHARACTER_CASE_INSENSITIVE.getConceptId()));

		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
				
		fileToLinesMap.put("sct2_Description", Pair.of(false, textDefinitionLine));
		fileToLinesMap.put("sct2_Description", Pair.of(false, unpublishedTextDefinitionLine));
		
		fileToLinesMap.put("sct2_TextDefinition", Pair.of(true, textDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition", Pair.of(true, unpublishedTextDefinitionLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportShouldAlwaysCreateTextDefinitionDescriptionAndLanguageRefsetFiles() throws Exception {

		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptRequestBody);
		
		final Date dateForNewVersion = getDateForNewVersion("SNOMEDCT");
		
		String versionName = Dates.formatByGmt(dateForNewVersion);
		String versionEffectiveDate = Dates.formatByGmt(dateForNewVersion, DateFormats.SHORT);
		
		whenCreatingVersion(versionName, versionEffectiveDate)
			.then().assertThat().statusCode(201);
		
		givenAuthenticatedRequest(ADMIN_API)
			.when().get("/codesystems/SNOMEDCT/versions/{id}", versionName)
			.then().assertThat().statusCode(200);
		
		assertComponentHasProperty(createMainPath(), SnomedComponentType.CONCEPT, conceptId, "definitionStatus", DefinitionStatus.PRIMITIVE.name());
		
		final Map<?, ?> updateRequest = ImmutableMap.builder()
				.put("definitionStatus", DefinitionStatus.FULLY_DEFINED)
				.put("commitComment", "Changed definition status of concept")
				.build();
		
		assertComponentCanBeUpdated(createMainPath(), SnomedComponentType.CONCEPT, conceptId, updateRequest);
		assertComponentHasProperty(createMainPath(), SnomedComponentType.CONCEPT, conceptId, "definitionStatus", DefinitionStatus.FULLY_DEFINED.name());
		
		// create new version
		
		final Date newVersion = getDateForNewVersion("SNOMEDCT");
		
		String newVersionName = Dates.formatByGmt(newVersion);
		String newVersionEffectiveDate = Dates.formatByGmt(newVersion, DateFormats.SHORT);
		
		whenCreatingVersion(newVersionName, newVersionEffectiveDate)
			.then().assertThat().statusCode(201);
		
		givenAuthenticatedRequest(ADMIN_API)
			.when().get("/codesystems/SNOMEDCT/versions/{id}", newVersionName)
			.then().assertThat().statusCode(200);
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", Branch.MAIN_PATH)
				.put("startEffectiveTime", newVersionEffectiveDate)
				.put("endEffectiveTime", newVersionEffectiveDate)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(Branch.MAIN_PATH))
			.and().body("startEffectiveTime", equalTo(newVersionEffectiveDate))
			.and().body("endEffectiveTime", equalTo(newVersionEffectiveDate));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
				
		fileToLinesMap.put("sct2_Description", Pair.of(false, ""));
		fileToLinesMap.put("sct2_TextDefinition", Pair.of(false, ""));
		fileToLinesMap.put("der2_cRefset_Language", Pair.of(false, ""));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void exportDescriptionsTextDefinitionsAndLanguageRefsetMembersPerLanguageCode() throws Exception {
		
		// create new version
		assertNewVersionCreated();
		
		// create new concept
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptRequestBody);

		final String englishTextDefinitionTerm = "Text Definition with effective time";
		String englishTextDefinitionId = createAcceptableDescription(conceptId, englishTextDefinitionTerm, "en", Concepts.TEXT_DEFINITION);
		
		final String danishTextDefinitionTerm = "Danish Text Definition with effective time";
		String danishTextDefinitionId = createAcceptableDescription(conceptId, danishTextDefinitionTerm, "da", Concepts.TEXT_DEFINITION);
		
		final String englishDescriptionTerm = "Description with effective time";
		String englishDescriptionId = createAcceptableDescription(conceptId, englishDescriptionTerm, "en", Concepts.SYNONYM);
		
		final String danishDescriptionTerm = "Danish Description with effective time";
		String danishDescriptionId = createAcceptableDescription(conceptId, danishDescriptionTerm, "da", Concepts.SYNONYM);

		// version new concept
		Date conceptVersionDate = assertNewVersionCreated();
		String conceptEffectiveDate = Dates.formatByGmt(conceptVersionDate, DateFormats.SHORT);

		final String unpublishedEnglishTextDefinitionTerm = "Unpublished Text Definition";
		String unpublishedEnglishTextDefinitionId = createAcceptableDescription(conceptId, unpublishedEnglishTextDefinitionTerm, "en", Concepts.TEXT_DEFINITION);
		
		final String unpublishedDanishTextDefinitionTerm = "Unpublished Danish Text Definition";
		String unpublishedDanishTextDefinitionId = createAcceptableDescription(conceptId, unpublishedDanishTextDefinitionTerm, "da", Concepts.TEXT_DEFINITION);
		
		final String unpublishedEnglishDescriptionTerm = "Unpublished Description";
		String unpublishedEnglishDescriptionId = createAcceptableDescription(conceptId, unpublishedEnglishDescriptionTerm, "en", Concepts.SYNONYM);
		
		final String unpublishedDanishDescriptionTerm = "Unpublished Danish Description";
		String unpublishedDanishDescriptionId = createAcceptableDescription(conceptId, unpublishedDanishDescriptionTerm, "da", Concepts.SYNONYM);
		
		// do not create new version
		
		final Map<Object, Object> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", Branch.MAIN_PATH)
				.put("startEffectiveTime", conceptEffectiveDate)
				.put("endEffectiveTime", conceptEffectiveDate)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(Branch.MAIN_PATH))
			.and().body("startEffectiveTime", equalTo(conceptEffectiveDate))
			.and().body("endEffectiveTime", equalTo(conceptEffectiveDate))
			.and().body("includeUnpublished", equalTo(true));
		
		final File exportArchive = assertExportFileCreated(exportId);
		
		String englishTextDefinitionLine = createDescriptionLine(englishTextDefinitionId, conceptEffectiveDate, conceptId, "en", Concepts.TEXT_DEFINITION, englishTextDefinitionTerm);
		String danishTextDefinitionLine = createDescriptionLine(danishTextDefinitionId, conceptEffectiveDate, conceptId, "da", Concepts.TEXT_DEFINITION, danishTextDefinitionTerm);
		String englishDescriptionLine = createDescriptionLine(englishDescriptionId, conceptEffectiveDate, conceptId, "en", Concepts.SYNONYM, englishDescriptionTerm);
		String danishDescriptionLine = createDescriptionLine(danishDescriptionId, conceptEffectiveDate, conceptId, "da", Concepts.SYNONYM, danishDescriptionTerm);
		
		String unpublishedEnglishTextDefinitionLine = createDescriptionLine(unpublishedEnglishTextDefinitionId, "", conceptId, "en", Concepts.TEXT_DEFINITION, unpublishedEnglishTextDefinitionTerm);
		String unpublishedDanishTextDefinitionLine = createDescriptionLine(unpublishedDanishTextDefinitionId, "", conceptId, "da", Concepts.TEXT_DEFINITION, unpublishedDanishTextDefinitionTerm);
		String unpublishedEnglishDescriptionLine = createDescriptionLine(unpublishedEnglishDescriptionId, "", conceptId, "en", Concepts.SYNONYM, unpublishedEnglishDescriptionTerm);
		String unpublishedDanishDescriptionLine = createDescriptionLine(unpublishedDanishDescriptionId, "", conceptId, "da", Concepts.SYNONYM, unpublishedDanishDescriptionTerm);

		String englishTextDefinitionMemberLine = createAcceptableLanguageRefsetMemberLine(englishTextDefinitionId, conceptEffectiveDate);
		String danishTextDefinitionMemberLine = createAcceptableLanguageRefsetMemberLine(danishTextDefinitionId, conceptEffectiveDate);
		String englishDescriptionMemberLine = createAcceptableLanguageRefsetMemberLine(englishDescriptionId, conceptEffectiveDate);
		String danishDescriptionMemberLine = createAcceptableLanguageRefsetMemberLine(danishDescriptionId, conceptEffectiveDate);
		
		String unpublishedEnglishTextDefinitionMemberLine = createAcceptableLanguageRefsetMemberLine(unpublishedEnglishTextDefinitionId, "");
		String unpublishedDanishTextDefinitionMemberLine = createAcceptableLanguageRefsetMemberLine(unpublishedDanishTextDefinitionId, "");
		String unpublishedEnglishDescriptionMemberLine = createAcceptableLanguageRefsetMemberLine(unpublishedEnglishDescriptionId, "");
		String unpublishedDanishDescriptionMemberLine = createAcceptableLanguageRefsetMemberLine(unpublishedDanishDescriptionId, "");
		
		final Multimap<String, Pair<Boolean, String>> fileToLinesMap = ArrayListMultimap.<String, Pair<Boolean, String>>create();
				
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, englishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, danishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(true, englishDescriptionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, danishDescriptionLine));
		
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, englishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, danishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, englishDescriptionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(true, danishDescriptionLine));
		
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(true, englishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, danishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, englishDescriptionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, danishDescriptionLine));

		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, englishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(true, danishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, englishDescriptionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, danishDescriptionLine));
		
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, unpublishedEnglishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, unpublishedDanishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(true, unpublishedEnglishDescriptionLine));
		fileToLinesMap.put("sct2_Description_Delta-en", Pair.of(false, unpublishedDanishDescriptionLine));
		
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, unpublishedEnglishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, unpublishedDanishTextDefinitionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(false, unpublishedEnglishDescriptionLine));
		fileToLinesMap.put("sct2_Description_Delta-da", Pair.of(true, unpublishedDanishDescriptionLine));
		
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(true, unpublishedEnglishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, unpublishedDanishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, unpublishedEnglishDescriptionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-en", Pair.of(false, unpublishedDanishDescriptionLine));

		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, unpublishedEnglishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(true, unpublishedDanishTextDefinitionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, unpublishedEnglishDescriptionLine));
		fileToLinesMap.put("sct2_TextDefinition_Delta-da", Pair.of(false, unpublishedDanishDescriptionLine));
		
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(true, englishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(false, danishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(true, englishDescriptionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(false, danishDescriptionMemberLine));
		
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(false, englishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(true, danishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(false, englishDescriptionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(true, danishDescriptionMemberLine));
		
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(true, unpublishedEnglishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(false, unpublishedDanishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(true, unpublishedEnglishDescriptionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-en", Pair.of(false, unpublishedDanishDescriptionMemberLine));
		
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(false, unpublishedEnglishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(true, unpublishedDanishTextDefinitionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(false, unpublishedEnglishDescriptionMemberLine));
		fileToLinesMap.put("der2_cRefset_LanguageDelta-da", Pair.of(true, unpublishedDanishDescriptionMemberLine));
		
		assertArchiveContainsLines(exportArchive, fileToLinesMap);
	}
	
	@Test
	public void mutexExport() throws Exception {
		final Map<Object, Object> config = ImmutableMap.builder()
												.put("type", "FULL")
												.put("branchPath", Branch.MAIN_PATH)
												.put("includeUnpublished", true)
												.build();

		// create/register 2 export requests
		final String exportId1 = assertExportConfigurationCanBeCreated(config);
		final String exportId2 = assertExportConfigurationCanBeCreated(config);
		
		// assert their existence
		assertExportConfiguration(exportId1);
		assertExportConfiguration(exportId2);
		
		Thread backgroundExportThread = startExportInBackground(exportId1);
		assertExportConflicts(exportId2);
		
		backgroundExportThread.join();
	}
	
	private String getLanguageRefsetMemberId(String descriptionId) {
		final Collection<Map<String, Object>> members = assertDescriptionExists(createMainPath(), descriptionId, "members()").extract().body().path("members.items");
		return String.valueOf(Iterables.getOnlyElement(members).get("id"));
	}
	
	private String createDescriptionLine(String id, String effectiveTime, String conceptId, String languageCode, String type, String term) {
		return getComponentLine(ImmutableList.<String> of(id, effectiveTime, "1", MODULE_SCT_CORE, conceptId, languageCode,
				type, term, CaseSignificance.INITIAL_CHARACTER_CASE_INSENSITIVE.getConceptId()));
	}
	
	private String createAcceptableLanguageRefsetMemberLine(String descriptionId, String effectiveTime) {
		return getComponentLine(ImmutableList.<String> of(getLanguageRefsetMemberId(descriptionId), effectiveTime, "1", MODULE_SCT_CORE, Concepts.REFSET_LANGUAGE_TYPE_UK, descriptionId,
				Acceptability.ACCEPTABLE.getConceptId()));
	}
	
	private String createAcceptableDescription(String conceptId, String term, String languageCode, String typeId) {
		
		final Map<?, ?> descriptionRequestBody = ImmutableMap.builder()
			.put("conceptId", conceptId)
			.put("moduleId", MODULE_SCT_CORE)
			.put("typeId", typeId)
			.put("term", term)
			.put("languageCode", languageCode)
			.put("acceptability", ACCEPTABLE_ACCEPTABILITY_MAP)
			.put("commitComment", "new description")
			.build();
		
		return assertComponentCreated(createMainPath(), SnomedComponentType.DESCRIPTION, descriptionRequestBody);
	}
	
	private void updateMemberEffectiveTime(final IBranchPath branchPath, final String memberId, final String effectiveTime, boolean force) {
		final Map<?, ?> effectiveTimeUpdate = ImmutableMap.of("effectiveTime", effectiveTime, "commitComment", "Update member effective time: " + memberId);
		// without force flag API responds with 204, but the content remains the same
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.with().contentType(ContentType.JSON)
			.and().body(effectiveTimeUpdate)
			.when().put("/{path}/{componentType}/{id}?force="+force, branchPath.getPath(), SnomedComponentType.MEMBER.toLowerCasePlural(), memberId)
			.then().log().ifValidationFails()
			.statusCode(204);
	}
	

	private String createAdditionalRelationshipOnMain() {
		final Map<?, ?> additionalRequestBody = givenRelationshipRequestBody(BLEEDING, TEMPORAL_CONTEXT, FINDING_CONTEXT, MODULE_SCT_CORE,
				CharacteristicType.ADDITIONAL_RELATIONSHIP, "New relationship on MAIN");
		final String additionalRelationshipId = assertComponentCreated(createMainPath(), SnomedComponentType.RELATIONSHIP, additionalRequestBody);
		assertComponentHasProperty(createMainPath(), SnomedComponentType.RELATIONSHIP, additionalRelationshipId, "characteristicType",
				CharacteristicType.ADDITIONAL_RELATIONSHIP.name());
		return additionalRelationshipId;
	}

	private String createInferredRelationshipOnMain() {
		final Map<?, ?> inferredRequestBody = givenRelationshipRequestBody(DISEASE, TEMPORAL_CONTEXT, FINDING_CONTEXT, MODULE_SCT_CORE,
				CharacteristicType.INFERRED_RELATIONSHIP, "New relationship on MAIN");
		final String inferredRelationshipId = assertComponentCreated(createMainPath(), SnomedComponentType.RELATIONSHIP, inferredRequestBody);
		assertComponentHasProperty(createMainPath(), SnomedComponentType.RELATIONSHIP, inferredRelationshipId, "characteristicType",
				CharacteristicType.INFERRED_RELATIONSHIP.name());
		return inferredRelationshipId;
	}

	private String createStatedRelationshipOnMain() {
		final Map<?, ?> statedRequestBody = givenRelationshipRequestBody(DISEASE, TEMPORAL_CONTEXT, FINDING_CONTEXT, MODULE_SCT_CORE,
				"New relationship on MAIN");
		final String statedRelationshipId = assertComponentCreated(createMainPath(), SnomedComponentType.RELATIONSHIP, statedRequestBody);
		assertComponentHasProperty(createMainPath(), SnomedComponentType.RELATIONSHIP, statedRelationshipId, "characteristicType",
				CharacteristicType.STATED_RELATIONSHIP.name());
		return statedRelationshipId;
	}

	private String getComponentLine(final List<String> lineElements) {
		return Joiner.on("\t").join(lineElements);
	}
	

	// TODO remove
	private Collection<String> getEffectiveDates(final String terminologyShortName) {
		
		final Map<?, ?> response = givenAuthenticatedRequest(ADMIN_API)
			.and().contentType(ContentType.JSON)
			.when().get("/codesystems/{shortName}/versions", terminologyShortName)
			.then().extract().body().as(Map.class);
		
		if (!response.containsKey("items")) {
			return Collections.emptyList();
		}
		
		final List<String> effectiveDates = Lists.newArrayList();
		@SuppressWarnings("unchecked")
		final List<Map<?, ?>> items = (List<Map<?, ?>>) response.get("items");
		for (final Map<?, ?> version : items) {
			final String effectiveDate = (String) version.get("effectiveDate");
			effectiveDates.add(effectiveDate);
		}
		
		return effectiveDates;
	}

	// TODO remove
	private Date getDateForNewVersion(final String terminologyShortname) {
		Date latestEffectiveDate = new Date();
		for (final String effectiveDate : getEffectiveDates(terminologyShortname)) {
			Date effDate = Dates.parse(effectiveDate, DateFormats.SHORT);
			if (latestEffectiveDate.before(effDate)) {
				latestEffectiveDate = effDate;
			}
		}

		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(latestEffectiveDate);
		calendar.add(Calendar.DATE, 1);

		return calendar.getTime();
	}
	
}
