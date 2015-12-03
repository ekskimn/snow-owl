package com.b2international.snowowl.snomed.api.impl;

import java.io.File;
import java.io.IOException;

import com.b2international.commons.FileUtils;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConceptUpdate;
import com.b2international.snowowl.snomed.api.domain.exception.SnomedPersistanceException;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConceptUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SnomedManualConceptMergeServiceImpl {

	final private ObjectMapper objectMapper;
	final private String storeRoot;
	
	private static String MERGE_REVIEW_STORE = "merge-review-store";
	private static final String SLASH = "/";
	private static final String FILE_TYPE = ".json";
	
	public SnomedManualConceptMergeServiceImpl() {
		objectMapper = new ObjectMapper();
		storeRoot = SnowOwlApplication.INSTANCE.getEnviroment().getDataDirectory().getPath();
	}

	public void storeConceptChanges(String branchPath, String mergeReviewId, ISnomedBrowserConceptUpdate conceptUpdate) throws SnomedPersistanceException {
		try {
			File conceptFile = getConceptStorePath(branchPath, mergeReviewId, conceptUpdate.getConceptId());
			conceptFile.getParentFile().mkdirs();
			objectMapper.writeValue(conceptFile, conceptUpdate);
		} catch (IOException e) {
			throw new SnomedPersistanceException("Failed to persist manual concept merge.", e);
		}
	}

	public boolean exists(String branchPath, String mergeReviewId, String conceptId) {
		return getConceptStorePath(branchPath, mergeReviewId, conceptId).isFile();
	}
	
	public ISnomedBrowserConceptUpdate retrieve(String branchPath, String mergeReviewId, String conceptId) throws IOException {
		File conceptFile = getConceptStorePath(branchPath, mergeReviewId, conceptId);
		if (!conceptFile.isFile()) {
			throw new NotFoundException("manual concept merge", conceptId);
		}
		return objectMapper.readValue(conceptFile, SnomedBrowserConceptUpdate.class);
	}
	
	private File getConceptStorePath(String branchPath, String mergeReviewId, String conceptId) {
		return new File(storeRoot + SLASH + MERGE_REVIEW_STORE + SLASH + branchPath + SLASH + mergeReviewId + SLASH + conceptId + FILE_TYPE); 
	}

	public void deleteAll(String branchPath, String mergeReviewId) {
		
		final File parentFile = getConceptStorePath(branchPath, mergeReviewId, "1").getParentFile();
		FileUtils.deleteDirectory(parentFile);
	}

}
