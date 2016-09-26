/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.exporter.server;

import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Sets.newHashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.b2international.collections.longs.LongKeyLongMap;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.snomed.datastore.services.ISnomedComponentService;
import com.b2international.snowowl.snomed.exporter.server.sandbox.SnomedExportConfiguration;
import com.b2international.snowowl.snomed.exporter.server.sandbox.SnomedExporter;
import com.b2international.snowowl.snomed.exporter.server.sandbox.AbstractSnomedRelationshipExporter;
import com.b2international.snowowl.snomed.exporter.server.sandbox.SnomedFileSwitchingExporter;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;


/**
 * This class executes the corresponding SNOMED&nbsp;CT specific exporter and writes the result to the given temporary working directory.
 * The subsequent path and filename is got by the specified importer.
 * 
 *
 */
public class SnomedExportExecutor {
	
	/**
	 * 
	 */
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final String RELEASE_BASE_DIRECTORY = "SnomedCT_Release_";
	
	private final String temporaryWorkingDirectory;
	private final File baseReleaseDir;
	private File releaseFileRelativeDirectory;
	private File releaseFilePath;
	private final SnomedExporter snomedExporter;
	private final Set<String> modulesToExport;
	private final String clientNamespace;
	private final Supplier<LongKeyLongMap> conceptIdToModuleIdSupplier = memoize(new Supplier<LongKeyLongMap>() {
		public LongKeyLongMap get() {
			return getServiceForClass(ISnomedComponentService.class).getConceptModuleMapping(configuration.getCurrentBranchPath());
		}
	});
	private final Collection<String> visitedIdWithEffectiveTime;
	private final Map<String, Writer> releaseFileWriters;

	private SnomedExportConfiguration configuration;

	public SnomedExportExecutor(final SnomedExporter snomedExporter, final String workingDirectory, final Set<String> modulesToExport, final String clientNamespace) {
		this.snomedExporter = snomedExporter;
		this.modulesToExport = modulesToExport;
		this.clientNamespace = clientNamespace;
		configuration = this.snomedExporter.getConfiguration();
		visitedIdWithEffectiveTime = newHashSet();
		releaseFileWriters = new HashMap<>();
		
		baseReleaseDir = new File(workingDirectory + File.separatorChar + RELEASE_BASE_DIRECTORY + clientNamespace);
		if (!baseReleaseDir.exists()) {
			baseReleaseDir.mkdirs();
		}
		
		this.temporaryWorkingDirectory = workingDirectory;
	}
	
	public void execute() throws IOException {
		releaseFilePath = write();
	}

	public File getTemporaryFile() {
		if (releaseFilePath == null) {
			throw new IllegalStateException("The exported file is not ready, this may sign that you are trying to get the file from a different thread or (more likely) you have not called the executor yet.");
		}
		return releaseFilePath;
	}
	
	public void deleteTemporaryFile() {
		if (releaseFilePath != null && releaseFilePath.exists()) {
			releaseFilePath.delete();
		}
	}
	
	private File write() throws IOException {
		releaseFileRelativeDirectory = new File(
				temporaryWorkingDirectory + 
				File.separatorChar + 
				RELEASE_BASE_DIRECTORY + 
				clientNamespace + 
				File.separatorChar + 
				snomedExporter.getRelativeDirectory());
		
		releaseFileRelativeDirectory.mkdirs();
		
		Writer writer = null;
		boolean fileSwitchingExporter = snomedExporter instanceof SnomedFileSwitchingExporter;
		if (!fileSwitchingExporter) {
			writer = getFileWriter(snomedExporter.getFileName());
		}
		try {			
			for (final String line : snomedExporter) {
				final String[] rows = line.split("\t");
				if (snomedExporter instanceof SnomedRf2Exporter && !isToExport(rows)) {
					continue;
				}
				if (fileSwitchingExporter) {
					writer = getFileWriter(((SnomedFileSwitchingExporter)snomedExporter).getFileName(rows));
				}
				writer.write(line);
				writer.write(SnomedExporter.CR_LF);
			}
			
		} finally {
			
			for (Writer writ : releaseFileWriters.values()) {
				writ.close();
			}

			if (null != snomedExporter) {
				try {
					snomedExporter.close();
				} catch (final Exception e) {
					try {
						snomedExporter.close();
					} catch (final Exception e1) {
						e.addSuppressed(e1);
					}
					throw new SnowowlRuntimeException("Error while releasing exporter.", e);
				}
			}
		}
		
		return releaseFileRelativeDirectory;
	}
	
