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
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentHasProperty;
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
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSetType;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
		
		String statedLine = getComponentLine(ImmutableList.<String> of(statedRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.STATED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String inferredLine = getComponentLine(ImmutableList.<String> of(inferredRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, DISEASE,
				FINDING_CONTEXT, "0", TEMPORAL_CONTEXT, CharacteristicType.INFERRED_RELATIONSHIP.getConceptId(),
				Concepts.EXISTENTIAL_RESTRICTION_MODIFIER));
		String additionalLine = getComponentLine(ImmutableList.<String> of(additionalRelationshipId, transientEffectiveTime, "1", MODULE_SCT_CORE, BLEEDING,
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
		
		Date newVersionDate = assertNewVersionCreated();
		
		String versionName = Dates.formatByGmt(newVersionDate);
		String versionEffectiveDate = Dates.formatByGmt(newVersionDate, DateFormats.SHORT);
		String versionPath = BranchPathUtils.createVersionPath(versionName).getPath();
		
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
		
		IBranchPath versionPath = BranchPathUtils.createVersionPath(versionName);
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
	
	private static void updateMemberEffectiveTime(final IBranchPath branchPath, final String memberId, final String effectiveTime, boolean force) {
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

	private String getComponentLine(List<String> lineElements) {
		return Joiner.on("\t").join(lineElements);
	}
	
	private Date assertNewVersionCreated() {
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		assertComponentCreated(createMainPath(), SnomedComponentType.CONCEPT, conceptRequestBody);
		
		final Date dateForNewVersion = getDateForNewVersion("SNOMEDCT");
		
		String versionName = Dates.formatByGmt(dateForNewVersion);
		String versionEffectiveDate = Dates.formatByGmt(dateForNewVersion, DateFormats.SHORT);
		
		whenCreatingVersion(versionName, versionEffectiveDate)
			.then().assertThat().statusCode(201);
		
		givenAuthenticatedRequest(ADMIN_API)
			.when().get("/codesystems/SNOMEDCT/versions/{id}", versionName)
			.then().assertThat().statusCode(200);
		
		return dateForNewVersion;
	}

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
