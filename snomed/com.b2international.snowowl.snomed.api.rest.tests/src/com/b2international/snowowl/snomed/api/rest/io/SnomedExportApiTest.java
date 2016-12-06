<<<<<<< HEAD
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
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.ACCEPTABLE_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenCreatingVersion;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentHasProperty;
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
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.core.domain.CaseSignificance;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
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
				.put("deltaStartEffectiveTime", versionEffectiveDate)
				.put("deltaEndEffectiveTime", versionEffectiveDate)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(versionPath))
			.and().body("deltaStartEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("deltaEndEffectiveTime", equalTo(versionEffectiveDate));
		
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
				.put("deltaStartEffectiveTime", versionEffectiveDate)
				.put("deltaEndEffectiveTime", versionEffectiveDate)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(Branch.MAIN_PATH))
			.and().body("deltaStartEffectiveTime", equalTo(versionEffectiveDate))
			.and().body("deltaEndEffectiveTime", equalTo(versionEffectiveDate))
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
=======
package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.eclipse.net4j.util.io.ZIPUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.b2international.commons.FileUtils;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.b2international.snowowl.test.commons.rest.RestExtensions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public class SnomedExportApiTest extends AbstractSnomedApiTest {

	private File extractDirectory = null;
	
	@Test
	public void testExport() throws IOException {
		File exportFile = assertFileCanBeExported("MAIN");
		extractDirectory = assertFileCanBeExtracted(exportFile);
		System.out.println(extractDirectory.getAbsolutePath());
		String dateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
		Assert.assertTrue(new File(extractDirectory, "SnomedCT_Release_INT/RF2Release/Terminology/sct2_Description_Snapshot-en_INT_" + dateString + ".txt").isFile());
		Assert.assertTrue(new File(extractDirectory, "SnomedCT_Release_INT/RF2Release/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_" + dateString + ".txt").isFile());
	}
	
	@After
	public void tearDown() {
		if (extractDirectory != null) {
			FileUtils.deleteDirectory(extractDirectory);
		}
	}
	
	private File assertFileCanBeExtracted(File exportFile) {
		File tempDir = Files.createTempDir();
		ZIPUtil.unzip(exportFile, tempDir);
		return tempDir;
	}

	private File assertFileCanBeExported(String branchPath) throws IOException {
		String exportId = assertExportConfigurationCanBeCreated(newExportConfiguration(branchPath));
		File exportFile = assertExportArchiveCanBeDownloaded(exportId);
		Assert.assertTrue(exportFile.isFile());
		Assert.assertTrue(exportFile.length() > 0);
		return exportFile;
	}

	private File assertExportArchiveCanBeDownloaded(String exportId) throws IOException {
		Response response = givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.when().get("/exports/" + exportId + "/archive");
		final InputStream releaseFileStream = response.asInputStream();
		File tempZipFile = File.createTempFile("export" + new Date().getTime(), ".zip");
		Files.copy(new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return releaseFileStream;
			}
		}, tempZipFile);
		return tempZipFile;
	}

	protected String assertExportConfigurationCanBeCreated(final Map<?, ?> exportConfiguration) {
		final Response response = whenCreatingExportConfiguration(exportConfiguration);

		final String location = RestExtensions.expectStatus(response, 201)
				.and().extract().response().header("Location");
		
		return lastPathSegment(location);
	}
	
	private Response whenCreatingExportConfiguration(Map<?, ?> exportConfiguration) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.and().body(exportConfiguration)
				.when().post("/exports");
	}

	private Map<?, ?> newExportConfiguration(String branchPath) {
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("branchPath", branchPath)
				.put("type", Rf2ReleaseType.SNAPSHOT.name())
				.build();
		return importConfiguration;
>>>>>>> origin/ms-develop
	}
	
}
