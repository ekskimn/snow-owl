package com.b2international.snowowl.snomed.datastore;

import com.b2international.snowowl.core.api.IBranchPath;

public class BranchSpecificSnomedClientStatementBrowser extends
		SnomedClientStatementBrowser {

	private IBranchPath branchPath;

	public BranchSpecificSnomedClientStatementBrowser(SnomedStatementBrowser delegateBrowser, IBranchPath branchPath) {
		super(delegateBrowser);
		this.branchPath = branchPath;
	}
	
	@Override
	public IBranchPath getBranchPath() {
		return branchPath;
	}

}
