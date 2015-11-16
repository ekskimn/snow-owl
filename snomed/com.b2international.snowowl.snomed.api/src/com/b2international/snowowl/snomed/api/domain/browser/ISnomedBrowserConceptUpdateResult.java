package com.b2international.snowowl.snomed.api.domain.browser;

public interface ISnomedBrowserConceptUpdateResult {
	public String getConceptId();
	public void setConceptId(String conceptId) ;
	public boolean isSuccess();
	public void setSuccess(boolean success) ;
	public String getErrorMsg() ;
	public void setErrorMsg(String errorMsg) ;
}
