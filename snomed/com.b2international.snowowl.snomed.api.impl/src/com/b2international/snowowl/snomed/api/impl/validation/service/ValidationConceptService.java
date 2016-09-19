package com.b2international.snowowl.snomed.api.impl.validation.service;

import org.ihtsdo.drools.service.ConceptService;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;

public class ValidationConceptService implements ConceptService {

	private SnomedTerminologyBrowser terminologyBrowser;
	private IBranchPath branchPath = null;

	public ValidationConceptService(IBranchPath branchPath, SnomedTerminologyBrowser terminologyBrowser) {
		this.branchPath = branchPath;
		this.terminologyBrowser = terminologyBrowser;
	}

	@Override
	public boolean isActive(String conceptId) {
		System.out.println("isActive     :     " + conceptId);
		System.out.println("  branchPath : " + branchPath);
		System.out.println("  browserNull: " + terminologyBrowser == null ? "true" : "false");
		
		SnomedConceptIndexEntry entry = terminologyBrowser.getConcept(branchPath, conceptId);
		System.out.println("  concept    : " + entry == null ? "null" : entry.toString());
		return terminologyBrowser.getConcept(branchPath, conceptId).isActive();
	}

}
