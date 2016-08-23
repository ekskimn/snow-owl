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
package com.b2international.snowowl.server.console;

import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import org.apache.lucene.search.Query;
import org.eclipse.emf.cdo.common.CDOCommonRepository.State;
import org.eclipse.emf.cdo.internal.server.syncing.RepositorySynchronizer;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSessionConfigurationFactory;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.slf4j.LoggerFactory;

import com.b2international.commons.StringUtils;
import com.b2international.index.BulkIndexWrite;
import com.b2international.index.Hits;
import com.b2international.index.Index;
import com.b2international.index.IndexWrite;
import com.b2international.index.Searcher;
import com.b2international.index.Writer;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Query;
import com.b2international.index.revision.Purge;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.ApplicationContext.ServiceRegistryEntry;
import com.b2international.snowowl.core.Repository;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.branch.BranchManager;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.Dates;
import com.b2international.snowowl.core.exceptions.NotFoundException;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.cdo.ICDORepositoryManager;
import com.b2international.snowowl.datastore.index.RevisionDocument;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.datastore.server.ServerDbUtils;
import com.b2international.snowowl.datastore.server.internal.branch.BranchManagerImpl;
import com.b2international.snowowl.datastore.server.internal.branch.InternalBranch;
import com.b2international.snowowl.datastore.server.internal.branch.InternalCDOBasedBranch;
import com.b2international.snowowl.datastore.server.reindex.OptimizeRequest;
import com.b2international.snowowl.datastore.server.reindex.PurgeRequest;
import com.b2international.snowowl.datastore.server.reindex.ReindexRequest;
import com.b2international.snowowl.datastore.server.reindex.ReindexRequestBuilder;
import com.b2international.snowowl.datastore.server.reindex.ReindexResult;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument.Builder;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.snomedrefset.SnomedRefSet;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

/**
 * OSGI command contribution with Snow Owl commands.
 *
 */
public class MaintenanceCommandProvider implements CommandProvider {

	private static final String DEFAULT_BRANCH_PREFIX = "|--";
	private static final String DEFAULT_INDENT = "   ";
	private static final Pattern TMP_BRANCH_NAME_PATTERN = Pattern.compile(String.format("^(%s)([%s]{1,%s})(_[0-9]{1,19}$)",
			Pattern.quote(Branch.TEMP_PREFIX), Branch.DEFAULT_ALLOWED_BRANCH_NAME_CHARACTER_SET, Branch.DEFAULT_MAXIMUM_BRANCH_NAME_LENGTH));

	private static final String LISTBRANCHES_COMMAND = "listbranches";
	private static final String LISTREPOSITORIES_COMMAND = "listrepositories";
	private static final String DBCREATEINDEX_COMMAND = "dbcreateindex";
<<<<<<< HEAD
	private static final String CHECKSERVICES_COMMAND = "checkservices";
	private static final String RECREATEINDEX = "recreateindex";

	class Artefact {
		public long storageKey;
		public long snomedId;
		public String branchPath;

		public Artefact(String branchPath, long snomedId, long storageKey) {
			this.branchPath = branchPath;
			this.snomedId = snomedId;
			this.storageKey = storageKey;
		}

		@Override
		public String toString() {
			return "Artefact [branchPath=" + branchPath + ", storageKey=" + storageKey + ", snomedId=" + snomedId + "]";
		}
	}
=======
>>>>>>> SO-2004: remove checkservices command

