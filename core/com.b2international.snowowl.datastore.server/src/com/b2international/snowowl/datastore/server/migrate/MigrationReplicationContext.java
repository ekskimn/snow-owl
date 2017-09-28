/*
 * Copyright 2017 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.migrate;

import static com.google.common.collect.Maps.newHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.groovy.control.CompilationFailedException;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.internal.server.DelegatingRepository;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.common.CDOReplicationContext;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.spi.server.InternalTransaction;
import org.eclipse.net4j.db.DBException;
import org.eclipse.net4j.util.om.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.index.IndexException;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.datastore.cdo.CDOCommitInfoUtils;
import com.b2international.snowowl.datastore.cdo.DelegatingTransaction;
import com.b2international.snowowl.datastore.replicate.BranchReplicator;
import com.b2international.snowowl.datastore.replicate.BranchReplicator.SkipBranchException;
import com.b2international.snowowl.datastore.server.reindex.OptimizeRequest;
import com.b2international.snowowl.datastore.server.reindex.PurgeRequest;
import com.b2international.snowowl.terminologymetadata.TerminologymetadataPackage;
import com.google.common.collect.Sets;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * @since 5.10.12
 */
@SuppressWarnings("restriction")
class MigrationReplicationContext implements CDOReplicationContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationReplicationContext.class);

	private final RepositoryContext context;
	private final long initialLastCommitTime;
	private final int initialBranchId;
	private final InternalSession replicatorSession;
	
	private TreeMap<Long, CDOBranch> branchesByBasetimestamp = new TreeMap<>();
	
	private int skippedCommits = 0;
	private int processedCommits = 0;
	private long failedCommitTimestamp = -1;
	private final Set<Integer> skippedBranches = Sets.newHashSet();

	private Exception exception;

	private boolean optimize = false;

	private final GroovyShell shell = new GroovyShell();
	private final Class<? extends Script> scriptClass;

	MigrationReplicationContext(final RepositoryContext context, final int initialBranchId, final long initialLastCommitTime, final InternalSession session, final String scriptLocation) {
		this.context = context;
		this.initialBranchId = initialBranchId;
		this.initialLastCommitTime = initialLastCommitTime;
		this.replicatorSession = session;
		try {
			this.scriptClass = shell.getClassLoader().parseClass(new File(scriptLocation));
		} catch (CompilationFailedException | IOException e) {
			throw new SnowowlRuntimeException("Couldn't compile script", e);
		}
	}

	@Override
	public void handleCommitInfo(final CDOCommitInfo commitInfo) {
		// skip commits by CDO_SYSTEM user
		if (CDOCommitInfoUtils.CDOCommitInfoQuery.EXCLUDED_USERS.contains(commitInfo.getUserID())
				|| commitInfo.getComment().equals("Create terminology and metadata content storage for repository")
				|| commitInfo.getComment().contains("Create primary code system for repository")
				|| commitInfo.getComment().contains("Create terminology content wrapper for repository")) {
			return;
		}
		
		if (failedCommitTimestamp != -1) {
			skippedCommits++;
			return;
		}
		
		final long commitTimestamp = commitInfo.getTimeStamp();
		
		Entry<Long, CDOBranch> lastBranchToReplicateBeforeCommit = branchesByBasetimestamp.floorEntry(commitTimestamp);
		if (lastBranchToReplicateBeforeCommit != null) {
			// get the first entry and use the base of that branch for purge
			Entry<Long, CDOBranch> currentBranchToReplicate = branchesByBasetimestamp.firstEntry();
			
			// before creating the branches, execute a purge on the latest segment of the parent branch
			PurgeRequest.builder()
				.setBranchPath(currentBranchToReplicate.getValue().getBase().getBranch().getPathName())
				.build()
				.execute(context);
			
			// replicate all branches created before the current commit in order
			do {
				final CDOBranch branch = currentBranchToReplicate.getValue();
				LOGGER.info("Replicating branch: " + branch.getName() + " at " + branch.getBase().getTimeStamp());
				try {
					context.service(BranchReplicator.class).replicateBranch(branch);
				} catch (SkipBranchException e) {
					LOGGER.warn("Skipping branch with all of its commits: {}", branch.getID());
					skippedBranches.add(branch.getID());
				}
				branchesByBasetimestamp.remove(currentBranchToReplicate.getKey());
				
				// check if there are more branches to create until this point
				currentBranchToReplicate = branchesByBasetimestamp.firstEntry();
			} while (currentBranchToReplicate != null && currentBranchToReplicate.getKey() <= lastBranchToReplicateBeforeCommit.getKey());
			
			if (optimize) {
				optimize();
				optimize = false;
			}
		}
		
		try {
			if (isVersionCommit(commitInfo)) {
				// optimize the index next time we create the version branch
				this.optimize = true;
			}
			
			if (skippedBranches.contains(commitInfo.getBranch().getID())) {
				skippedCommits++;
				return;
			}
			
			LOGGER.info("Replicating commit: " + commitInfo.getComment() + " at " + commitInfo.getBranch().getName() + "@" + commitTimestamp);	
		} catch (DBException e) {
			skippedCommits++;
			
			if (e.getMessage().startsWith("Branch with ID")) {
				LOGGER.warn("Skipping commit with missing branch: " + commitInfo.getComment() + " at " + commitInfo.getBranch().getID() + "@" + commitTimestamp);
				return;
			} else {
				failedCommitTimestamp = commitTimestamp;
				this.exception = e;
			}
		}
		
		final InternalRepository repository = replicatorSession.getManager().getRepository();
		final InternalRepository delegateRepository = new DelegatingRepository() {
			@Override
			protected InternalRepository getDelegate() {
				return repository;
			}
			
			@Override
			public void failCommit(long timestamp) {
				failedCommitTimestamp = timestamp;
				skippedCommits++;
			}
		};
		
		// this is not the actual HEAD of the particular branch!!
		CDOBranch branch = commitInfo.getBranch();
		CDOBranchPoint head = branch.getHead();

		InternalTransaction transaction = replicatorSession.openTransaction(InternalSession.TEMP_VIEW_ID, head);
		DelegatingTransaction delegatingTransaction = new DelegatingTransaction(transaction) {
			//Transaction needs to return the delegating repository as well
			@Override
			public InternalRepository getRepository() {
				return delegateRepository;
			}
		};

		MigratingCommitContext commitContext = new MigratingCommitContext(delegatingTransaction, commitInfo);

		// run a custom groovy script to manipulate each commit data before committing it
		final Map<String, Object> ctx = newHashMap();
		ctx.put("ctx", commitContext);
		final Binding binding = new Binding(ctx);
		try {
			Script script = scriptClass.newInstance();
			script.setBinding(binding);
			script.run();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IndexException("Couldn't instantiate groovy script class", e);
		}

		commitContext.preWrite();
		boolean success = false;
		
		try {
			commitContext.write(new Monitor());
			commitContext.commit(new Monitor());
			success = true;
			processedCommits++;
		} finally {
			commitContext.postCommit(success);
			transaction.close();
			StoreThreadLocal.setSession(replicatorSession);
		}
	}

	private boolean isVersionCommit(final CDOCommitInfo commitInfo) {
		for (CDOIDAndVersion newObject : commitInfo.getNewObjects()) {
			if (newObject instanceof CDORevision) {
				if (TerminologymetadataPackage.Literals.CODE_SYSTEM_VERSION.isSuperTypeOf(((CDORevision) newObject).getEClass())) {
					return true;
				}
			}
		}
		for (CDORevisionKey changedObject : commitInfo.getChangedObjects()) {
			if (changedObject instanceof CDORevision) {
				if (TerminologymetadataPackage.Literals.CODE_SYSTEM_VERSION.isSuperTypeOf(((CDORevision) changedObject).getEClass())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void optimize() {
		OptimizeRequest.builder().setMaxSegments(8).build().execute(context);
	}

	@Override
	public int getLastReplicatedBranchID() {
		return initialBranchId;
	}

	@Override
	public long getLastReplicatedCommitTime() {
		return initialLastCommitTime;
	}

	@Override
	public String[] getLockAreaIDs() {
		return new String[] {};
	}

	@Override
	public void handleBranch(CDOBranch branch) {
		branchesByBasetimestamp.put(branch.getBase().getTimeStamp(), branch);
	}

	@Override
	public boolean handleLockArea(LockArea area) {
		return false;
	}
	
	public long getFailedCommitTimestamp() {
		return failedCommitTimestamp;
	}
	
	public int getSkippedCommits() {
		return skippedCommits;
	}
	
	public int getProcessedCommits() {
		return processedCommits;
	}
	
	public Exception getException() {
		return exception;
	}


}
