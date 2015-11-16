package com.b2international.snowowl.snomed.api.impl.domain.browser;

import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdateResult;

public class SnomedBrowserConceptUpdateResult implements ISnomedBrowserConceptUpdateResult {
	
	String conceptId;
	
	boolean success;
	
	String errorMsg;
	
	public String getConceptId() {
		return conceptId;
	}
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
}