	@Override
	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("---Snow Owl commands---\n");
<<<<<<< HEAD
		buffer.append("\tsnowowl checkservices - Checks the core services presence\n");
<<<<<<< HEAD
		buffer.append("\tsnowowl dbcreateindex [nsUri] - creates the CDO_CREATED index on the proper DB tables for all classes contained by a package identified by its unique namspace URI\n");
=======
		buffer.append("\tsnowowl dbcreateindex [nsUri] - creates the CDO_CREATED index on the proper DB tables for all classes contained by a package identified by its unique namespace URI.\n");
		buffer.append("\tsnowowl listrepositories - prints all the repositories in the system.\n");
		buffer.append("\tsnowowl listbranches [repository] - prints all the branches in the system for a repository.\n");
>>>>>>> SO-2004: remove checkservices command
		buffer.append("\tsnowowl reindex [repositoryId] [failedCommitTimestamp]- reindexes the content for the given repository ID from the given failed commit timestamp (optional, default timestamp is 1 which means no failed commit).\n");
		buffer.append("\tsnowowl optimize [repositoryId] [maxSegments] - optimizes the underlying index for the repository to have the supplied maximum number of segments (default number is 1)\n");
		buffer.append("\tsnowowl purge [repositoryId] [branchPath] [ALL|LATEST|HISTORY] - optimizes the underlying index by deleting unnecessary documents from the given branch using the given purge strategy (default strategy is LATEST)\n");
		buffer.append("\tsnowowl listbranches [repository] [branchPath] - prints all the child branches of the specified branch path in the system for a repository. Branch path is MAIN by default and has to be full path (e.g. MAIN/PROJECT/TASK)\n");
=======
		buffer.append("\tsnowowl listrepositories - prints all the repositories in the system.\n");
		buffer.append("\tsnowowl listbranches [repository] - prints all the branches in the system for a repository.\n");
		buffer.append("\tsnowowl replacedupids [branchPath] - replaces components with duplicate ids in the SNOMED CT repository on a given branch (e.g. MAIN/PROJECT/TASK1). If no branch is given the replacement is executed for all the branches.\n");
		buffer.append(
				"\tsnowowl dbcreateindex [nsUri] - creates the CDO_CREATED index on the proper DB tables for all classes contained by a package identified by its unique namspace URI\n");
		buffer.append("\tsnowowl recreateindex - recreates the index from the CDO store.");
>>>>>>> origin/ms-develop
		return buffer.toString();
	}

	/**
	 * Reflective template method declaratively registered. Needs to start with
	 * "_".
	 * 
	 * @param interpreter
	 * @throws InterruptedException
	 */
	public void _snowowl(CommandInterpreter interpreter) throws InterruptedException {
		String cmd = interpreter.nextArgument();
<<<<<<< HEAD

<<<<<<< HEAD
			if ("dbcreateindex".equals(cmd)) {
				executeCreateDbIndex(interpreter);
=======
		try {
			if (DBCREATEINDEX_COMMAND.equals(cmd)) {
				createDbIndex(interpreter);
>>>>>>> SO-2004: remove checkservices command
				return;
			}
=======
		if (CHECKSERVICES_COMMAND.equals(cmd)) {
			checkServices(interpreter);
			return;
		}
>>>>>>> origin/ms-develop

		if (DBCREATEINDEX_COMMAND.equals(cmd)) {
			createDbIndex(interpreter);
			return;
		}

		if (LISTREPOSITORIES_COMMAND.equals(cmd)) {
			listRepositories(interpreter);
			return;
		}

<<<<<<< HEAD
			if ("reindex".equals(cmd)) {
				reindex(interpreter);
				return;
			}
			
			if ("optimize".equals(cmd)) {
				optimize(interpreter);
				return; 
			}
			
			if ("purge".equals(cmd)) {
				purge(interpreter);
				return;
			}
			
			if ("fixtempbranches".equals(cmd)) {
				fixTempBranches(interpreter);
				return;
			}
			
			if ("fixrefsetdocs".equals(cmd)) {
				fixRefsetDocs(interpreter);
				return;
			}
			
			if ("fixrelationships".equals(cmd)) {
				fixRelationships(interpreter);
				return;
			}
			
			interpreter.println(getHelp());
		} catch (Exception ex) {
			LoggerFactory.getLogger("console").error("Failed to execute command", ex);
			if (Strings.isNullOrEmpty(ex.getMessage())) {
				interpreter.println("Something went wrong during the processing of your request.");
			} else {
				interpreter.println(ex.getMessage());
			}
=======
		if (LISTBRANCHES_COMMAND.equals(cmd)) {
			listBranches(interpreter);
			return;
		}
		
		if ("replacedupids".equals(cmd)) {
			checkDuplicateIds(interpreter);
			return;
		}

		if (RECREATEINDEX.equals(cmd)) {
			executeRecreateIndex(interpreter);
			return;
>>>>>>> origin/ms-develop
		}
		interpreter.println(getHelp());
	}

<<<<<<< HEAD
	private void fixRelationships(final CommandInterpreter interpreter) {
		
		final RepositoryManager repositoryManager = ApplicationContext.getInstance().getService(RepositoryManager.class);
		final Repository repository = repositoryManager.get(SnomedDatastoreActivator.REPOSITORY_UUID);
		
		Index index = repository.service(Index.class);
		
		index.write(new IndexWrite<Void>() {
			@Override
			public Void execute(Writer writer) throws IOException {
				
				Set<String> relationshipIds = newHashSet("6624515026", "6624516025", "6624517023", "6613668024");
				
				Searcher searcher = writer.searcher();
				Hits<SnomedRelationshipIndexEntry> hits = searcher.search(Query.select(SnomedRelationshipIndexEntry.class)
						.where(RevisionDocument.Expressions.ids(relationshipIds)).limit(Integer.MAX_VALUE).build());
				
				interpreter.println(String.format("Found %s relationship documents", hits.getTotal()));
				
				for (SnomedRelationshipIndexEntry entry : hits) {
					writer.remove(SnomedRelationshipIndexEntry.class, entry._id());
					interpreter.println(String.format("Removing relationships document: %s", entry));
				}
								
				writer.commit();
				
				return null;
			}
		});
		
		
	}

	private void fixRefsetDocs(final CommandInterpreter interpreter) {
		
		final RepositoryManager repositoryManager = ApplicationContext.getInstance().getService(RepositoryManager.class);
		final Repository repository = repositoryManager.get(SnomedDatastoreActivator.REPOSITORY_UUID);
		
		Index index = repository.service(Index.class);
		
		index.write(new IndexWrite<Void>() {
			@Override
			public Void execute(Writer writer) throws IOException {
				
				try (SnomedEditingContext editingContext = new SnomedEditingContext(BranchPathUtils.createMainPath())) {
					
					Set<String> refsetIds = Sets.newHashSet();
					
					EList<EObject> eRefsets = editingContext.getRefSetEditingContext().getEditingContextRootResource().eContents();
					for (EObject eRefset : eRefsets) {
						if (eRefset instanceof SnomedRefSet) {
							SnomedRefSet snomedRefSet = (SnomedRefSet) eRefset;
							refsetIds.add(snomedRefSet.getIdentifierId());
						}
					}
					
					interpreter.println(String.format("Found %s reference sets", refsetIds.size()));
					
					Searcher searcher = writer.searcher();
					Hits<SnomedConceptDocument> hits = searcher.search(Query.select(SnomedConceptDocument.class)
							.where(RevisionDocument.Expressions.ids(refsetIds)).limit(Integer.MAX_VALUE).build());
					
					for (SnomedConceptDocument hit : hits) {
						
						if (hit.getRefSetStorageKey() > 0) {
							interpreter.println(String.format("Found correct refset document: %s", hit.getId()));
							continue;
						}
						
						final Builder doc = SnomedConceptDocument.builder(hit);
						doc.branchPath(hit.getBranchPath());
						doc.replacedIns(hit.getReplacedIns());
						doc.commitTimestamp(hit.getCommitTimestamp());
						doc.segmentId(hit.getSegmentId());
						
						SnomedRefSet refSet = editingContext.getRefSetEditingContext().lookup(hit.getId(), SnomedRefSet.class);
						
						if (refSet != null) {
							doc.refSet(refSet);
						}
						
						interpreter.println(String.format("Fixed refset document with id %s", hit.getId()));
						
						writer.put(hit._id(), doc.build());
						
					}
					
					writer.commit();
				}
				
				return null;
			}
		});
	}

	private void fixTempBranches(final CommandInterpreter interpreter) {
		
		final RepositoryManager repositoryManager = ApplicationContext.getInstance().getService(RepositoryManager.class);
		final Repository repository = repositoryManager.get(SnomedDatastoreActivator.REPOSITORY_UUID);
		
		Index index = repository.service(Index.class);
		
		Integer numberOfAffectedBranches = index.write(new IndexWrite<Integer>() {
			@Override
			public Integer execute(Writer writer) throws IOException {
				
				Searcher searcher = writer.searcher();
				Hits<InternalBranch> hits = searcher.search(Query.select(InternalBranch.class).where(Expressions.matchAll()).limit(Integer.MAX_VALUE).build());
				
				Multimap<Integer, InternalBranch> idToBranchesMap = Multimaps.index(hits, new Function<InternalBranch, Integer>() {
					@Override
					public Integer apply(InternalBranch input) {
						input.setBranchManager((BranchManagerImpl) repository.service(BranchManager.class));
						return ((InternalCDOBasedBranch) input).cdoBranchId();
					}
				});
				
				List<IndexWrite<Void>> indexWrites = Lists.newArrayList();
				
				int i = 0;
				
				for (Entry<Integer, Collection<InternalBranch>> entry : idToBranchesMap.asMap().entrySet()) {
					
					if (entry.getValue().size() > 1) {
						
						List<String> branchInfo = FluentIterable.from(entry.getValue()).transform(new Function<InternalBranch, String>() {
							@Override
							public String apply(InternalBranch input) {
								return String.format("[%s] %s", input.headTimestamp(), input.path());
							}
						}).toList();
						
						if (entry.getValue().size() > 2) {
							interpreter.println(String.format("More than one branch exists with the same id: %s %s", entry.getKey(), Joiner.on("; ").join(branchInfo)));
							continue;
						}
						
						InternalBranch first = Iterables.getFirst(entry.getValue(), null);
						InternalBranch second = Iterables.getLast(entry.getValue(), null);
						
						if (first != null && second != null) {
							
							InternalBranch keep = null;
							InternalBranch remove = null;
							
							if (first.name().startsWith(Branch.TEMP_PREFIX) && !second.name().startsWith(Branch.TEMP_PREFIX)) {
								keep = second;
								remove = first;
							} else if (!first.name().startsWith(Branch.TEMP_PREFIX) && second.name().startsWith(Branch.TEMP_PREFIX)) {
								keep = first;
								remove = second;
							} else {
								interpreter.println(String.format("Inconsistent temporary branch name prefixes: %s %s", entry.getKey(), Joiner.on("; ").join(branchInfo)));
								continue;
							}
							
							if (!isBranchesStructurallyEqual((InternalCDOBasedBranch) keep, (InternalCDOBasedBranch) remove)) {
								interpreter.println(String.format("Inconsistent temporary branches: %s %s", entry.getKey(), Joiner.on("; ").join(branchInfo)));
								continue;
							}
							
							if (remove.headTimestamp() > keep.headTimestamp()) {
								
								final InternalBranch finalKeep = keep.withHeadTimestamp(remove.headTimestamp());
								
								IndexWrite<Void> update = new IndexWrite<Void>() {
									@Override
									public Void execute(Writer index) throws IOException {
										index.put(finalKeep.path(), finalKeep);
										return null;
									}
								};
								
								indexWrites.add(update);
								
								interpreter.println(String.format("Using head timestamp of %s for %s: [%s] >> [%s]", remove.name(), keep.name(), remove.headTimestamp(), keep.headTimestamp()));
								
							}
							
							final InternalBranch finalRemove = remove;
							
							IndexWrite<Void> delete = new IndexWrite<Void>() {
								@Override
								public Void execute(Writer index) throws IOException {
									index.remove(InternalBranch.class, finalRemove.path());
									return null;
								}
							};
							
							indexWrites.add(delete);
							
							i++;
							
							interpreter.println(String.format("Removing temporary branch entry: %s", remove.name()));
						}
						
					} else if (entry.getValue().size() == 1) {
						
						final InternalBranch singleBranch = Iterables.getOnlyElement(entry.getValue());
						
						if (singleBranch.name().startsWith(Branch.TEMP_PREFIX)) {
							
							IndexWrite<Void> delete = new IndexWrite<Void>() {
								@Override
								public Void execute(Writer index) throws IOException {
									index.remove(InternalBranch.class, singleBranch.path());
									return null;
								}
							};
							
							indexWrites.add(delete);
							i++;
							interpreter.println(String.format("Removing standalone temporary branch entry: %s", singleBranch.name()));
						}
					}
				}
				
				if (!indexWrites.isEmpty())	{
					
					BulkIndexWrite<Void> bulkIndexWrite = new BulkIndexWrite<>(indexWrites);
					bulkIndexWrite.execute(writer);
					
					writer.commit();
					
					interpreter.println("Changes successfully committed to index.");
					
				}
				
				return i;
=======
	public synchronized void createDbIndex(CommandInterpreter interpreter) {
		String nsUri = interpreter.nextArgument();
		if (!Strings.isNullOrEmpty(nsUri)) {
			ServerDbUtils.createCdoCreatedIndexOnTables(nsUri);
		} else {
			interpreter.println("Namespace URI should be specified.");
		}
	}

	public synchronized void listRepositories(CommandInterpreter interpreter) {
		Set<String> uuidKeySet = getRepositoryManager().uuidKeySet();
		if (!uuidKeySet.isEmpty()) {
			interpreter.println("Repositories:");
			for (String repositoryName : uuidKeySet) {
				interpreter.println(String.format("\t%s", repositoryName));
>>>>>>> origin/ms-develop
			}
		});
		
		if (numberOfAffectedBranches > 0) {
			interpreter.println(String.format("%s temporary branches were fixed", numberOfAffectedBranches));
		} else {
			interpreter.println("None of the temporary branches are inconsistent");
		}
	}
<<<<<<< HEAD
	
	private boolean isBranchesStructurallyEqual(InternalCDOBasedBranch keep, InternalCDOBasedBranch remove) {
		return keep.baseTimestamp() == remove.baseTimestamp() &&
				keep.parentPath().equals(remove.parentPath()) &&
				keep.name().equals(getBareTemporaryBranchName(remove.name())) &&
				keep.parentSegments().size() == remove.parentSegments().size() && keep.parentSegments().containsAll(remove.parentSegments()) &&
				keep.segments().size() >= remove.segments().size() && keep.segments().containsAll(remove.segments());
	}
	
	private String getBareTemporaryBranchName(String tempBranchName) {
		Matcher matcher = TMP_BRANCH_NAME_PATTERN.matcher(tempBranchName);
		if (matcher.matches()) {
			return matcher.group(2);
		}
		return "";
	}

	private void purge(CommandInterpreter interpreter) {
		final String repositoryId = interpreter.nextArgument();
		
		if (Strings.isNullOrEmpty(repositoryId)) {
			interpreter.println("RepositoryId parameter is required");
			return;
		}
		
		final String branchPath = interpreter.nextArgument();
		
		if (Strings.isNullOrEmpty(branchPath)) {
			interpreter.print("BranchPath parameter is required");
			return;
		}
		
		
		final String purgeArg = interpreter.nextArgument();
		final Purge purge = Strings.isNullOrEmpty(purgeArg) ? Purge.LATEST : Purge.valueOf(purgeArg);
		if (purge == null) {
			interpreter.print("Invalid purge parameter. Select one of " + Joiner.on(",").join(Purge.values()));
			return;
		}
		
		PurgeRequest.builder()
			.setBranchPath(branchPath)
			.setPurge(purge)
			.build(repositoryId)
			.execute(getBus())
			.getSync();
	}

	private void reindex(CommandInterpreter interpreter) {
		final String repositoryId = interpreter.nextArgument();
		
		if (Strings.isNullOrEmpty(repositoryId)) {
			interpreter.println("RepositoryId parameter is required");
			return;
		}
		
		final ReindexRequestBuilder req = ReindexRequest.builder();
		
		final String failedCommitTimestamp = interpreter.nextArgument();
		if (!StringUtils.isEmpty(failedCommitTimestamp)) {
			req.setFailedCommitTimestamp(Long.parseLong(failedCommitTimestamp));
		}
		
		final ReindexResult result = req
				.build(repositoryId)
				.execute(getBus())
				.getSync();
		
		interpreter.println(result.getMessage());
	}

	private static IEventBus getBus() {
		return ApplicationContext.getServiceForClass(IEventBus.class);
	}
	
	private void optimize(CommandInterpreter interpreter) {
		final String repositoryId = interpreter.nextArgument();
		if (Strings.isNullOrEmpty(repositoryId)) {
			interpreter.println("RepositoryId parameter is required.");
			return;
		}
		
		// default max segments is 1
		int maxSegments = 1;
		final String maxSegmentsArg = interpreter.nextArgument();
		if (!Strings.isNullOrEmpty(maxSegmentsArg)) {
			maxSegments = Integer.parseInt(maxSegmentsArg);
		}
=======

	public synchronized void listBranches(CommandInterpreter interpreter) {
		String repositoryName = interpreter.nextArgument();
		if (isValidRepositoryName(repositoryName, interpreter)) {
			interpreter.println(String.format("Branches for repository %s:", repositoryName));

			Branch mainBranch = BranchPathUtils.getMainBranchForRepository(repositoryName);

			List<Branch> allBranches = newArrayList(mainBranch.children());
			allBranches.add(mainBranch);

			printBranchHierarchy(allBranches, Sets.<Branch> newHashSet(), mainBranch, interpreter);
		}
	}

	private void checkDuplicateIds(CommandInterpreter interpreter) {
>>>>>>> origin/ms-develop

		interpreter.println("Optimizing index to max. " + maxSegments + " number of segments...");
		OptimizeRequest.builder()
			.setMaxSegments(maxSegments)
			.build(repositoryId)
			.execute(getBus())
			.getSync();
		interpreter.println("Index optimization completed.");
	}

	public synchronized void executeCreateDbIndex(CommandInterpreter interpreter) {

		String nsUri = interpreter.nextArgument();
		if (null != nsUri) {
			ServerDbUtils.createCdoCreatedIndexOnTables(nsUri);
		} else {
			interpreter.println("Namespace URI should be specified.");
		}
	}

	public synchronized void listRepositories(CommandInterpreter interpreter) {
		ICDORepositoryManager repositoryManager = getRepositoryManager();
		Set<String> uuidKeySet = repositoryManager.uuidKeySet();
		if (!uuidKeySet.isEmpty()) {
			interpreter.println("Repositories:");
			for (String repositoryName : uuidKeySet) {
				interpreter.println("  " + repositoryName);
			}
		}
	}

	public synchronized void listBranches(CommandInterpreter interpreter) {
		
		String repositoryUUID = interpreter.nextArgument();
		
		if (isValidRepositoryName(repositoryUUID, interpreter)) {
			
			String parentBranchPath = interpreter.nextArgument();
			
			if (Strings.isNullOrEmpty(parentBranchPath)) {
				interpreter.println("Parent branch path was not specified, falling back to MAIN");
				parentBranchPath = Branch.MAIN_PATH;
			} else if (!parentBranchPath.startsWith(Branch.MAIN_PATH)) {
				interpreter.println("Specify parent branch with full path. i.e. MAIN/PROJECT/TASK1");
				return;
			}
			
			Branch parentBranch = null;
			
			try {
				 parentBranch = RepositoryRequests.branching(repositoryUUID).prepareGet(parentBranchPath).executeSync(getBus(), 1000);
			} catch (NotFoundException e) {
				interpreter.println(String.format("Unable to find %s", parentBranchPath));
				return;
			}
			
			if (parentBranch != null) {
				interpreter.println(String.format("Branch hierarchy for %s in repository %s:", parentBranchPath, repositoryUUID));
				print(parentBranch, getDepthOfBranch(parentBranch), interpreter);
			}
			
		}
	}
	
	private void print(final Branch branch, final int parentDepth, CommandInterpreter interpreter) {
		
		printBranch(branch, getDepthOfBranch(branch) - parentDepth, interpreter);
		
		List<? extends Branch> children = FluentIterable.from(branch.children()).filter(new Predicate<Branch>() {
			@Override
			public boolean apply(Branch input) {
				return input.parentPath().equals(branch.path());
			}
		}).toSortedList(new Comparator<Branch>() {
			@Override
			public int compare(Branch o1, Branch o2) {
				return Longs.compare(o1.baseTimestamp(), o2.baseTimestamp());
			}
		});
		
		if (children.size() != 0) {
			for (Branch child : children) {
				print(child, parentDepth, interpreter);
			}
		}
		
	}

	private void printBranch(Branch branch, int depth, CommandInterpreter interpreter) {
		interpreter.println(String.format("%-30s %-12s B: %s H: %s",
				String.format("%s%s%s", 
				getIndentationForBranch(depth), 
				DEFAULT_BRANCH_PREFIX, 
				branch.name()),
				String.format("[%s]", branch.state()),
				Dates.formatByGmt(branch.baseTimestamp(), DateFormats.LONG), 
				Dates.formatByGmt(branch.headTimestamp(), DateFormats.LONG)));
	}
		
	private String getIndentationForBranch(int depth) {
		String indent = "";
		for (int i = 0; i < depth; i++) {
			indent += DEFAULT_INDENT;
		}
		return indent;
	}

	private int getDepthOfBranch(Branch currentBranch) {
		return Iterables.size(Splitter.on(Branch.SEPARATOR).split(currentBranch.path()));
	}

<<<<<<< HEAD
	public synchronized void checkServices(CommandInterpreter ci) {
		
		ci.println("Checking core services...");
=======
	private void printBranchHierarchy(List<Branch> branches, Set<Branch> visitedBranches, Branch currentBranch, CommandInterpreter interpreter) {
		interpreter.println(String.format("%s%s%s", getDepthOfBranch(currentBranch), DEFAULT_BRANCH_PREFIX, currentBranch.name()));
		visitedBranches.add(currentBranch);
		for (Branch branch : branches) {
			if (!visitedBranches.contains(branch)) {
				if (branch.parentPath().equals(currentBranch.path())) {
					printBranchHierarchy(branches, visitedBranches, branch, interpreter);
				}
			}
		}
	}

	private String getDepthOfBranch(Branch currentBranch) {
		int depth = Splitter.on(Branch.SEPARATOR).splitToList(currentBranch.path()).size();
		String indent = "";
		for (int i = 1; i < depth; i++) {
			indent = indent + DEFAULT_INDENT;
		}
		return indent;
	}

<<<<<<< HEAD
	public synchronized void checkServices(CommandInterpreter interpreter) {

		interpreter.println("Checking core services...");

>>>>>>> origin/ms-develop
		try {
			Collection<ServiceRegistryEntry<?>> services = ApplicationContext.getInstance().checkServices();
			for (ServiceRegistryEntry<?> entry : services) {
				interpreter.println(String.format("Interface: %s : %s", entry.getServiceInterface(), entry.getImplementation()));
			}
<<<<<<< HEAD
			ci.println("Core services are registered properly and available for use.");
		} catch (final Throwable t) {
			ci.print("Error: " + t.getMessage());
		}
	}

=======
>>>>>>> SO-2004: remove checkservices command
	private boolean isValidRepositoryName(String repositoryName, CommandInterpreter interpreter) {
		
		if (Strings.isNullOrEmpty(repositoryName)) {
			interpreter.println("Repository name should be specified. Execute 'listrepositories' to see the available repositories.");
			return false;
		}
		
		Set<String> uuidKeySet = getRepositoryManager().uuidKeySet();
		
=======
			interpreter.println("Core services are registered properly and available for use.");
		} catch (final SnowowlRuntimeException e) {
			interpreter.printStackTrace(e);
		}
	}

	@SuppressWarnings("restriction")
	public synchronized void executeRecreateIndex(CommandInterpreter interpreter) throws InterruptedException {

		String repositoryName = "snomedStore";

		ICDOConnectionManager connectionManager = ApplicationContext.getServiceForClass(ICDOConnectionManager.class);

		RepositorySynchronizer synchronizer = new RepositorySynchronizer();
		ICDOConnection cdoConnection = connectionManager.getByUuid(repositoryName);
		final CDONet4jSessionConfiguration sessionConfiguration = cdoConnection.getSessionConfiguration();
		synchronizer.setRemoteSessionConfigurationFactory(new CDOSessionConfigurationFactory() {

			@Override
			public CDOSessionConfiguration createSessionConfiguration() {
				return sessionConfiguration;
			}
		});

		// replicate commits as opposed to raw lines
		synchronizer.setRawReplication(false);
		SnowOwlDummyInternalRepository localRepository = new SnowOwlDummyInternalRepository();
		synchronizer.setLocalRepository(localRepository);
		synchronizer.activate();

		// do the work, wait until it finishes
		do {
			Thread.sleep(10000);
		} while (localRepository.getState() == State.ONLINE);

		synchronizer.deactivate();
	}

	private boolean isValidRepositoryName(String repositoryName, CommandInterpreter interpreter) {
		Set<String> uuidKeySet = getRepositoryManager().uuidKeySet();
>>>>>>> origin/ms-develop
		if (!uuidKeySet.contains(repositoryName)) {
			interpreter.println("Could not find repository called: " + repositoryName);
			interpreter.println("Available repository names are: " + uuidKeySet);
			return false;
		}
		
		return true;
	}

	private ICDORepositoryManager getRepositoryManager() {
		return ApplicationContext.getServiceForClass(ICDORepositoryManager.class);
	}
}