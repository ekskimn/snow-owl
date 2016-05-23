package com.b2international.snowowl.snomed.api.rest.components;

import static com.b2international.snowowl.datastore.BranchPathUtils.createMainPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedBranchingApiAssert.givenBranchWithPath;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanBeDeleted;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanNotBeDeleted;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentCanBeUpdated;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertComponentHasProperty;
import static com.b2international.snowowl.snomed.api.rest.SnomedComponentApiAssert.assertConceptExists;

import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedComponentType;
import com.b2international.snowowl.snomed.core.domain.DefinitionStatus;
import com.google.common.collect.ImmutableMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SnomedReleasedConceptApiTest extends AbstractSnomedApiTest {

	private static final String CONCEPT_ID = "63961392103";

	@Test
	public void automaticRestorationOfEffectiveTime() {
		givenBranchWithPath(testBranchPath);
		assertConceptExists(testBranchPath, CONCEPT_ID);
		
		assertComponentHasProperty(testBranchPath, SnomedComponentType.CONCEPT, CONCEPT_ID, "effectiveTime", "20150204");
		
		updateDefinitionStatus(CONCEPT_ID, DefinitionStatus.FULLY_DEFINED);

		assertComponentHasProperty(testBranchPath, SnomedComponentType.CONCEPT, CONCEPT_ID, "effectiveTime", null);

		updateDefinitionStatus(CONCEPT_ID, DefinitionStatus.PRIMITIVE);

		assertComponentHasProperty(testBranchPath, SnomedComponentType.CONCEPT, CONCEPT_ID, "effectiveTime", "20150204");
	}

	@Test
	public void deleteFails() {
		assertComponentCanNotBeDeleted(createMainPath(), SnomedComponentType.CONCEPT, CONCEPT_ID, false);
	}

	@Test
	public void forceDelete() {
		assertComponentCanBeDeleted(createMainPath(), SnomedComponentType.CONCEPT, CONCEPT_ID, true);
	}

	private void updateDefinitionStatus(final String conceptId,
			final DefinitionStatus definitionStatus) {
		final Map<?, ?> updateRequestBody = ImmutableMap.builder()
				.put("definitionStatus", definitionStatus.toString())
				.put("commitComment", "Changed concept definition status")
				.build();
		assertComponentCanBeUpdated(testBranchPath, SnomedComponentType.CONCEPT, conceptId, updateRequestBody);
	}

}
