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

import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.*;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.b2international.commons.platform.PlatformUtil;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.api.domain.Rf2ReleaseType;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @since 2.0
 */
public class SnomedImportApiExamplesTest extends AbstractSnomedImportApiTest {

	private static final Set<String> FINISH_STATES = ImmutableSet.of("COMPLETED", "FAILED");

	private static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(1L);

	private static final long POLL_TIMEOUT = TimeUnit.SECONDS.toMillis(30L);

	private void assertImportFileCanBeUploaded(final String importId, final String importFile) {
		givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API).with().multiPart(new File(PlatformUtil.toAbsolutePath(getClass(), importFile)))
		.when().post("/imports/{id}/archive", importId)
		.then().assertThat().statusCode(204);
	}

	private void assertImportCompletes(final String importId) {
		final long endTime = System.currentTimeMillis() + POLL_TIMEOUT;

		long currentTime;
		String currentStatus;

		do {

			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (final InterruptedException e) {
				fail(e.toString());
			}

			currentStatus = givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
					.when().get("/imports/{id}", importId)
					.then().assertThat().statusCode(200)
					.and().extract().response().body().path("status");

			currentTime = System.currentTimeMillis();

		} while (!FINISH_STATES.contains(currentStatus) && currentTime < endTime);

		assertEquals("End state should be COMPLETED.", currentStatus, "COMPLETED");
	}

	private void assertImportFileCanBeImported(final String importFile) {

		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("type", Rf2ReleaseType.DELTA.name())
				.put("branchPath", testBranchPath.getPath())
				.put("languageRefSetId", Concepts.REFSET_LANGUAGE_TYPE_UK)
				.put("createVersions", false)
				.build();

		final String importId = assertImportConfigurationCanBeCreated(importConfiguration);
		assertImportFileCanBeUploaded(importId, importFile);
		assertImportCompletes(importId);
	}

	@Test
	public void importNewConcept() {
		givenBranchWithPath(testBranchPath);
		assertConceptNotExists(testBranchPath, "63961392103");

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");

		assertConceptExists(testBranchPath, "63961392103");
	}

	@Test
	public void importNewDescription() {
		givenBranchWithPath(testBranchPath);
		assertDescriptionNotExists(testBranchPath, "11320138110");

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150201_new_description.zip");

		assertDescriptionExists(testBranchPath, "11320138110");
	}

	@Test
	public void importNewRelationship() {
		givenBranchWithPath(testBranchPath);
		assertRelationshipNotExists(testBranchPath, "24088071128");

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150202_new_relationship.zip");

		assertRelationshipExists(testBranchPath, "24088071128");
	}

	@Test
	public void importNewPreferredTerm() {
		givenBranchWithPath(testBranchPath);

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");
		assertPreferredTermEquals(testBranchPath, "63961392103", "13809498114");

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150201_new_description.zip");
		assertPreferredTermEquals(testBranchPath, "63961392103", "13809498114");

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150203_change_pt.zip");
		assertPreferredTermEquals(testBranchPath, "63961392103", "11320138110");
	}

	@Test
	public void importConceptInactivation() {
		givenBranchWithPath(testBranchPath);

		assertImportFileCanBeImported("SnomedCT_Release_INT_20150131_new_concept.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150201_new_description.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150202_new_relationship.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150203_change_pt.zip");
		assertImportFileCanBeImported("SnomedCT_Release_INT_20150204_inactivate_concept.zip");

		assertComponentActive(testBranchPath, SnomedComponentType.CONCEPT, "63961392103", false);
	}
}
