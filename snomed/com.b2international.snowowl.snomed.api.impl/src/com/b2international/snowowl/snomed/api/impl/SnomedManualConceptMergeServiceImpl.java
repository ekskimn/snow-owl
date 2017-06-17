package com.b2international.snowowl.snomed.api.impl;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.b2international.commons.FileUtils;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class SnomedManualConceptMergeServiceImpl {

	@Resource
	private ObjectMapper objectMapper;
	
	private ObjectMapper objectMapper() {
		return Preconditions.checkNotNull(objectMapper == null ? objectMapper = ApplicationContext.getInstance().getServiceChecked(ObjectMapper.class) : objectMapper,  "objectMapper cannot be null!");
	}
	
	private String storeRoot;
	
	private static String MERGE_REVIEW_STORE = "merge-review-store";
	private static final String SLASH = "/";
	private static final String FILE_TYPE = ".json";
	
	@PostConstruct
	protected void init() {
		storeRoot = SnowOwlApplication.INSTANCE.getEnviroment().getDataDirectory().getPath();
	}

	public void storeConceptChanges(String branchPath, String mergeReviewId, ISnomedBrowserConcept conceptUpdate) {
		try {
			File conceptFile = getConceptStorePath(branchPath, mergeReviewId, conceptUpdate.getConceptId());
			conceptFile.getParentFile().mkdirs();
			objectMapper().writeValue(conceptFile, conceptUpdate);
		} catch (IOException e) {
			throw new SnowowlRuntimeException("Failed to persist manual concept merge.", e);
		}
	}

	public boolean exists(String branchPath, String mergeReviewId, String conceptId) {
		return getConceptStorePath(branchPath, mergeReviewId, conceptId).isFile();
	}
	
	public ISnomedBrowserConcept retrieve(String branchPath, String mergeReviewId, String conceptId) throws IOException {
		File conceptFile = getConceptStorePath(branchPath, mergeReviewId, conceptId);
		if (!conceptFile.isFile()) {
			throw new NotFoundException("manual concept merge", conceptId);
		}
		return objectMapper().readValue(conceptFile, SnomedBrowserConcept.class);
	}
	
	private File getConceptStorePath(String branchPath, String mergeReviewId, String conceptId) {
		if (storeRoot == null) {
			init();
		}
		return new File(storeRoot + SLASH + MERGE_REVIEW_STORE + SLASH + branchPath + SLASH + mergeReviewId + SLASH + conceptId + FILE_TYPE); 
	}

	public void deleteAll(String branchPath, String mergeReviewId) {
		final File parentFile = getConceptStorePath(branchPath, mergeReviewId, "1").getParentFile();
		FileUtils.deleteDirectory(parentFile);
	}
}
