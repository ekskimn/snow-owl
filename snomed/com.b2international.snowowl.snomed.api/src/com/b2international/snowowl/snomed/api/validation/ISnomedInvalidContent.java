package com.b2international.snowowl.snomed.api.validation;

public interface ISnomedInvalidContent {
	
	String getConceptId();
	String getComponentId();
	String getMessage();
	String getSeverity();

}
