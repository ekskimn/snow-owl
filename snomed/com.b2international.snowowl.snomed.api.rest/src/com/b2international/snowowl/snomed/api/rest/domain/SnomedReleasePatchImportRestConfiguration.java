package com.b2international.snowowl.snomed.api.rest.domain;

import com.b2international.snowowl.snomed.core.domain.ISnomedImportConfiguration;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as=SnomedImportRestConfiguration.class)
public interface SnomedReleasePatchImportRestConfiguration {

	void setPatchReleaseVersion(String patchReleaseVersion);

	String getPatchReleaseVersion();

	String getBranchPath();

	void setBranchPath(String branchPath);
		
	public ISnomedImportConfiguration toConfig();

}
