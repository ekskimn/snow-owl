package com.b2international.snowowl.snomed.api.rest.domain;

import com.b2international.snowowl.snomed.core.domain.ISnomedImportConfiguration;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as=SnomedImportRestConfiguration.class)
public interface SnomedStandardImportRestConfiguration {

	String getBranchPath();

	void setBranchPath(String branchPath);

	Rf2ReleaseType getType();

	Boolean getCreateVersions();

	void setType(Rf2ReleaseType type);

	void setCreateVersions(Boolean createVersions);

	/**
	 * Returns with the language reference set identifier concept ID for the import configuration.
	 * @return the language reference set ID for the preferred language.
	 */
	String getLanguageRefSetId();

	/**
	 * Sets the language reference set identifier concept ID based on
	 * the language reference set identifier concept ID argument.
	 * @param languageRefSetId the language reference set ID for the preferred language. 
	 */
	void setLanguageRefSetId(String languageRefSetId);

	public ISnomedImportConfiguration toConfig();

}