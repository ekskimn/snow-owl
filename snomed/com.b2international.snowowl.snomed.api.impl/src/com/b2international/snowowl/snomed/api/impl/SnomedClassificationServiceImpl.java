/*
 * Copyright 2011-2017 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.impl;

import static com.google.common.collect.Sets.newHashSet;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.domain.IComponentRef;
import com.b2international.snowowl.core.events.Notifications;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions;
import com.b2international.snowowl.datastore.remotejobs.RemoteJobEntry;
import com.b2international.snowowl.datastore.remotejobs.RemoteJobNotification;
import com.b2international.snowowl.datastore.request.job.JobRequests;
import com.b2international.snowowl.datastore.server.domain.StorageRef;
import com.b2international.snowowl.datastore.server.index.SingleDirectoryIndexManager;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.ISnomedClassificationService;
import com.b2international.snowowl.snomed.api.browser.ISnomedBrowserService;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.domain.classification.ClassificationStatus;
import com.b2international.snowowl.snomed.api.domain.classification.IClassificationRun;
import com.b2international.snowowl.snomed.api.domain.classification.IEquivalentConcept;
import com.b2international.snowowl.snomed.api.domain.classification.IEquivalentConceptSet;
import com.b2international.snowowl.snomed.api.domain.classification.IRelationshipChange;
import com.b2international.snowowl.snomed.api.domain.classification.IRelationshipChangeList;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationship;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationshipTarget;
import com.b2international.snowowl.snomed.api.impl.domain.browser.SnomedBrowserRelationshipType;
import com.b2international.snowowl.snomed.api.impl.domain.classification.ClassificationRun;
import com.b2international.snowowl.snomed.api.impl.domain.classification.EquivalentConcept;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.domain.SnomedConcepts;
import com.b2international.snowowl.snomed.core.domain.SnomedDescription;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.reasoner.classification.AbstractResponse.Type;
import com.b2international.snowowl.snomed.reasoner.classification.ClassificationSettings;
import com.b2international.snowowl.snomed.reasoner.classification.GetResultResponse;
import com.b2international.snowowl.snomed.reasoner.classification.PersistChangesResponse;
import com.b2international.snowowl.snomed.reasoner.classification.SnomedReasonerService;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

import io.reactivex.disposables.Disposable;

/**
 */
public class SnomedClassificationServiceImpl implements ISnomedClassificationService {

	private static final Logger LOG = LoggerFactory.getLogger(SnomedClassificationServiceImpl.class);
	
	private ClassificationRunIndex indexService;
	private ExecutorService executorService;
	private Disposable remoteJobSubscription;

	@Resource
	private SnomedBrowserService browserService;
	
	private SnomedBrowserService browserService() {
		return Preconditions.checkNotNull(browserService == null ? browserService = (SnomedBrowserService) ApplicationContext.getInstance().getServiceChecked(ISnomedBrowserService.class) : browserService, "browserService cannot be null!");
	}
	
	@Resource
	private IEventBus bus;
	
	@Resource
	private Integer maxReasonerRuns;

	private Integer maxReasonerRuns() {
		
		return maxReasonerRuns == null ? 
					maxReasonerRuns = ApplicationContext.getInstance().getServiceChecked(SnowOwlConfiguration.class).getModuleConfig(SnomedCoreConfiguration.class).getMaxReasonerRuns() 
				: 
					maxReasonerRuns;
	}
	
	private IEventBus bus() {
		return Preconditions.checkNotNull(bus == null ? bus = ApplicationContext.getInstance().getServiceChecked(IEventBus.class) : bus,  "bus cannot be null!");
	}
	
	private volatile boolean initialized = false;
	
	private void checkServices() {
		if (!initialized) 
			init();
	}
	
	@PostConstruct
	protected void init() {
		LOG.info("Initializing classification service; keeping indexed data for {} recent run(s).", maxReasonerRuns()); 
		
		final File dir = new File(new File(SnowOwlApplication.INSTANCE.getEnviroment().getDataDirectory(), "indexes"), "classification_runs");
		indexService = new ClassificationRunIndex(dir);
		ApplicationContext.getInstance().getServiceChecked(SingleDirectoryIndexManager.class).registerIndex(indexService);

		try {
			indexService.trimIndex(maxReasonerRuns());
			indexService.invalidateClassificationRuns();
		} catch (final IOException e) {
			LOG.error("Failed to run housekeeping tasks for the classification index.", e);
		}

		// TODO: common ExecutorService for asynchronous work?
		executorService = Executors.newCachedThreadPool();
		remoteJobSubscription = getNotifications()
				.ofType(RemoteJobNotification.class)
				.subscribe(this::onRemoteJobNotification);
		
		initialized = true;
	}
	
