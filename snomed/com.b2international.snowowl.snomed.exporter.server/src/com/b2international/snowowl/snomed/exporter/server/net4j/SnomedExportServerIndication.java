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
package com.b2international.snowowl.snomed.exporter.server.net4j;

import static com.b2international.snowowl.datastore.BranchPathUtils.createPath;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.net4j.signal.IndicationWithMonitoring;
import org.eclipse.net4j.signal.SignalProtocol;
import org.eclipse.net4j.util.io.ExtendedDataInputStream;
import org.eclipse.net4j.util.io.ExtendedDataOutputStream;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.b2international.commons.FileUtils;
import com.b2international.commons.time.TimeUtil;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.index.revision.RevisionIndexRead;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.LogUtils;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.Net4jProtocolConstants;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.CodeSystemService;
import com.b2international.snowowl.datastore.ICodeSystemVersion;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants;
import com.b2international.snowowl.snomed.common.ContentSubType;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSet;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedMapSetSetting;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.exporter.model.SnomedExportResult;
import com.b2international.snowowl.snomed.exporter.model.SnomedExportResult.Result;
import com.b2international.snowowl.snomed.exporter.server.SnomedExportContext;
import com.b2international.snowowl.snomed.exporter.server.SnomedExportContextImpl;
import com.b2international.snowowl.snomed.exporter.server.SnomedRefSetExporterFactory;
import com.b2international.snowowl.snomed.exporter.server.rf1.Id2Rf1PropertyMapper;
import com.b2international.snowowl.snomed.exporter.server.rf1.SnomedRf1ConceptExporter;
import com.b2international.snowowl.snomed.exporter.server.rf1.SnomedRf1DescriptionExporter;
import com.b2international.snowowl.snomed.exporter.server.rf1.SnomedRf1RelationshipExporter;
import com.b2international.snowowl.snomed.exporter.server.rf2.SnomedExporter;
import com.b2international.snowowl.snomed.exporter.server.rf2.SnomedInferredRelationshipExporter;
import com.b2international.snowowl.snomed.exporter.server.rf2.SnomedRf2ConceptExporter;
import com.b2international.snowowl.snomed.exporter.server.rf2.SnomedRf2DescriptionExporter;
import com.b2international.snowowl.snomed.exporter.server.rf2.SnomedStatedRelationshipExporter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

/**
 * This class receives requests from client side and depending the user request executes exports correspondingly.
 * 
 * The response is a zipped archive containing the exported files following the RF2 directory standard. The zipped archive and the working directory
 * can be found during the export in your system dependent temporary folder. After finishing the export and uploading the zipped file to the client
 * the working directory and the zipped archive will be deleted.
 * 
 */
public class SnomedExportServerIndication extends IndicationWithMonitoring {

	/* 
	 * XXX: reference equality (==) is required by AtomicReference, so use this exact string, not an equal empty one! The string contains 
	 * the text 'another user' to avoid confusing log messages if the publication finishes before the "winner" (the person who started
	 * an export before receiving this indication) can be retrieved from the AtomicReference. 
	 */
	private static final String NO_USER = "another user";

