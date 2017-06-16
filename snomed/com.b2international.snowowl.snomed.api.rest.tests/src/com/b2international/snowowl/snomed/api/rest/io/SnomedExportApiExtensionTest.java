package com.b2international.snowowl.snomed.api.rest.io;

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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.CodeSystemVersionRestRequests;
import com.b2international.snowowl.snomed.api.rest.SnomedRestFixtures;
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
	protected String getAdditionalPathSegment() {
		return "SNOMEDCT-EXT-" + System.nanoTime();
	}
	
	@Test
	public void validateDeltaExportArchiveStructure() throws Exception {
		setupExtension();
		Date versionEffectiveTime = assertNewVersionCreated(branchPath, branchPath.lastSegment(), true);
		
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
		setupExtension();
		// create new concept with both 'en' and 'da' languages
		String conceptId = SnomedRestFixtures.createNewConcept(branchPath);
		addPolyglotDescriptions(conceptId);
		
		Date versionEffectiveTime = assertNewVersionCreated(branchPath, branchPath.lastSegment(), false);
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
		SnomedRestFixtures.createNewDescription(branchPath, conceptId, Concepts.TEXT_DEFINITION, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "en");
		SnomedRestFixtures.createNewDescription(branchPath, conceptId, Concepts.TEXT_DEFINITION, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "da");
		SnomedRestFixtures.createNewDescription(branchPath, conceptId, Concepts.SYNONYM, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "en");
		SnomedRestFixtures.createNewDescription(branchPath, conceptId, Concepts.SYNONYM, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "da");
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
	
	private File downloadExportArchive(String effectiveTimeString) throws Exception {
		final Map<?, ?> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", branchPath.getPath())
				.put("startEffectiveTime", effectiveTimeString)
				.put("endEffectiveTime", effectiveTimeString)
				.put("includeUnpublished", true)
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(branchPath.getPath()))
			.and().body("startEffectiveTime", equalTo(effectiveTimeString))
			.and().body("endEffectiveTime", equalTo(effectiveTimeString))
			.and().body("includeUnpublished", equalTo(true));
	
		final File exportArchive = assertExportFileCreated(exportId);
		return exportArchive;
	}
	
	
	
	private void setupExtension() {
		// codeSystem needs a versioned base branch
		String dateAsString = CodeSystemVersionRestRequests.getNextAvailableEffectiveDateAsString("SNOMEDCT");
		CodeSystemVersionRestRequests.createVersion("SNOMEDCT", dateAsString, dateAsString);
		createSnomedExtensionCodeSystem(branchPath.lastSegment(), branchPath.getPath(), "DAN");
	}
}
