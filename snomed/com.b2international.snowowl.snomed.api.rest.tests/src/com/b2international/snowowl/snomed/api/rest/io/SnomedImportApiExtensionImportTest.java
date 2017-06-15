package com.b2international.snowowl.snomed.api.rest.io;

import java.util.Map;

import org.junit.Test;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.rest.BranchBase;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.google.common.collect.ImmutableMap;

/**
 * @since 5.0.6
 */
@BranchBase(Branch.MAIN_PATH)
public class SnomedImportApiExtensionImportTest extends AbstractSnomedImportApiTest {

	private static final String DANISH_LANGUAGE_REFSET = "554461000005103";

	@Override
	protected String getAdditionalPathSegment() {
		return "SNOMEDCT-DK";
	}
	
	@Test
	public void extensionCanBeImported() {
		// create extension branch first
		Map<?, ?> extensionBranchMetadata = createMetadata();
		updateBranchWithMetadata(branchPath, extensionBranchMetadata);

		// create the code system of the exentsion branch		
		createSnomedExtensionCodeSystem(getAdditionalPathSegment(), branchPath.getPath(), "DAN");
		
		
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.DELTA.name())
				.put("branchPath", branchPath.getPath())
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