	private Writer getFileWriter(String filename) throws IOException {
		Writer writer = releaseFileWriters.get(filename);
		if (writer == null) {
			File file = new File(releaseFileRelativeDirectory, filename);
			boolean newFile = !file.exists();
			writer = new BufferedWriter(new FileWriter(file));
			if (newFile) {
				writeFileHeader(writer, snomedExporter.getColumnHeaders());
			}
			releaseFileWriters.put(filename, writer);
		}
		return writer;
	}
	
	private void writeFileHeader(final Writer writer, final String[] columnHeaders) throws IOException {
		writer.write(Joiner.on("\t").join(columnHeaders).trim());
		writer.write(SnomedExporter.CR_LF);
	}

	private boolean isToExport(final String[] split) {
		final String id = split[0];
		if ("id".equals(id)) { //header
			return true;
		}
		
		final String effectiveTime = split[1];
		final String idWithEffectiveTime = new StringBuilder(id).append(effectiveTime).toString();
		
		if (!visitedIdWithEffectiveTime.add(idWithEffectiveTime)) {
			return false;
		}
		
		final String moduleId = split[3];
		if (isModuleToExport(moduleId)) {
			if (snomedExporter instanceof AbstractSnomedRelationshipExporter) {
				if (isSourceAndDestinationRight(split)) {
					return true;
				} else {
					return false;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private boolean isModuleToExport(final String moduleId) {
		return modulesToExport.contains(moduleId);
	}
	
	private boolean isSourceAndDestinationRight(final String[] split) {
		final long moduleId = conceptIdToModuleIdSupplier.get().get(Long.parseLong(split[4])/*sourceModuleId*/);
		return modulesToExport.contains(String.valueOf(moduleId));
	}

	public File writeExtendedDescriptionTypeExplanation() throws IOException {
		final File extendedDescriptionTypePath = new File(
				temporaryWorkingDirectory + 
				File.separatorChar + 
				RELEASE_BASE_DIRECTORY + 
				clientNamespace + 
				File.separatorChar + 
				snomedExporter.getRelativeDirectory() + 
				File.separatorChar + 
				"Extended_Description_Type_Explanation.txt");
		
		if (extendedDescriptionTypePath.getParentFile() != null) {
			extendedDescriptionTypePath.getParentFile().mkdirs();
		}
		
		extendedDescriptionTypePath.createNewFile();
		
		final Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(extendedDescriptionTypePath), "UTF-8"));
		writer.write(getExtendedDescriptionTypeExplanationFileFormat());
		writer.close();
		
		return extendedDescriptionTypePath;
	}

	private String getExtendedDescriptionTypeExplanationFileFormat() {
		return new StringBuilder("TypeID\tDescription type\n")
		.append(" 1\tPreferred term\n")
		.append(" 2\tSynonym\n")
		.append(" 3\tFully specified name\n")
		.append(" 4\tFull name\n")
		.append(" 5\tAbbreviation\n")
		.append(" 6\tProduct term\n")
		.append(" 7\tShort name\n")
		.append(" 8\tPreferred plural\n")
		.append(" 9\tNote\n")
		.append("10\tSearch term\n")
		.append("11\tAbbreviation plural\n")
		.append("12\tProduct term plural\n")
		.toString();
	}
}