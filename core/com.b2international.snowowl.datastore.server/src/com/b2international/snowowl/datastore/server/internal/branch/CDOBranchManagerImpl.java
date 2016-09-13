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
package com.b2international.snowowl.datastore.server.internal.branch;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.transaction.CDOMerger;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;

import com.b2international.commons.Pair;
import com.b2international.index.IndexWrite;
import com.b2international.index.Writer;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Query;
import com.b2international.snowowl.core.Metadata;
import com.b2international.snowowl.core.MetadataImpl;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.branch.BranchMergeException;
import com.b2international.snowowl.core.exceptions.MergeConflictException;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.core.merge.MergeConflict;
import com.b2international.snowowl.core.users.SpecialUserStore;
import com.b2international.snowowl.datastore.cdo.CDOUtils;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.events.BranchChangedEvent;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions;
import com.b2international.snowowl.datastore.replicate.BranchReplicator;
import com.b2international.snowowl.datastore.server.CDOServerCommitBuilder;
import com.b2international.snowowl.datastore.server.internal.InternalRepository;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * {@link BranchManager} implementation based on {@link CDOBranch} functionality.
 *
 * @since 4.1
 */
public class CDOBranchManagerImpl extends BranchManagerImpl implements BranchReplicator {

    private static final String CDO_BRANCH_ID = "cdoBranchId";

	private final InternalRepository repository;
	private final AtomicInteger segmentIds = new AtomicInteger(0);
	
    public CDOBranchManagerImpl(final InternalRepository repository) {
        super(repository.getIndex());
        this.repository = repository;
       	
        final CDOBranch cdoMainBranch = repository.getCdoMainBranch();
      	final long baseTimestamp = repository.getBaseTimestamp(cdoMainBranch);
      	// assign first segment to MAIN
      	final int segmentId = segmentIds.getAndIncrement();
		initBranchStore(new CDOMainBranchImpl(baseTimestamp, repository.getHeadTimestamp(cdoMainBranch), new MetadataImpl(), segmentId, ImmutableSet.of(segmentId)));
       	
		int maxExistingSegment = segmentId;
		for (Branch branch : getBranches()) {
			if (branch instanceof InternalCDOBasedBranch) {
				final int branchSegmentId = ((InternalCDOBasedBranch) branch).segmentId();
				if (branchSegmentId > maxExistingSegment) {
					maxExistingSegment = branchSegmentId;
				}
			}
		}
		segmentIds.set(maxExistingSegment+1);
    }
    
    private synchronized Pair<Integer, Integer> nextTwoSegments() {
    	final int newBranchSegment = segmentIds.getAndIncrement();
    	final int newParentSegment = segmentIds.getAndIncrement();
    	return Pair.of(newBranchSegment, newParentSegment);
    }
    
    @Override
    public void replicateBranch(final org.eclipse.emf.cdo.common.branch.CDOBranch branch) {
		if (!branch.isMainBranch()) {
			final int cdoBranchId = branch.getID();
			// if content already available with this cdoBranchId then skip
			Branch existingBranch = getBranch(cdoBranchId);
			
			if (existingBranch == null) {
				try {
					// get existing branch by name, this can be null, if this is the first replication of the branch
					existingBranch = getBranch(branch.getPathName());
				} catch (NotFoundException e) {
					// ignore not found branches
				}
				final int parentCdoBranchId = branch.getBase().getBranch().getID();
				final InternalCDOBasedBranch parent = (InternalCDOBasedBranch) getBranch(parentCdoBranchId);
				if (parent == null) {
					throw new SkipBranchException(branch);
				}
				final String name = branch.getName();
				final long baseTimestamp = repository.getBaseTimestamp(branch);
				final long headTimestamp = repository.getHeadTimestamp(branch);
				reopen(parent, name, existingBranch == null ? new MetadataImpl() : existingBranch.metadata(), baseTimestamp, headTimestamp, cdoBranchId);
			}
		}
    }

    CDOBranch getCDOBranch(Branch branch) {
        checkArgument(!branch.isDeleted(), "Deleted branches cannot be retrieved.");
        final int branchId = ((InternalCDOBasedBranch) branch).cdoBranchId();
        return loadCDOBranch(branchId);
    }

    private Branch getBranch(Integer branchId) {
    	return getBranchFromStore(Query.select(InternalBranch.class).where(Expressions.match(CDO_BRANCH_ID, branchId)));
    }
    
    private CDOBranch loadCDOBranch(Integer branchId) {
        return repository.getCdoBranchManager().getBranch(branchId);
    }

    @Override
    InternalBranch rebase(InternalBranch branch, InternalBranch onTopOf, String commitMessage, Runnable postReopen) {
		CDOTransaction testTransaction = null;
		CDOTransaction newTransaction = null;
		
		try {
			testTransaction = applyChangeSet(branch, onTopOf, true);
			final InternalBranch rebasedBranch = reopen(onTopOf, branch.name(), branch.metadata());
			postReopen.run();
			
			if (branch.headTimestamp() > branch.baseTimestamp()) {
				newTransaction = transferChangeSet(testTransaction, rebasedBranch);
				 // Implicit notification (reopen & commit)
				return commitChanges(branch, rebasedBranch, commitMessage, newTransaction);
			} else {
				sendChangeEvent(rebasedBranch.path()); // Explicit notification (reopen)
				return rebasedBranch;
			}
			
		} finally {
			LifecycleUtil.deactivate(testTransaction);
			LifecycleUtil.deactivate(newTransaction);
		}
    }
    
