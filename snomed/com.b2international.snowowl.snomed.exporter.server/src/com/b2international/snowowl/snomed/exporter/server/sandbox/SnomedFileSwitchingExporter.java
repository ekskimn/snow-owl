package com.b2international.snowowl.snomed.exporter.server.sandbox;

public interface SnomedFileSwitchingExporter extends SnomedExporter {
	
	String getFileName(String[] rows);

}
