package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.MODULE_SCT_CORE;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.ACCEPTABLE_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants.PREFERRED_ACCEPTABILITY_MAP;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCreated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.givenConceptRequestBody;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @since 5.0.6
 */
public class SnomedExportApiExtensionTest extends AbstractSnomedExportApiTest {

	@Override
	protected IBranchPath createRandomBranchPath() {
		return createNestedBranch(BranchPathUtils.createMainPath(), UUID.randomUUID().toString(), "SNOMEDCT-EXT-" + System.nanoTime());
	}

	@Override
	public void setup() {
		// setting up branch paths
		super.setup();
		// setting up extension
		setupExtension();
	}
	
	@Test
	public void validateDeltaExportArchiveStructure() throws Exception {
		Date versionEffectiveTime = assertNewVersionCreated(testBranchPath, testBranchPath.lastSegment(), true);
		
		String effectiveTimeString = Dates.formatByGmt(versionEffectiveTime, DateFormats.SHORT);
		final File exportArchive = downloadExportArchive(effectiveTimeString);
		assertDeltaExportArchiveStructure(exportArchive);
	}

	@Test(expected = AssertionError.class)
	public void checkInvalidExportFileStructure() {
		File invalidExportFile = new File(PlatformUtil.toAbsolutePath(getClass(), "invalid-delta-exportfile.zip"));
		assertDeltaExportArchiveStructure(invalidExportFile);
	}
	
	@Test
	public void validateExtensionDeltaExportFileSplitting() throws Exception {
		// create new concept with both 'en' and 'da' languages
		final Map<?, ?> conceptRequestBody = givenConceptRequestBody(null, ROOT_CONCEPT, MODULE_SCT_CORE, PREFERRED_ACCEPTABILITY_MAP, false);
		String conceptId = assertComponentCreated(testBranchPath, SnomedComponentType.CONCEPT, conceptRequestBody);
		addPolyglotDescriptions(conceptId);
		
		Date versionEffectiveTime = assertNewVersionCreated(testBranchPath, testBranchPath.lastSegment(), false);
		String effectiveTimeString = Dates.formatByGmt(versionEffectiveTime, DateFormats.SHORT);
		
		final File exportArchive = downloadExportArchive(effectiveTimeString);
		assertLanguageCodeFileSplitting(exportArchive);
	}
	
	@Test(expected = AssertionError.class)
	public void checkInvalidLanguageSplitExportFile() {
		File incompletDeltaExport = new File(PlatformUtil.toAbsolutePath(getClass(), "invalid-delta-exportfile.zip"));
		assertLanguageCodeFileSplitting(incompletDeltaExport);
	}

