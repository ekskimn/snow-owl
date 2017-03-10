package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.snomed.api.rest.CodeSystemApiAssert.assertCodeSystemCreated;
import static com.b2international.snowowl.snomed.api.rest.CodeSystemApiAssert.newCodeSystemRequestDefaultBodyBuilder;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.whenUpdatingBranchWithPathAndMetadata;

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
		assertMetadataUpdateForBaseBranch();
		assertCreateCodeSystem();
		
		
		
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.DELTA.name())
				.put("branchPath", testBranchPath.getPath())
				.put("createVersions", true)
				.build();
		
		final String importId = assertImportConfigurationCanBeCreated(importConfiguration);
		
		assertImportFileCanBeUploaded(importId, "SnomedCT_Release_INT_20150205_extension-test.zip");
		assertImportCompletes(importId);
	}

	private void assertMetadataUpdateForBaseBranch() {
		whenUpdatingBranchWithPathAndMetadata(testBranchPath, createMetadata());
	}

	private void assertCreateCodeSystem() {
		final Map<String, String> newExtensionCodeSystemRequestBody = 
							newCodeSystemRequestDefaultBodyBuilder(getExtensionName(), testBranchPath.getPath(), "DAN")
							.put("extensionOf", "SNOMEDCT")
							.build();
		assertCodeSystemCreated(newExtensionCodeSystemRequestBody);
	}

	private Map<?, ?> createMetadata() {
		return ImmutableMap.<String, String> builder()
				.put("assertionGroupNames", "common-authoring,dk-authoring")
				.put("defaultModuleId", Concepts.MODULE_B2I_EXTENSION)
				.put("defaultNamespace", "1000005")
				.put("shortname","dk")
				.put("requiredLanguageRefset.da",DANISH_LANGUAGE_REFSET)
				.put("codeSystemShortName", "SNOMEDCT-DK")
				.build();
	}
}