	private static final AtomicReference<String> ACTIVE_FULL_RF2_PUBLICATION_USER = new AtomicReference<String>(NO_USER);

	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SnomedExportServerIndication.class);

	private static final String RELEASE_ROOT_DIRECTORY_NAME = "SnomedCT_Release";
	
	private boolean coreComponentExport;
	private ContentSubType releaseType;
	private String unsetEffectiveTimeLabel;
	private boolean includeRf1;
	private boolean includeExtendedDescriptionTypes;
	private Set<String> modulesToExport;
	private Date startEffectiveTime;
	private Date endEffectiveTime;

	private List<SnomedReferenceSet> referenceSetsToExport;
	private Set<SnomedMapSetSetting> settings;

	// Used for logging
	private String userId;
	private IBranchPath branchPath;

	private SnomedExportResult result;

	private SnomedExportContext exportContext;

	// indicates whether the unpublished artifacts should be part of the export process
	private boolean includeUnpublished;

	// this is the directory where the exported files with the RF2 directory "standard" are put
	private Path tempDir;


	public SnomedExportServerIndication(SignalProtocol<?> protocol) {
		super(protocol, Net4jProtocolConstants.SNOMED_EXPORT_SIGNAL);
	}

	@Override
	protected int getIndicatingWorkPercent() {
		return 0;
	}

	@Override
	protected void indicating(ExtendedDataInputStream in, OMMonitor monitor) throws Exception {

		userId = in.readUTF();
		branchPath = createPath(in.readUTF());
		
		String startEffectiveTimeString = in.readUTF();
		String endEffectiveTimeString = in.readUTF();
		startEffectiveTime = startEffectiveTimeString.equals("") ? null : convertRF2StringToDate(startEffectiveTimeString);
		endEffectiveTime = endEffectiveTimeString.equals("") ? null : convertRF2StringToDate(endEffectiveTimeString);
		
		releaseType = ContentSubType.getByValue(in.readInt());
		unsetEffectiveTimeLabel = in.readUTF();
		includeUnpublished = in.readBoolean();
		
		includeRf1 = in.readBoolean();
		includeExtendedDescriptionTypes = in.readBoolean();

		coreComponentExport = in.readBoolean();
		
		final int numberOfRefSetsToExport = in.readInt();
		Set<String> refsetIdentifierConcepts = numberOfRefSetsToExport > 0 ? Sets.<String> newHashSetWithExpectedSize(numberOfRefSetsToExport)
				: Collections.<String> emptySet();
		for (int i = 0; i < numberOfRefSetsToExport; i++) {
			refsetIdentifierConcepts.add(in.readUTF());
		}
		
		referenceSetsToExport = refsetIdentifierConcepts.isEmpty() ? Collections.<SnomedReferenceSet>emptyList() : SnomedRequests.prepareSearchRefSet()
				.all()
				.setComponentIds(refsetIdentifierConcepts)
				.build(branchPath.getPath())
				.executeSync(getEventBus()).getItems();
		
		final int settingSize = in.readInt();
		settings = settingSize > 0 ? Sets.<SnomedMapSetSetting>newHashSetWithExpectedSize(settingSize) : Collections.<SnomedMapSetSetting>emptySet();
		for (int i = 0; i < settingSize; i++) {
			settings.add(SnomedMapSetSetting.read(in));
		}
		
		final int modulesToExportSize = in.readInt();
		modulesToExport = modulesToExportSize > 0 ? Sets.<String>newHashSetWithExpectedSize(modulesToExportSize) : Collections.<String>emptySet();
		for (int i = 0; i < modulesToExportSize; i++) {
			modulesToExport.add(in.readUTF());
		}
		
		String namespace = in.readUTF();
		
		tempDir = Files.createTempDirectory("export");
		
		exportContext = new SnomedExportContextImpl(
				branchPath, 
				releaseType, 
				unsetEffectiveTimeLabel,
				startEffectiveTime, 
				endEffectiveTime,
				modulesToExport,
				new Id2Rf1PropertyMapper(),
				getReleaseRootPath(tempDir, namespace));
		
		logActivity(String.format("SNOMED CT %s export has been requested", releaseType.getDisplayName()));
	}

	@Override
	protected void responding(ExtendedDataOutputStream out, final OMMonitor monitor) throws Exception {

		File file = null;
		
		result = new SnomedExportResult();

		try {
			
			Stopwatch stopwatch = Stopwatch.createStarted();
			
			monitor.begin(calculateProgressMonitorStep());
			
			checkOtherPublication();
			
			if (Result.IN_PROGRESS != result.getResult()) {
				
				// obtain the index service here to ensure a consistent view of the data during the export process
				RepositoryManager repositoryManager = ApplicationContext.getInstance().getService(RepositoryManager.class);
				RevisionIndex revisionIndex = repositoryManager.get(SnomedDatastoreActivator.REPOSITORY_UUID).service(RevisionIndex.class);
				file = doExport(revisionIndex, monitor);
				
				logActivity("Transferring export result...");
				
				sendResult(out, file, monitor);
				
				logActivity(String.format("SNOMED CT export finished in %s", TimeUtil.toString(stopwatch)));

				monitor.worked(1);
			}
			
		}  catch (Exception e) {
			
			final String reason = null != e.getMessage() ? " Reason: '" + e.getMessage() + "'" : "";
			logActivity("Caught exception while exporting SNOMED CT terminology." + reason);
			
			if (e.getClass().isAssignableFrom(RuntimeException.class)) {
				result.setResultAndMessage(Result.EXCEPTION, "An error occurred while exporting SNOMED CT components: could not retrieve data from database.");
			} else if (e.getClass().isAssignableFrom(IOException.class)) {
				result.setResultAndMessage(Result.EXCEPTION, "An error occurred while exporting SNOMED CT components: could not create release files.");
			}
			
		} finally {

			monitor.done();
			
			if (null != file) {
				file.delete();
			}
			
			/* 
			 * If we couldn't set userId on the AtomicReference at the beginning somehow, this will have no effect, which is good -- we 
			 * don't want to destroy another user's export directory if currentTimeMillis returned the same value for both users, for example.
			 */
			if (ACTIVE_FULL_RF2_PUBLICATION_USER.compareAndSet(userId, NO_USER)) {
				FileUtils.deleteDirectory(tempDir.toFile());
			}
		}
	}

	private void sendResult(ExtendedDataOutputStream out, File file, OMMonitor monitor) throws IOException {
		
		out.writeObject(result);
		
		if (Result.SUCCESSFUL == result.getResult()) {
			long size = file.length();
			BufferedInputStream in = null;

			monitor.fork(size);

			out.writeLong(size);

			try {
				in = new BufferedInputStream(new FileInputStream(file));
				while (size != 0L) {
					int chunk = Net4jProtocolConstants.BUFFER_SIZE;
					if (size < Net4jProtocolConstants.BUFFER_SIZE) {
						chunk = (int) size;
					}

					monitor.worked(chunk / 1.0);

					byte[] buffer = new byte[chunk];
					in.read(buffer);
					out.writeByteArray(buffer);

					size -= chunk;
				}
			} finally {
				in.close();
			}
		}
		
	}

	private void checkOtherPublication() {
		if (coreComponentExport) {
			if (!ACTIVE_FULL_RF2_PUBLICATION_USER.compareAndSet(NO_USER, userId)) {
				final String publishingUserId = ACTIVE_FULL_RF2_PUBLICATION_USER.get();
				
				logActivity(String.format("SNOMED CT export is already in progress by %s.", publishingUserId));
				
				result.setResult(Result.IN_PROGRESS);
			}
		}
	}
	
	private File doExport(final RevisionIndex revisionIndex, final OMMonitor monitor) throws Exception {
		
		switch (exportContext.getContentSubType()) {
			case DELTA:
				executeDeltaExport(revisionIndex, monitor);
				break;
			case SNAPSHOT:
				executeSnapshotExport(revisionIndex, monitor);
				break;
			case FULL:
				executeFullExport(revisionIndex, monitor);
				break;
		}
		
		logActivity("Creating SNOMED CT export archive...");
		File zipFile = FileUtils.createZipArchive(tempDir.toFile(), Files.createTempFile("export", ".zip").toFile());

		if (monitor.isCanceled()) {
			processCancel();
			return null;
		} else {
			monitor.worked(1);
		}

		return zipFile;
	}
	
	
	private void executeDeltaExport(final RevisionIndex revisionIndex, final OMMonitor monitor) {
		
		if (startEffectiveTime == null && endEffectiveTime == null) {
			
			executeExport(revisionIndex, branchPath.getPath(), true, monitor);
			
		} else {
			
			List<ICodeSystemVersion> sortedVersions = FluentIterable.from(getAllCodeSystemVersions()).filter(new Predicate<ICodeSystemVersion>() {
				@Override
				public boolean apply(ICodeSystemVersion input) {
					
					Date versionEffectiveDate = new Date(input.getEffectiveDate());
	
					if (startEffectiveTime != null && endEffectiveTime != null) {
						return (versionEffectiveDate.after(startEffectiveTime) || versionEffectiveDate.equals(startEffectiveTime))
								&& (versionEffectiveDate.before(endEffectiveTime) || versionEffectiveDate.equals(endEffectiveTime));
					} else if (startEffectiveTime == null) {
						return versionEffectiveDate.before(endEffectiveTime) || versionEffectiveDate.equals(endEffectiveTime);
					} else if (endEffectiveTime == null) {
						return versionEffectiveDate.after(startEffectiveTime) || versionEffectiveDate.equals(startEffectiveTime);
					}
					
					return false;
				}
				
			}).toSortedList(new Comparator<ICodeSystemVersion>() {
				@Override
				public int compare(ICodeSystemVersion o1, ICodeSystemVersion o2) {
					return Longs.compare(o1.getEffectiveDate(), o2.getEffectiveDate());
				}
			});
			
			if (sortedVersions.isEmpty()) {
				String message = null;
				if (startEffectiveTime != null && endEffectiveTime != null) {
					message = String.format("No version branch found to export between the effective dates %s - %s", 
							Dates.formatByHostTimeZone(startEffectiveTime), Dates.formatByHostTimeZone(endEffectiveTime));
				} else if (startEffectiveTime == null) {
					message = String.format("No version branch found to export before the effective date %s", 
							Dates.formatByHostTimeZone(endEffectiveTime));
				} else if (endEffectiveTime == null) {
					message = String.format("No version branch found to export after the effective date %s", 
							Dates.formatByHostTimeZone(startEffectiveTime));
				}
				throw new BadRequestException(message);
			}
			
			List<String> versionBranchPaths = convertToBranchPaths(sortedVersions);
			
			for (String versionBranchPath : versionBranchPaths) {
				executeExport(revisionIndex, versionBranchPath, false, monitor);
			}

			if (includeUnpublished) {
				executeExport(revisionIndex, branchPath.getPath(), true, monitor);
			}
		}
		
	}

	private void executeSnapshotExport(RevisionIndex revisionIndex, OMMonitor monitor) {
		
		executeExport(revisionIndex, branchPath.getPath(), false, monitor);
		
		if (includeUnpublished) {
			executeExport(revisionIndex, branchPath.getPath(), true, monitor);
		}
		
	}

	private void executeFullExport(RevisionIndex revisionIndex, OMMonitor monitor) {
		
		List<ICodeSystemVersion> sortedVersions = FluentIterable.from(getAllCodeSystemVersions()).toSortedList(new Comparator<ICodeSystemVersion>() {
			@Override
			public int compare(ICodeSystemVersion o1, ICodeSystemVersion o2) {
				return Longs.compare(o1.getEffectiveDate(), o2.getEffectiveDate());
			}
		});
		
		long startTime = 0L;
		
		for (ICodeSystemVersion version : sortedVersions) {
			
			exportContext.setStartEffectiveTime(new Date(startTime));
			exportContext.setEndEffectiveTime(new Date(version.getEffectiveDate()));
			
			String versionBranchPath = convertToBranchPath(version);
			executeExport(revisionIndex, versionBranchPath, false, monitor);
			
			startTime = version.getEffectiveDate();
		}
		
		if (includeUnpublished) {
			executeExport(revisionIndex, branchPath.getPath(), true, monitor);
		}
		
	}

	private void executeExport(final RevisionIndex revisionIndex, final String versionBranchPath, final boolean unpublishedExport, final OMMonitor monitor) {
		
		exportContext.setUnpublishedExport(unpublishedExport);
		
		revisionIndex.read(versionBranchPath, new RevisionIndexRead<Void>() {
	
			@Override
			public Void execute(RevisionSearcher revisionSearcher) throws IOException {
	
				if (monitor.isCanceled()) {
					processCancel();
					return null;
				}
				
				if (coreComponentExport) {
					logActivity(String.format("Starting export of %score components from branch path '%s'",
							unpublishedExport ? "unpublished " : "", versionBranchPath));
					executeCoreExport(revisionSearcher, monitor);
				}
				
				if (!referenceSetsToExport.isEmpty()) {
					
					logActivity(String.format("Starting export of %sreference sets from branch path '%s'", 
							unpublishedExport ? "unpublished " : "", versionBranchPath));
					
					for (SnomedReferenceSet referenceSet : referenceSetsToExport) {
						
						if (monitor.isCanceled()) {
							processCancel();
							return null;
						}
						
						executeRefSetExport(referenceSet, revisionSearcher, monitor);
					}
				}
				
				return null;
			}
			
		});
		
	}

	private void executeCoreExport(final RevisionSearcher revisionSearcher, final OMMonitor monitor) throws IOException {
	
		logActivity(String.format("Exporting %sSNOMED CT concepts into RF2 format", exportContext.isUnpublishedExport() ? "unpublished " : ""));
		new SnomedRf2ConceptExporter(exportContext, revisionSearcher).execute();
		
		if (monitor.isCanceled()) {
			return;
		} else {
			monitor.worked(2);
		}
		
		logActivity(String.format("Exporting %sSNOMED CT descriptions into RF2 format", exportContext.isUnpublishedExport() ? "unpublished " : ""));
		new SnomedRf2DescriptionExporter(exportContext, revisionSearcher).execute();
		
		if (monitor.isCanceled()) {
			return;
		} else {
			monitor.worked(2);
		}
		
		logActivity(String.format("Exporting non-stated %sSNOMED CT relationships into RF2 format", exportContext.isUnpublishedExport() ? "unpublished " : ""));
		new SnomedInferredRelationshipExporter(exportContext, revisionSearcher).execute();
		
		if (monitor.isCanceled()) {
			return;
		} else {
			monitor.worked(2);
		}
		
		logActivity(String.format("Exporting stated %sSNOMED CT relationships into RF2 format", exportContext.isUnpublishedExport() ? "unpublished " : ""));
		new SnomedStatedRelationshipExporter(exportContext, revisionSearcher).execute();
		
		if (monitor.isCanceled()) {
			return;
		} else {
			monitor.worked(2);
		}
		
		if (!exportContext.isUnpublishedExport() && includeRf1) {
			
			logActivity("Exporting SNOMED CT concepts into RF1 format");
			new SnomedRf1ConceptExporter(exportContext, revisionSearcher).execute();
			
			if (monitor.isCanceled()) {
				return;
			} else {
				monitor.worked(2);
			}
			
			logActivity("Exporting SNOMED CT descriptions into RF1 format");
			new SnomedRf1DescriptionExporter(exportContext, revisionSearcher, includeExtendedDescriptionTypes).execute();
			
			if (monitor.isCanceled()) {
				return;
			} else {
				monitor.worked(2);
			}
			
			logActivity("Exporting SNOMED CT relationships into RF1 format");
			new SnomedRf1RelationshipExporter(exportContext, revisionSearcher).execute();
			
			if (monitor.isCanceled()) {
				return;
			} else {
				monitor.worked(2);
			}
		}
	}

	private void executeRefSetExport(final SnomedReferenceSet refset, final RevisionSearcher revisionSearcher, final OMMonitor monitor) throws IOException {
		
		final SnomedExporter refSetExporter = SnomedRefSetExporterFactory.getRefSetExporter(refset, exportContext, revisionSearcher);
		
		logActivity(String.format("Exporting SNOMED CT reference set into RF2 format. Reference set identifier concept ID: %s", refset.getId()));
		refSetExporter.execute();
	
		if (!exportContext.isUnpublishedExport() && includeRf1) {
			
			logActivity("Exporting SNOMED CT reference set into RF1 format. Reference set identifier concept ID: " + refset.getId());
			
			// RF1 subset exporter.
			Iterable<SnomedExporter> subsetExporters = SnomedRefSetExporterFactory.getSubsetExporter(refset, exportContext, revisionSearcher);
			
			for (final SnomedExporter exporter : subsetExporters) {
				exporter.execute();
			}
			
			// RF1 map set exporter.
			final SnomedMapSetSetting mapsetSetting = getSettingForRefSet(refset.getId());
			if (null != mapsetSetting) {
				Iterable<SnomedExporter> crossMapExporters = SnomedRefSetExporterFactory.getCrossMapExporter(refset, exportContext, mapsetSetting, revisionSearcher);
				for (final SnomedExporter exporter : crossMapExporters) {
					exporter.execute();
				}
			}
		}
		
		monitor.worked(1);
	}

	private List<String> convertToBranchPaths(List<ICodeSystemVersion> sortedVersions) {
		return FluentIterable.from(sortedVersions).transform(new Function<ICodeSystemVersion, String>() {
			@Override
			public String apply(ICodeSystemVersion input) {
				return convertToBranchPath(input);
			}
		}).toList();
	}
	
	private String convertToBranchPath(ICodeSystemVersion version) {
		// FIXME when MS changes are merged
		return BranchPathUtils.createPath(BranchPathUtils.createMainPath(), version.getVersionId()).getPath();
	}

	private void processCancel() {
		logActivity("SNOMED CT export canceled.");
		result.setResult(Result.CANCELED);
	}

	private int calculateProgressMonitorStep() {
		int counter = 0;

		if (coreComponentExport) {
			counter += 8;
			if (includeRf1) {
				counter += 6;
			}
		}

		counter += referenceSetsToExport.size();

		counter++; // compressing zip
		counter++; // sending file to the client;

		return counter;
	}

	private Date convertRF2StringToDate(String dateInRF2Format) {
		try {
			return EffectiveTimes.parse(dateInRF2Format, SnomedConstants.RF2_EFFECTIVE_TIME_FORMAT);
		} catch (SnowowlRuntimeException e) {
			LOGGER.error(String.format("Couldn't parse RF2 date %s.", dateInRF2Format), e);
			throw new BadRequestException("Couldn't parse RF2 date %s.", dateInRF2Format);
		}
	}
	
	/*returns with the map set setting for the specified reference set identifier concept ID*/
	private SnomedMapSetSetting getSettingForRefSet(final String refSetId) {
		return Iterables.getOnlyElement(Iterables.filter(settings, new Predicate<SnomedMapSetSetting>() {
			@Override public boolean apply(final SnomedMapSetSetting setting) {
				return setting.getRefSetId().equals(refSetId);
			}
		}), null);
	}
	
	private void logActivity(final String message) {
		LogUtils.logExportActivity(LOGGER, userId, branchPath, message);
	}
	
	private Collection<ICodeSystemVersion> getAllCodeSystemVersions() {
		return ApplicationContext.getServiceForClass(CodeSystemService.class).getAllTags(SnomedDatastoreActivator.REPOSITORY_UUID);
	}
	
	private IEventBus getEventBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}

	private Path getReleaseRootPath(Path tempDir, String namespace) {
		String dirName = Strings.isNullOrEmpty(namespace) ? RELEASE_ROOT_DIRECTORY_NAME : String.format("%s_%s", RELEASE_ROOT_DIRECTORY_NAME, namespace);
		return Paths.get(tempDir.toString(), dirName);
	}
}