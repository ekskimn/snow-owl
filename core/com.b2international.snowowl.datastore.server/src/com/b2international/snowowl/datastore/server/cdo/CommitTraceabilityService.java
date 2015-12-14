package com.b2international.snowowl.datastore.server.cdo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta.Type;
import org.eclipse.emf.cdo.server.IRepository.WriteAccessHandler;
import org.eclipse.emf.cdo.server.IStoreAccessor.CommitContext;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.server.RepositoryNotFoundException;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import com.b2international.snowowl.datastore.cdo.CDOUtils;

public class CommitTraceabilityService implements WriteAccessHandler {

	private static final Map<String, CommitTraceabilityService> serviceMap = new HashMap<>();
	private final List<CommitTraceabilityListener> listeners;
	private ExecutorService executorService = Executors.newCachedThreadPool();
	
	public CommitTraceabilityService(final String repositoryUuid) {
		listeners = new ArrayList<CommitTraceabilityListener>();
		synchronized (CommitTraceabilityService.class) {
			serviceMap.put(repositoryUuid, this);
		}
	}
	
	@Override
	public void handleTransactionBeforeCommitting(ITransaction transaction, CommitContext commitContext, OMMonitor monitor)
			throws RuntimeException {
		
	}

	@Override
	public void handleTransactionAfterCommitted(ITransaction transaction, CommitContext commitContext, OMMonitor monitor) {
		final String commitComment = commitContext.getCommitComment();
		final String branchPath = commitContext.getBranchPoint().getBranch().getPathName();
		System.out.println("commitComment : " + commitComment);

		final Map<String, List<String>> newObjectCdoIds = new HashMap<>();
		final Map<String, List<String>> updatedObjectCdoIds = new HashMap<>();
//		TODO final Set<String> detachedObjectCdoIds = new HashSet<String>();
		
		System.out.println("NEW");
		final InternalCDORevision[] newObjects = commitContext.getNewObjects();
		for (InternalCDORevision internalCDORevision : newObjects) {
			final CDOID id = internalCDORevision.getID();
			System.out.println("id " + id);
			final String className = internalCDORevision.getEClass().getName();
			System.out.println("class " + className);
			if (!newObjectCdoIds.containsKey(className)) {
				newObjectCdoIds.put(className, new ArrayList<String>());
			}
			newObjectCdoIds.get(className).add(id.toString());
		}
		
		System.out.println("UPDATED");
		final InternalCDORevision[] dirtyObjects = commitContext.getDirtyObjects();
		for (InternalCDORevision internalCDORevision : dirtyObjects) {
			final CDOID id = internalCDORevision.getID();
			System.out.println("id " + id);
			final String className = internalCDORevision.getEClass().getName();
			System.out.println("class " + className);
			if (!updatedObjectCdoIds.containsKey(className)) {
				updatedObjectCdoIds.put(className, new ArrayList<String>());
			}
			updatedObjectCdoIds.get(className).add(id.toString());
		}
		
		
		for (final CommitTraceabilityListener commitTraceabilityListener : listeners) {
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					commitTraceabilityListener.transactionCommitted(branchPath, commitComment, newObjectCdoIds, updatedObjectCdoIds, null);
				}
			});
		}
	}

	@Override
	public void handleTransactionRollback(ITransaction transaction, CommitContext commitContext) {
		
	}

	public static void addListener(final String repositoryUuid, CommitTraceabilityListener listener) {
		synchronized (CommitTraceabilityService.class) {
			final CommitTraceabilityService commitTraceabilityService = serviceMap.get(repositoryUuid);
			if (commitTraceabilityService == null) {
				throw new RepositoryNotFoundException(repositoryUuid);
			}
			commitTraceabilityService.listeners.add(listener);
		}		
	}

}
