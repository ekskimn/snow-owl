package com.b2international.snowowl.snomed.api.impl.validation.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtility {
	final static Logger logger = LoggerFactory.getLogger(ValidationDescriptionService.class);
	
	private static final Map<String, String> dialectMatchesUsToGb = new HashMap<>();
	
	static {
		String fileNameUs = "/opt/termserver/resources/test-resources/usTerms.txt";
		String fileNameGb = "/opt/termserver/resources/test-resources/gbTerms.txt";
		
		File fileUs = new File(fileNameUs);
		File fileGb = new File(fileNameGb);
		
		FileReader fileReaderUs, fileReaderGb;
		BufferedReader bufferedReaderUs, bufferedReaderGb;
		
		try {
			
			// open the readers
			fileReaderUs = new FileReader(fileUs);
			bufferedReaderUs = new BufferedReader(fileReaderUs);
			fileReaderGb = new FileReader(fileGb);
			bufferedReaderGb = new BufferedReader(fileReaderGb);
			
			// skip header liens
			bufferedReaderUs.readLine();
			bufferedReaderGb.readLine();
			
			String lineUs, lineGb;
			int skippedUs = 0;
			int skippedGb = 0;
			
			while ((lineUs = bufferedReaderUs.readLine()) != null) {
				lineGb = bufferedReaderGb.readLine();
				
				if (lineGb != null) {
					dialectMatchesUsToGb.put(lineUs.toLowerCase(), lineGb.toLowerCase());
				} else {
					skippedUs++;
				}
			}
			
			while ((lineGb = bufferedReaderGb.readLine()) != null) {
				skippedGb++;
			}
			logger.info("Loaded " + dialectMatchesUsToGb.size() + " dialect mappings for en-us to en-gb");
			if (skippedUs > 0) {
				logger.info("File mismatch: skipped " + skippedUs + " en-us words");
			}
			else if (skippedGb > 0) {
				logger.info("File mismatch: skipped " + skippedGb + " en-gb words");
			} else {
				logger.info("en-us and en-gb file lengths match");
			}
			
		} catch (IOException e) {
			logger.info("Failed to load international file map: " + e.getMessage());

		}
	}
	
	// intended for use in ValidationDescriptionService
	public static boolean hasUsSpelling(String word) {
		return dialectMatchesUsToGb.containsKey(word.toLowerCase());
	}
	
	// intended for use in ValidationDescriptionService
	public static boolean hasGbSpelling(String word) {
		return dialectMatchesUsToGb.values().contains(word.toLowerCase());
	}
	
	// used by 
	public static String getGbSpellingForUsSpelling(String usWord) {
		String word = dialectMatchesUsToGb.get(usWord.toLowerCase());
		// preserve first-letter capitalization
		if (word != null && !word.isEmpty() && usWord != null && !usWord.isEmpty() && usWord.substring(0, 1).equals(usWord.substring(0, 1).toUpperCase())) {
			word = word.substring(0, 1).toUpperCase() + word.substring(1);
		}
		return word;
	}

}
