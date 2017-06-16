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
package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.snomed.api.rest.CodeSystemApiAssert.assertCodeSystemCreated;
import static com.b2international.snowowl.snomed.api.rest.CodeSystemApiAssert.newCodeSystemRequestBody;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentActive;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptIndexedBrowserPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptNotExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertDescriptionExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertDescriptionNotExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertDescriptionPropertyEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertPreferredTermEquals;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertRelationshipExists;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertRelationshipNotExists;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;

import java.io.File;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.google.common.collect.ImmutableMap;

/**
 * @since 2.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SnomedImportApiExamplesTest extends AbstractSnomedImportApiTest {

	@Override
	protected IBranchPath createRandomBranchPath() {
		return BranchPathUtils.createMainPath();
	}
	
	@Test
	public void import010NewConcept() {
		assertConceptNotExists(testBranchPath, "63961392103");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");
		assertConceptExists(testBranchPath, "63961392103");
		assertPreferredTermEquals(testBranchPath, "63961392103", "13809498114");
		givenAuthenticatedRequest("/admin").when().get("/codesystems/{shortName}/versions/{version}", "SNOMEDCT", "2015-01-31").then().statusCode(200);
	}
	
	@Test
	public void import011PatchConcept() {
		// Assert concept released and primitive
		assertConceptPropertyEquals(testBranchPath, "63961392103", "effectiveTime", "20150131");
		assertConceptPropertyEquals(testBranchPath, "63961392103", "released", true);
		assertConceptPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "PRIMITIVE");
		assertConceptIndexedBrowserPropertyEquals(testBranchPath, "63961392103", "effectiveTime", "20150131");
		assertConceptIndexedBrowserPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "PRIMITIVE");

		// Assert description released and case insensitive
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "effectiveTime", "20150131");
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "released", true);
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "caseSignificance", "CASE_INSENSITIVE");
		
		assertImportFileCanNotBeImported("SnomedCT_Release_INT_20150131_patch_release_concept.zip");
		assertConceptPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "PRIMITIVE");
		
		//Patch content will now allow dates other than the patch date to be modified
		assertPatchImportFileCanBeImported("SnomedCT_Release_INT_20150131_patch_release_concept.zip", "2014-07-31");
		assertConceptPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "FULLY_DEFINED");
		
		assertPatchImportFileCanBeImported("SnomedCT_Release_INT_20150131_patch_release_concept.zip", "2015-01-31");

		// Assert concept released and fully defined with unchanged effectiveTime
		assertConceptPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "FULLY_DEFINED");
		assertConceptPropertyEquals(testBranchPath, "63961392103", "effectiveTime", "20150131");
		assertConceptPropertyEquals(testBranchPath, "63961392103", "released", true);
		assertConceptIndexedBrowserPropertyEquals(testBranchPath, "63961392103", "definitionStatus", "FULLY_DEFINED");
		assertConceptIndexedBrowserPropertyEquals(testBranchPath, "63961392103", "effectiveTime", "20150131");

		// Assert description released and initial character case sensitive with unchanged effectiveTime
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "effectiveTime", "20150131");
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "released", true);
		assertDescriptionPropertyEquals(testBranchPath, "13809498114", "caseSignificance", "INITIAL_CHARACTER_CASE_INSENSITIVE");
	}

	@Test
	public void import02NewDescription() {
		assertDescriptionNotExists(testBranchPath, "11320138110");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150201_new_description.zip");
		assertDescriptionExists(testBranchPath, "11320138110");
		givenAuthenticatedRequest("/admin").when().get("/codesystems/{shortName}/versions/{version}", "SNOMEDCT", "2015-02-01").then().statusCode(200);
	}

	@Test
	public void import03NewRelationship() {
		assertRelationshipNotExists(testBranchPath, "24088071128");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150202_new_relationship.zip");
		assertRelationshipExists(testBranchPath, "24088071128");
		givenAuthenticatedRequest("/admin").when().get("/codesystems/{shortName}/versions/{version}", "SNOMEDCT", "2015-02-02").then().statusCode(200);
	}

	@Test
	public void import04NewPreferredTerm() {
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150203_change_pt.zip");
		assertPreferredTermEquals(testBranchPath, "63961392103", "11320138110");
		givenAuthenticatedRequest("/admin").when().get("/codesystems/{shortName}/versions/{version}", "SNOMEDCT", "2015-02-03").then().statusCode(200);
	}

	@Test
	public void import05ConceptInactivation() {
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150204_inactivate_concept.zip");
		assertComponentActive(testBranchPath, SnomedComponentType.CONCEPT, "63961392103", false);
	}
	
	@Test
	public void import06IndexInitBug_ImportSameNewConceptWithAdditionalDescriptionShouldNotFail() throws Exception {
		assertConceptExists(testBranchPath, "63961392103").body("active", CoreMatchers.equalTo(false));
		assertPreferredTermEquals(testBranchPath, "63961392103", "11320138110");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_index_init_bug.zip");
		assertConceptExists(testBranchPath, "63961392103").body("active", CoreMatchers.equalTo(false));
		assertPreferredTermEquals(testBranchPath, "63961392103", "11320138110");
	}
	
	@Test
	public void import07InvalidBranchPath() {
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.FULL.name())
				.put("branchPath", "DK")
				.put("createVersions", true)
				.build();
		
		final String importId = assertImportConfigurationCanBeCreated(importConfiguration);
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API).with()
			.multiPart(new File(PlatformUtil.toAbsolutePath(getClass(), "SnomedCT_Release_INT_20150131_new_concept.zip")))
			.when().post("/imports/{id}/archive", importId)
			.then().assertThat().statusCode(400);
	}
	
	@Test
	public void import08NewExtensionConceptOnNestedBranch() {
		final IBranchPath nestedBranch = createNestedBranch(testBranchPath, "extensionImport");
		final Map<String, String> newCodeSystemRequestBody = newCodeSystemRequestBody("extensionImport", nestedBranch.getPath());
		assertCodeSystemCreated(newCodeSystemRequestBody);
		assertConceptNotExists(nestedBranch, "555231000005107");
		
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.DELTA.name())
				.put("branchPath", nestedBranch.getPath())
				.put("createVersions", true)
				.put("codeSystemShortName", "extensionImport")
				.build();

		final String importId = assertImportConfigurationCanBeCreated(importConfiguration);
		assertImportFileCanBeUploaded(importId, "SnomedCT_Release_INT_20150205_new_extension_concept.zip");
		assertImportCompletes(importId);
		
		assertConceptExists(nestedBranch, "555231000005107");
		givenAuthenticatedRequest("/admin")
				.when().get("/codesystems/{shortName}/versions/{version}", "extensionImport", "2015-02-05")
				.then().statusCode(200);
	}
	
}
