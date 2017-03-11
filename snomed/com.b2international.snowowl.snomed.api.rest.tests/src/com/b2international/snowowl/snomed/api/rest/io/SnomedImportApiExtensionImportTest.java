package com.b2international.snowowl.snomed.api.rest.io;

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.google.common.collect.ImmutableMap;

/**
 * @since 5.0.6
 */
public class SnomedImportApiExtensionImportTest extends AbstractSnomedImportApiTest {

	private static final String DANISH_LANGUAGE_REFSET = "554461000005103";

	@Override
	protected IBranchPath createRandomBranchPath() {
		return createNestedBranch(BranchPathUtils.createMainPath(), UUID.randomUUID().toString(), getExtensionName());
	}
	
	private String getExtensionName() {
		return "SNOMEDCT-DK";
	}
	
	@Test
	public void extensionCanBeImported() {
		// create extension branch first
		Map<?, ?> extensionBranchMetadata = createMetadata();
		updateBranchWithMetadata(extensionBranchMetadata);

		// create the code system of the exentsion branch		
		createSnomedExtensionCodeSystem(getExtensionName(), testBranchPath.getPath(), "DAN");
		
		
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.DELTA.name())
				.put("branchPath", testBranchPath.getPath())
				.put("createVersions", true)
				.build();
		
		final String importId = assertImportConfigurationCanBeCreated(importConfiguration);
		
		assertImportFileCanBeUploaded(importId, "SnomedCT_Release_INT_20150205_extension-test.zip");
		assertImportCompletes(importId);
	}

	private Map<?, ?> createMetadata() {
		return ImmutableMap.<String, String> builder()
				.put("assertionGroupNames", "common-authoring")
				.put("defaultModuleId", Concepts.MODULE_B2I_EXTENSION)
				.put("defaultNamespace", "1000005")
				.put("shortname","dk")
				.put("requiredLanguageRefset.da", DANISH_LANGUAGE_REFSET)
				.put("codeSystemShortName", "SNOMEDCT-DK")
				.build();
	}
}
