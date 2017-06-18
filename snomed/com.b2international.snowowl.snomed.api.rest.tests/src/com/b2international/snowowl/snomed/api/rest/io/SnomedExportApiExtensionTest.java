package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.CodeSystemVersionRestRequests;
import com.b2international.snowowl.snomed.api.rest.SnomedBranchingRestRequests;
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
	
	
	private String extensionName() {
		return String.format("SNOMEDCT-EXT-%s", System.currentTimeMillis());
	}
	
	@Test
	public void validateDeltaExportArchiveStructure() throws Exception {
		IBranchPath extensionBranch = setupExtension(extensionName());
		
		SnomedRestFixtures.createNewConcept(extensionBranch);
		
		String effectiveTimeString = CodeSystemVersionRestRequests.getNextAvailableEffectiveDateAsString(extensionBranch.lastSegment());
		CodeSystemVersionRestRequests.createVersion(extensionBranch.lastSegment(), effectiveTimeString, effectiveTimeString).statusCode(201);
		
		final File exportArchive = downloadExportArchive(effectiveTimeString, extensionBranch);
		assertDeltaExportArchiveStructure(exportArchive);
	}

	@Test(expected = AssertionError.class)
	public void checkInvalidExportFileStructure() {
		File invalidExportFile = new File(PlatformUtil.toAbsolutePath(getClass(), "invalid-delta-exportfile.zip"));
		assertDeltaExportArchiveStructure(invalidExportFile);
	}
	
	@Test
	public void validateExtensionDeltaExportFileSplitting() throws Exception {
		IBranchPath extensionBranch = setupExtension(extensionName());
		// create new concept with both 'en' and 'da' languages
		String conceptId = SnomedRestFixtures.createNewConcept(extensionBranch);
		addPolyglotDescriptions(conceptId, extensionBranch);
		
		String effectiveTimeString = CodeSystemVersionRestRequests.getNextAvailableEffectiveDateAsString(extensionBranch.lastSegment());
		CodeSystemVersionRestRequests.createVersion(extensionBranch.lastSegment(), effectiveTimeString, effectiveTimeString).statusCode(201);
		
		final File exportArchive = downloadExportArchive(effectiveTimeString, extensionBranch);
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
	
	private void addPolyglotDescriptions(String conceptId, IBranchPath extensionBranch) {
		SnomedRestFixtures.createNewDescription(extensionBranch, conceptId, Concepts.TEXT_DEFINITION, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "en");
		SnomedRestFixtures.createNewDescription(extensionBranch, conceptId, Concepts.TEXT_DEFINITION, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "da");
		SnomedRestFixtures.createNewDescription(extensionBranch, conceptId, Concepts.SYNONYM, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "en");
		SnomedRestFixtures.createNewDescription(extensionBranch, conceptId, Concepts.SYNONYM, SnomedRestFixtures.ACCEPTABLE_ACCEPTABILITY_MAP, "da");
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
	
	private File downloadExportArchive(String effectiveTimeString, IBranchPath path) throws Exception {
		final Map<?, ?> config = ImmutableMap.builder()
				.put("type", "DELTA")
				.put("branchPath", path.getPath())
				.put("startEffectiveTime", effectiveTimeString)
				.put("endEffectiveTime", effectiveTimeString)
				.put("includeUnpublished", true)
				.put("codeSystemShortName", path.lastSegment())
				.build();
			
		final String exportId = assertExportConfigurationCanBeCreated(config);
		
		assertExportConfiguration(exportId)
			.and().body("type", equalTo("DELTA"))
			.and().body("branchPath", equalTo(path.getPath()))
			.and().body("startEffectiveTime", equalTo(effectiveTimeString))
			.and().body("endEffectiveTime", equalTo(effectiveTimeString))
			.and().body("includeUnpublished", equalTo(true))
			.and().body("codeSystemShortName", equalTo(path.lastSegment()));
	
		final File exportArchive = assertExportFileCreated(exportId);
		return exportArchive;
	}
	
	
	
	private IBranchPath setupExtension(String extensionName) {
		// codeSystem needs a versioned base branch
		SnomedRestFixtures.createNewConcept(branchPath);
		String dateAsString = CodeSystemVersionRestRequests.getNextAvailableEffectiveDateAsString("SNOMEDCT");
		String lastPathSegment = lastPathSegment(CodeSystemVersionRestRequests.createVersion("SNOMEDCT", dateAsString, dateAsString).statusCode(201)
									.body(equalTo(""))
									.extract().header("Location"));
		
		IBranchPath extensionBaseBranch = BranchPathUtils.createPath(branchPath, lastPathSegment /*+ IBranchPath.SEPARATOR + extensionName()*/);
		SnomedBranchingRestRequests.createBranchRecursively(extensionBaseBranch);
		IBranchPath extensionBranch = BranchPathUtils.createPath(extensionBaseBranch, extensionName);
		SnomedBranchingRestRequests.createBranch(extensionBranch);
		createSnomedExtensionCodeSystem(extensionName, extensionBranch.getPath(), "DAN");
		
		
		return extensionBranch;
	}
}
