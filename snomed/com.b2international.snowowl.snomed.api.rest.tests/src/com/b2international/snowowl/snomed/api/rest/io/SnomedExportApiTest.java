package com.b2international.snowowl.snomed.api.rest.io;

import static com.b2international.snowowl.test.commons.rest.RestExtensions.givenAuthenticatedRequest;
import static com.b2international.snowowl.test.commons.rest.RestExtensions.lastPathSegment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.eclipse.net4j.util.io.ZIPUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.b2international.commons.FileUtils;
import com.b2international.snowowl.snomed.api.rest.AbstractSnomedApiTest;
import com.b2international.snowowl.snomed.api.rest.SnomedApiTestConstants;
import com.b2international.snowowl.snomed.core.domain.Rf2ReleaseType;
import com.b2international.snowowl.test.commons.rest.RestExtensions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public class SnomedExportApiTest extends AbstractSnomedApiTest {

	private File extractDirectory = null;
	
	@Test
	public void testExport() throws IOException {
		File exportFile = assertFileCanBeExported("MAIN");
		extractDirectory = assertFileCanBeExtracted(exportFile);
		System.out.println(extractDirectory.getAbsolutePath());
		String dateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
		Assert.assertTrue(new File(extractDirectory, "SnomedCT_Release_INT/RF2Release/Terminology/sct2_Description_Snapshot-en_INT_" + dateString + ".txt").isFile());
		Assert.assertTrue(new File(extractDirectory, "SnomedCT_Release_INT/RF2Release/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_" + dateString + ".txt").isFile());
	}
	
	@After
	public void tearDown() {
		if (extractDirectory != null) {
			FileUtils.deleteDirectory(extractDirectory);
		}
	}
	
	private File assertFileCanBeExtracted(File exportFile) {
		File tempDir = Files.createTempDir();
		ZIPUtil.unzip(exportFile, tempDir);
		return tempDir;
	}

	private File assertFileCanBeExported(String branchPath) throws IOException {
		String exportId = assertExportConfigurationCanBeCreated(newExportConfiguration(branchPath));
		File exportFile = assertExportArchiveCanBeDownloaded(exportId);
		Assert.assertTrue(exportFile.isFile());
		Assert.assertTrue(exportFile.length() > 0);
		return exportFile;
	}

	private File assertExportArchiveCanBeDownloaded(String exportId) throws IOException {
		Response response = givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
			.when().get("/exports/" + exportId + "/archive");
		final InputStream releaseFileStream = response.asInputStream();
		File tempZipFile = File.createTempFile("export" + new Date().getTime(), ".zip");
		Files.copy(new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return releaseFileStream;
			}
		}, tempZipFile);
		return tempZipFile;
	}

	protected String assertExportConfigurationCanBeCreated(final Map<?, ?> exportConfiguration) {
		final Response response = whenCreatingExportConfiguration(exportConfiguration);

		final String location = RestExtensions.expectStatus(response, 201)
				.and().extract().response().header("Location");
		
		return lastPathSegment(location);
	}
	
	private Response whenCreatingExportConfiguration(Map<?, ?> exportConfiguration) {
		return givenAuthenticatedRequest(SnomedApiTestConstants.SCT_API)
				.with().contentType(ContentType.JSON)
				.and().body(exportConfiguration)
				.when().post("/exports");
	}

	private Map<?, ?> newExportConfiguration(String branchPath) {
		final Map<?, ?> importConfiguration = ImmutableMap.builder()
				.put("branchPath", branchPath)
				.put("type", Rf2ReleaseType.SNAPSHOT.name())
				.build();
		return importConfiguration;
	}
	
}