	private void onRemoteJobNotification(RemoteJobNotification notification) {
		if (!RemoteJobNotification.isChanged(notification)) {
			return;
		}
		
		JobRequests.prepareSearch()
		.all()
		.filterByIds(notification.getJobIds())
		.buildAsync()
		.execute(getEventBus())
		.then(remoteJobs -> {
			for (RemoteJobEntry remoteJob : remoteJobs) {
				onRemoteJobChanged(remoteJob);
			}
			return remoteJobs;
		});
	}

	private void onRemoteJobChanged(RemoteJobEntry remoteJob) {
		String type = (String) remoteJob.getParameters().get("type");
		
		switch (type) {
		case "ClassifyRequest":
			onClassifyJobChanged(remoteJob);
			break;
		case "PersistChangesRequest":
			onPersistJobChanged(remoteJob);
			break;
		default:
			break;
		}
	}
	
	private void onClassifyJobChanged(RemoteJobEntry remoteJob) {
		checkServices();
		try {
			
			switch (remoteJob.getState()) {
			case CANCELED:
				indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.CANCELED);
				break;
			case FAILED:
				indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.FAILED);
				break;
			case FINISHED: 
				onClassifyJobFinished(remoteJob);
				break;
			case RUNNING:
				indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.RUNNING);
				break;
			case SCHEDULED:
				indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.SCHEDULED);
				break;
			case CANCEL_REQUESTED:
				// Nothing to do for this state change
				break;
			default:
				throw new IllegalStateException(MessageFormat.format("Unexpected remote job state ''{0}''.", remoteJob.getState()));
			}
			
		} catch (final IOException e) {
			LOG.error("Caught IOException while updating classification status.", e);
		}
	}

	private void onClassifyJobFinished(RemoteJobEntry remoteJob) {
		checkServices();
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					
					final GetResultResponse result = getReasonerService().getResult(remoteJob.getId());
					final Type responseType = result.getType();
	
					switch (responseType) {
						case NOT_AVAILABLE: 
							indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.FAILED);
							break;
						case STALE: 
							indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.STALE);
							break;
						case SUCCESS:
							indexService.updateClassificationRunStatus(remoteJob.getId(), ClassificationStatus.COMPLETED, result.getChanges());
							break;
						default:
							throw new IllegalStateException(MessageFormat.format("Unexpected response type ''{0}''.", responseType));
					}
	
				} catch (final IOException e) {
					LOG.error("Caught IOException while registering classification data.", e);
				}
			}
		});
	}

	private void onPersistJobChanged(RemoteJobEntry remoteJob) {
		checkServices();
		try {

			String classificationJobId = (String) remoteJob.getParameters().get("classificationId");
			
			switch (remoteJob.getState()) {
			case CANCELED: //$FALL-THROUGH$
			case FAILED:
				indexService.updateClassificationRunStatus(classificationJobId, ClassificationStatus.SAVE_FAILED);
				break;
			case FINISHED: 
				indexService.updateClassificationRunStatus(classificationJobId, ClassificationStatus.SAVED);
				break;
			case RUNNING: //$FALL-THROUGH$
			case SCHEDULED: //$FALL-THROUGH$
			case CANCEL_REQUESTED:
				// Nothing to do for these state changes
				break;
			default:
				throw new IllegalStateException(MessageFormat.format("Unexpected remote job state ''{0}''.", remoteJob.getState()));
			}

		} catch (final IOException e) {
			LOG.error("Caught IOException while updating classification status after save.", e);
		}
	}

	@PreDestroy
	protected void shutdown() {
		if (null != remoteJobSubscription) {
			remoteJobSubscription.dispose();
			remoteJobSubscription = null;
		}

		if (null != executorService) {
			executorService.shutdown();
			executorService = null;
		}
		
		if (null != indexService) {
			ApplicationContext.getInstance().getServiceChecked(SingleDirectoryIndexManager.class).unregisterIndex(indexService);
			try {
				Closeables.close(indexService, true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			indexService = null;
		}
		
		LOG.info("Classification service shut down.");
	}

	private static SnomedReasonerService getReasonerService() {
		return ApplicationContext.getServiceForClass(SnomedReasonerService.class);
	}

	private static IEventBus getEventBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}
	
	private static Notifications getNotifications() {
		return ApplicationContext.getServiceForClass(Notifications.class);
	}

	@Override
	public List<IClassificationRun> getAllClassificationRuns(final String branchPath) {
		checkServices();
		final StorageRef storageRef = createStorageRef(branchPath);

		try {
			return indexService.getAllClassificationRuns(storageRef);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IClassificationRun beginClassification(final String branchPath, final String reasonerId, final String userId) {
		checkServices();
		final StorageRef storageRef = createStorageRef(branchPath);
		final IBranchPath oldBranchPath = storageRef.getBranch().branchPath();

		final ClassificationSettings settings = new ClassificationSettings(userId, oldBranchPath)
				.withParentContextDescription(DatastoreLockContextDescriptions.ROOT)
				.withReasonerId(reasonerId);

		final ClassificationRun classificationRun = new ClassificationRun();
		classificationRun.setId(settings.getClassificationId());
		classificationRun.setReasonerId(reasonerId);
		classificationRun.setCreationDate(new Date());
		classificationRun.setUserId(userId);
		classificationRun.setStatus(ClassificationStatus.SCHEDULED);
		
		try {
			indexService.upsertClassificationRun(oldBranchPath, classificationRun);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		
		getReasonerService().beginClassification(settings);
		return classificationRun;
	}

	@Override
	public IClassificationRun getClassificationRun(final String branchPath, final String classificationId) {
		checkServices();
		final StorageRef storageRef = createStorageRef(branchPath);
		
		try {
			return indexService.getClassificationRun(storageRef, classificationId);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<IEquivalentConceptSet> getEquivalentConceptSets(final String branchPath, final String classificationId, final List<ExtendedLocale> locales) {
		checkServices();
		// Check if it exists
		getClassificationRun(branchPath, classificationId);
		StorageRef storageRef = createStorageRef(branchPath);
		try {
			final List<IEquivalentConceptSet> conceptSets = indexService.getEquivalentConceptSets(storageRef, classificationId);
			final Set<String> conceptIds = newHashSet();
			
			for (final IEquivalentConceptSet conceptSet : conceptSets) {
				for (final IEquivalentConcept equivalentConcept : conceptSet.getEquivalentConcepts()) {
					conceptIds.add(equivalentConcept.getId());
				}
			}

			final Map<String, SnomedDescription> fsnMap = new DescriptionService(bus(), branchPath).getFullySpecifiedNames(conceptIds, locales);
			for (final IEquivalentConceptSet conceptSet : conceptSets) {
				for (final IEquivalentConcept equivalentConcept : conceptSet.getEquivalentConcepts()) {
					final String equivalentConceptId = equivalentConcept.getId();
					final SnomedDescription fsn = fsnMap.get(equivalentConceptId);
					if (fsn != null) {
						((EquivalentConcept) equivalentConcept).setLabel(fsn.getTerm());
					} else {
						((EquivalentConcept) equivalentConcept).setLabel(equivalentConceptId);
					}
				}
			}
			
			return conceptSets;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public IRelationshipChangeList getRelationshipChanges(final String branchPath, final String classificationId, final int offset, final int limit) {
		return getRelationshipChanges(branchPath, classificationId, null, offset, limit);
	}
	
	private IRelationshipChangeList getRelationshipChanges(String branchPath, String classificationId, String conceptId, int offset, int limit) {
		checkServices();
		// Check if it exists
		getClassificationRun(branchPath, classificationId);
		StorageRef storageRef = createStorageRef(branchPath);
		try {
			return indexService.getRelationshipChanges(storageRef, classificationId, conceptId, offset, limit);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ISnomedBrowserConcept getConceptPreview(String branchPath, String classificationId, String conceptId, List<ExtendedLocale> locales) {
		final IComponentRef componentRef = SnomedServiceHelper.createComponentRef(branchPath, conceptId);
		final SnomedBrowserConcept conceptDetails = (SnomedBrowserConcept) browserService.getConceptDetails(componentRef, locales);

		final List<ISnomedBrowserRelationship> relationships = Lists.newArrayList(conceptDetails.getRelationships());
		final IRelationshipChangeList relationshipChanges = getRelationshipChanges(branchPath, classificationId, conceptId, 0, 10000);

		/* 
		 * XXX: We don't want to match anything that is part of the inferred set below, so we remove relationships from the existing list, 
		 * all in advance. (Revisit should this assumption prove to be incorrect.)
		 */
		for (IRelationshipChange relationshipChange : relationshipChanges.getChanges()) {
			switch (relationshipChange.getChangeNature()) {
				case REDUNDANT:
					relationships.remove(findRelationship(relationships, relationshipChange));
					break;
				default:
					break;
			}
		}
		
		// Collect all concept representations that will be required for the conversion
		final Set<String> relatedIds = Sets.newHashSet();
		for (IRelationshipChange relationshipChange : relationshipChanges.getChanges()) {
			switch (relationshipChange.getChangeNature()) {
				case INFERRED:
					relatedIds.add(relationshipChange.getDestinationId());
					relatedIds.add(relationshipChange.getTypeId());
					break;
				default:
					break;
			}
		}
		
		final SnomedConcepts relatedConcepts = SnomedRequests.prepareSearchConcept()
				.setLimit(relatedIds.size())
				.filterByIds(relatedIds)
				.setLocales(locales)
				.setExpand("fsn()")
				.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath)
				.execute(bus())
				.getSync();
		
		final Map<String, SnomedConcept> relatedConceptsById = Maps.uniqueIndex(relatedConcepts, new Function<SnomedConcept, String>() {
			@Override public String apply(SnomedConcept input) { return input.getId(); }
		});
		
		final LoadingCache<SnomedConcept, SnomedBrowserRelationshipType> types = CacheBuilder.newBuilder().build(new CacheLoader<SnomedConcept, SnomedBrowserRelationshipType>() {
			@Override
			public SnomedBrowserRelationshipType load(SnomedConcept key) throws Exception {
				return browserService.convertBrowserRelationshipType(key);
			}
		});
		
		final LoadingCache<SnomedConcept, SnomedBrowserRelationshipTarget> targets = CacheBuilder.newBuilder().build(new CacheLoader<SnomedConcept, SnomedBrowserRelationshipTarget>() {
			@Override
			public SnomedBrowserRelationshipTarget load(SnomedConcept key) throws Exception {
				return browserService.convertBrowserRelationshipTarget(key);
			}
		});
		
		for (IRelationshipChange relationshipChange : relationshipChanges.getChanges()) {
			switch (relationshipChange.getChangeNature()) {
				case INFERRED:
					final SnomedBrowserRelationship inferred = new SnomedBrowserRelationship();
					
					// XXX: Default and/or not populated values are shown as commented lines below
					inferred.setActive(true);
					inferred.setCharacteristicType(CharacteristicType.INFERRED_RELATIONSHIP);
					// inferred.setEffectiveTime(null);
					inferred.setGroupId(relationshipChange.getGroup());
					inferred.setModifier(relationshipChange.getModifier());
					// inferred.setModuleId(null);
					// inferred.setRelationshipId(null);
					// inferred.setReleased(false);
					inferred.setSourceId(relationshipChange.getSourceId());
				
					SnomedConcept destinationConcept = relatedConceptsById.get(relationshipChange.getDestinationId());
					SnomedConcept typeConcept = relatedConceptsById.get(relationshipChange.getTypeId());
					inferred.setTarget(targets.getUnchecked(destinationConcept));
					inferred.setType(types.getUnchecked(typeConcept));

					relationships.add(inferred);
					break;
				default:
					break;
			}
		}
		
		// Replace immutable relationship list with preview
		conceptDetails.setRelationships(relationships);
		return conceptDetails;
	}

	private ISnomedBrowserRelationship findRelationship(List<ISnomedBrowserRelationship> relationships, IRelationshipChange relationshipChange) {
		for (ISnomedBrowserRelationship relationship : relationships) {
			if (relationship.isActive()
					&& relationship.getSourceId().equals(relationshipChange.getSourceId())
					&& relationship.getType().getConceptId().equals(relationshipChange.getTypeId())
					&& relationship.getTarget().getConceptId().equals(relationshipChange.getDestinationId())
					&& relationship.getGroupId() == relationshipChange.getGroup()
					&& relationship.getCharacteristicType().equals(CharacteristicType.INFERRED_RELATIONSHIP)
					&& relationship.getModifier().equals(relationshipChange.getModifier())) {					
				return relationship;
			}
		}
		return null;
	}

	@Override
	public void persistChanges(final String branchPath, final String classificationId, final String userId) {
		checkServices();
		// Check if it exists
		IClassificationRun classificationRun = getClassificationRun(branchPath, classificationId);

		if (!ClassificationStatus.COMPLETED.equals(classificationRun.getStatus())) {
			return;
		}

		final PersistChangesResponse persistChanges = getReasonerService().persistChanges(classificationId, userId);
		final ClassificationStatus saveStatus;

		switch (persistChanges.getType()) {
			case NOT_AVAILABLE:
			case STALE:
				saveStatus = ClassificationStatus.STALE;
				break;
			case SUCCESS:
				saveStatus = ClassificationStatus.SAVING_IN_PROGRESS;
				break;
			default:
				throw new IllegalStateException(MessageFormat.format("Unhandled persist change response type ''{0}''.", persistChanges.getType()));
		}
		
		try {
			indexService.updateClassificationRunStatus(classificationId, saveStatus);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void removeClassificationRun(final String branchPath, final String classificationId) {
		checkServices();
		JobRequests.prepareDelete(classificationId)
				.buildAsync()
				.execute(getEventBus())
				.then(ignored -> {
					try {
						indexService.deleteClassificationData(classificationId);
					} catch (IOException e) {
						LOG.error("Caught IOException while deleting classification data for ID {}.", classificationId, e);
					}
					return ignored;
				})
				.getSync();
		
	}

	private StorageRef createStorageRef(final String branchPath) {
		final StorageRef storageRef = new StorageRef(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath);
		storageRef.checkStorageExists();
		return storageRef;
	}
}