	private void assertDeltaExportArchiveStructure(final File exportArchive) {
		Multimap<String, String> expectedStructure = createExpectedStructure();
		try (FileSystem fs = FileSystems.newFileSystem(exportArchive.toPath(), null)) {
			List<Entry<String, String>> missing = expectedStructure.entries().stream().filter(expEntry -> !hasExpectedPath(fs, expEntry)).collect(Collectors.toList());
			Assert.assertTrue(String.format("Unexpected export file structure. Missing required structure: %s", Lists.transform(missing, entry -> entry.getValue())), missing.isEmpty());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void assertLanguageCodeFileSplitting(final File exportArchive) {
		try (FileSystem fs = FileSystems.newFileSystem(exportArchive.toPath(), null)) {
			Path languageRefsetDirectory = fs.getPath("/SnomedCT_Release_INT", "RF2Release", "Refset", "Language");
			Path terminologyPath = fs.getPath("/SnomedCT_Release_INT", "RF2Release", "Terminology");
			
			assertFileSplitting(languageRefsetDirectory);
			assertFileSplitting(terminologyPath);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void addPolyglotDescriptions(String conceptId) {
		final String englishTextDefinitionTerm = "Text Definition with effective time";
		createAcceptableDescription(conceptId, englishTextDefinitionTerm, "en", Concepts.TEXT_DEFINITION);
		
		final String danishTextDefinitionTerm = "Danish Text Definition with effective time";
		createAcceptableDescription(conceptId, danishTextDefinitionTerm, "da", Concepts.TEXT_DEFINITION);
		
		final String englishDescriptionTerm = "Description with effective time";
		createAcceptableDescription(conceptId, englishDescriptionTerm, "en", Concepts.SYNONYM);
		
		final String danishDescriptionTerm = "Danish Description with effective time";
		createAcceptableDescription(conceptId, danishDescriptionTerm, "da", Concepts.SYNONYM);
	}

	private void assertFileSplitting(Path path) throws IOException {
		final List<String> languageCodes = ImmutableList.of("-en_", "-da_");

		List<String> problematicLanuageCodes = languageCodes.stream().filter(languageCode -> {
			try {
				return !Files.walk(path)
						.filter(file -> !file.toString().equals("/")) //skip the root folder
						.anyMatch(file -> toFileName(file).contains(languageCode));
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(),e);
			}
		}).collect(Collectors.toList());
		
		Assert.assertTrue(String.format("Export didn't contain any files for language code(s): %s", Lists.transform(problematicLanuageCodes, codePattern -> codePattern.replaceAll("[-,_]", ""))), problematicLanuageCodes.isEmpty());
	}
	
	private boolean hasExpectedPath(FileSystem fs, Entry<String, String> expectedParentToFileEntry) {
		try {
			return Files.walk(fs.getPath("/"))
					.filter(path -> !path.toString().equals("/")) //skip the root folder
					.anyMatch(actualPath -> checkPath(actualPath, expectedParentToFileEntry));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
	}

	private boolean checkPath(Path path, Entry<String, String> expEntry) {
		boolean parentPathMatches = hasNamePattern(path.getParent(), expEntry.getKey());
		boolean pathMatches = hasNamePattern(path, expEntry.getValue());
		return pathMatches && parentPathMatches;
	}


	private boolean hasNamePattern(Path path, String pattern) {
		return toFileName(path).contains(pattern);
	}
	
	private String toFileName(Path path) {
		if (path.getNameCount() == 0)
			return path.toString();
		return path.getFileName().toString();
	}
	
	private Multimap<String, String> createExpectedStructure() {
		Multimap<String, String> expectedStructure = ImmutableMultimap.<String, String> builder()
				.put("/", "SnomedCT_Release_INT")
				.put("SnomedCT_Release_INT", "RF2Release")
				.putAll("RF2Release", Lists.newArrayList("Terminology", "Refset"))
				.putAll("Terminology", Lists.newArrayList("TextDefinition_Delta", "StatedRelationship_Delta", "Relationship_Delta", "Description_Delta", "Concept_Delta"))
				.putAll("Refset", Lists.newArrayList("Metadata", "Language", "Content")).build();
		return expectedStructure;
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
		
		return assertComponentCreated(testBranchPath, SnomedComponentType.DESCRIPTION, descriptionRequestBody);
	}

	private File downloadExportArchive(String effectiveTimeString) throws Exception {
		final Map<?, ?> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", testBranchPath.getPath())
				.put("startEffectiveTime", effectiveTimeString)
				.put("endEffectiveTime", effectiveTimeString)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(testBranchPath.getPath()))
			.and().body("startEffectiveTime", equalTo(effectiveTimeString))
			.and().body("endEffectiveTime", equalTo(effectiveTimeString))
			.and().body("includeUnpublished", equalTo(true));
	
		final File exportArchive = assertExportFileCreated(exportId);
		return exportArchive;
	}
	
	private void setupExtension() {
		// codeSystem needs a versioned base branch
		assertNewVersionCreated(testBranchPath.getParent(), "SNOMEDCT", true);
		createSnomedExtensionCodeSystem(testBranchPath.lastSegment(), testBranchPath.getPath(), "DAN");
	}
}