    private CDOTransaction transferChangeSet(final CDOTransaction transaction, final InternalBranch rebasedBranch) {
		final ICDOConnection connection = repository.getConnection();
		final CDOBranch rebasedCdoBranch = getCDOBranch(rebasedBranch);
		final CDOTransaction newTransaction = connection.createTransaction(rebasedCdoBranch);
		
		CDOUtils.mergeTransaction(transaction, newTransaction);
		return newTransaction;
	}
    
    @Override
    InternalBranch applyChangeSet(InternalBranch from, InternalBranch to, boolean dryRun, boolean isRebase, String commitMessage) {
        final CDOTransaction dirtyTransaction = applyChangeSet(from, to, isRebase);
        try {
        	if (dryRun) {
            	return to;
            } else {
            	return commitChanges(from, to, commitMessage, dirtyTransaction);
            }
		} finally {
			LifecycleUtil.deactivate(dirtyTransaction);
		}
    }
    
    private CDOTransaction applyChangeSet(InternalBranch from, InternalBranch to, boolean isRebase) {
    	final CDOBranch sourceBranch = getCDOBranch(from);
    	final CDOBranch targetBranch = getCDOBranch(to);
    	final ICDOConnection connection = repository.getConnection();
    	final CDOBranchMerger merger = new CDOBranchMerger(repository.getConflictProcessor(), isRebase);
    	final CDOTransaction targetTransaction = connection.createTransaction(targetBranch);

    	try {
    		// XXX: specifying sourceBase instead of defaulting to the computed common ancestor point here
    		targetTransaction.merge(sourceBranch.getHead(), sourceBranch.getBase(), merger);
    		merger.postProcess(targetTransaction);
    		return targetTransaction;
    	} catch (CDOMerger.ConflictException e) {
    		CDOView sourceView = connection.createView(sourceBranch);
    		Collection<MergeConflict> conflicts = merger.handleCDOConflicts(sourceView, targetTransaction);
    		LifecycleUtil.deactivate(targetTransaction);
    		LifecycleUtil.deactivate(sourceView);
			throw new MergeConflictException(conflicts, String.format("Could not resolve all conflicts while applying changeset on '%s' from '%s'.", to.path(), from.path()));
    	}
    }
    
	private InternalBranch commitChanges(InternalBranch from, InternalBranch to, String commitMessage, CDOTransaction targetTransaction) {
		try { 

            if (targetTransaction.isDirty()) {
    			// FIXME: Using "System" user and "synchronize" description until a more suitable pair can be specified here
            	targetTransaction.setCommitComment(commitMessage);
            	CDOCommitInfo commitInfo = new CDOServerCommitBuilder(SpecialUserStore.SYSTEM_USER_NAME, commitMessage, targetTransaction)
            			.parentContextDescription(DatastoreLockContextDescriptions.SYNCHRONIZE)
            			.commitOne();
            	
	            return to.withHeadTimestamp(commitInfo.getTimeStamp());
            } else {
            	return to;
            }

        } catch (CommitException e) {
            throw new BranchMergeException("Failed to apply changeset on '%s' from '%s'.", to.path(), from.path(), e);
        }
	}
    
    @Override
    InternalBranch reopen(InternalBranch parent, String name, Metadata metadata) {
        final CDOBranch childCDOBranch = createCDOBranch(parent, name);
        final CDOBranchPoint[] basePath = childCDOBranch.getBasePath();
        final long timeStamp = basePath[basePath.length - 1].getTimeStamp();
		return reopen(parent, name, metadata, timeStamp, timeStamp, childCDOBranch.getID());
    }

    private InternalBranch reopen(final InternalBranch parent, final String name, final Metadata metadata, final long baseTimestamp, final long headTimestamp, final int cdoBranchId) {
    	final Pair<Integer, Integer> nextTwoSegments = nextTwoSegments();
    	return commit(new IndexWrite<InternalBranch>() {
			@Override
			public InternalBranch execute(Writer index) throws IOException {
				final InternalCDOBasedBranch parentBranch = (InternalCDOBasedBranch) parent;
				final String parentPath = parent.path();
				final Set<Integer> parentSegments = newHashSet();
		    	// all branch should know the segment path to the ROOT
		    	parentSegments.addAll(parentBranch.parentSegments());
		    	parentSegments.addAll(parentBranch.segments());
				
				// the "new" child branch
				final CDOBranchImpl childBranch = new CDOBranchImpl(name, parentPath, baseTimestamp, headTimestamp, metadata, cdoBranchId, nextTwoSegments.getA(), Collections.singleton(nextTwoSegments.getA()), parentSegments);
				create(childBranch).execute(index);
				update(parent.getClass(), parentPath, new Function<InternalBranch, InternalBranch>() {
					@Override
					public InternalBranch apply(InternalBranch input) {
						return parentBranch.withSegmentId(nextTwoSegments.getB());
					}
				}).execute(index);
				return childBranch;
			}
		});
    }
    
    private CDOBranch createCDOBranch(InternalBranch parent, String name) {
        return getCDOBranch(parent).createBranch(name);
    }

    /**
     * Notifies this branch manager that a commit has happened on the given cdoBranchId with the given commitTimestamp.
     * @param cdoBranchId
     * @param commitTimestamp
     */
    public final void handleCommit(int cdoBranchId, long commitTimestamp) {
    	handleCommit((InternalBranch) getBranch(cdoBranchId), commitTimestamp);
    }
    
    @Override
    void sendChangeEvent(final String branch) {
    	new BranchChangedEvent(repository.id(), branch).publish(repository.events());
    }
}
