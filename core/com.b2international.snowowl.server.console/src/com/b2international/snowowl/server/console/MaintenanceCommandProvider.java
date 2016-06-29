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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.eclipse.emf.cdo.common.CDOCommonRepository.State;
import org.eclipse.emf.cdo.internal.server.syncing.RepositorySynchronizer;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSessionConfigurationFactory;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.ApplicationContext.ServiceRegistryEntry;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.cdo.ICDOConnection;
import com.b2international.snowowl.datastore.cdo.ICDOConnectionManager;
import com.b2international.snowowl.datastore.cdo.ICDORepositoryManager;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.datastore.server.ServerDbUtils;
import com.b2international.snowowl.datastore.server.index.IndexServerService;
import com.b2international.snowowl.datastore.server.snomed.index.ConceptIdStorageKeyCollector;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.core.domain.ISnomedDescription;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.core.domain.SnomedDescriptions;
import com.b2international.snowowl.snomed.core.domain.SnomedRelationships;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.SnomedIdentifiers;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * OSGI command contribution with Snow Owl maintenance type commands.
 *
 */
public class MaintenanceCommandProvider implements CommandProvider {

	/**
	 * 
	 */
	private static final String DEFAULT_BRANCH_PREFIX = "|---";
	private static final String DEFAULT_INDENT = "    ";
	private static final String LISTBRANCHES_COMMAND = "listbranches";
	private static final String LISTREPOSITORIES_COMMAND = "listrepositories";
	private static final String DBCREATEINDEX_COMMAND = "dbcreateindex";
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

	@Override
	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("---Snow Owl commands---\n");
		buffer.append("\tsnowowl checkservices - Checks the core services presence\n");
		buffer.append("\tsnowowl listrepositories - prints all the repositories in the system.\n");
		buffer.append("\tsnowowl listbranches [repository] - prints all the branches in the system for a repository.\n");
		buffer.append("\tsnowowl replacedupids [branchPath] - replaces components with duplicate ids in the SNOMED CT repository on a given branch (e.g. MAIN/PROJECT/TASK1). If no branch is given the replacement is executed for all the branches.\n");
		buffer.append(
				"\tsnowowl dbcreateindex [nsUri] - creates the CDO_CREATED index on the proper DB tables for all classes contained by a package identified by its unique namspace URI\n");
		buffer.append("\tsnowowl recreateindex - recreates the index from the CDO store.");
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

		if (CHECKSERVICES_COMMAND.equals(cmd)) {
			checkServices(interpreter);
			return;
		}

		if (DBCREATEINDEX_COMMAND.equals(cmd)) {
			createDbIndex(interpreter);
			return;
		}

		if (LISTREPOSITORIES_COMMAND.equals(cmd)) {
			listRepositories(interpreter);
			return;
		}

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
		}
		interpreter.println(getHelp());
	}

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
			}
		}
	}

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

		String repositoryId = "snomedStore";
		String branchName = interpreter.nextArgument();

		IEventBus eventBus = ApplicationContext.getInstance().getService(IEventBus.class);
		Branch mainBranch = RepositoryRequests.branching(repositoryId).prepareGet("MAIN").executeSync(eventBus);

		Map<Long, Artefact> artefactMap = Maps.newHashMap();
		Branch selectedBranch = null;
		if (branchName != null) {
			selectedBranch = RepositoryRequests.branching(repositoryId).prepareGet(branchName).executeSync(eventBus, 1000);
			chechBranch(mainBranch, artefactMap, interpreter, selectedBranch);
		}
	}

	// Depth-first traversal
	private void chechBranch(Branch childBranch, Map<Long, Artefact> artefactMap, CommandInterpreter interpreter,
			Branch selectedBranch) {

		// These are already published versions with no dups
		List<String> skipVersions = Lists.newArrayList("2002-01-31", "2002-07-31", "2003-01-31", "2003-07-31",
				"2003-10-31", "2004-01-31", "2004-07-31", "2004-09-30", "2005-01-31", "2005-07-31", "2005-09-30",
				"2006-01-31", "2006-07-31", "2006-09-30", "2007-01-31", "2007-07-31", "2007-10-01", "2008-01-31",
				"2008-07-31", "2008-10-31", "2009-01-31", "2009-07-31", "2009-10-31", "2010-01-31", "2010-07-31",
				"2010-10-01", "2011-01-31", "2013-07-31", "2014-01-31", "2014-07-31", "2015-01-31", "2011-07-31",
				"2011-10-01", "2012-01-31", "2012-07-31", "2013-01-31", "2015-07-31");

		if (!skipVersions.contains(childBranch.name())) {
			//run for all branches
			if (selectedBranch == null) {
				interpreter.println("Processing: " + childBranch.path());
				processBranchContent(childBranch, artefactMap, interpreter);
				Collection<? extends Branch> children = childBranch.children();
				for (Branch branch : children) {
					chechBranch(branch, artefactMap, interpreter, selectedBranch);
				}
			
			//process the selected branch and its direct parent
			} else {
				interpreter.println("Processing: " + selectedBranch.parent().path());
				processBranchContent(selectedBranch.parent(), artefactMap, interpreter);
				interpreter.println("Processing: " + selectedBranch.path());
				processBranchContent(selectedBranch, artefactMap, interpreter);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void processBranchContent(Branch childBranch, Map<Long, Artefact> artefactMap,
			CommandInterpreter interpreter) {

		final IndexServerService indexService = (IndexServerService) ApplicationContext.getInstance()
				.getService(SnomedIndexService.class);
		final Query query = SnomedMappings.newQuery().concept().description().relationship().matchAny();

		IBranchPath branchPath = childBranch.branchPath();
		final int hitCount = indexService.getTotalHitCount(branchPath, query);
		final ConceptIdStorageKeyCollector collector = new ConceptIdStorageKeyCollector(hitCount);
		indexService.search(branchPath, query, collector);

		long[][] branchIds = collector.getIds();
		for (int i = 0; i < branchIds.length; i++) {
			long snomedId = branchIds[i][0];
			long storageKey = branchIds[i][1];
			Artefact artefact = artefactMap.get(snomedId);
			if (artefactMap.containsKey(snomedId) && artefact.storageKey != storageKey) {
				Artefact newArtefact = new Artefact(childBranch.path(), snomedId, storageKey);
				interpreter.println("Found duplicate in: " + artefact + " and: " + newArtefact);
				replaceDuplicateId(artefact, newArtefact, interpreter);
			} else {
				artefactMap.put(branchIds[i][0], new Artefact(childBranch.path(), branchIds[i][0], storageKey));
			}
		}
		interpreter.println("Branch '" + childBranch.name() + "' size: " + branchIds.length + ", total id map size: "
				+ artefactMap.size() + "\n");

	}

	private void replaceDuplicateId(Artefact artefact, Artefact otherArtefact, CommandInterpreter interpreter) {
		String artefactPath = artefact.branchPath;
		String otherArtefactPath = otherArtefact.branchPath;

		if (artefactPath.contains(otherArtefactPath)) {
			interpreter.println("Event triggered to replace the id for the component on the child branch:" + artefact);
			replaceComponent(artefact, interpreter);
		} else if (otherArtefactPath.contains(artefactPath)) {
			interpreter.println(
					"Event triggered to replace the id for the component on the child branch:" + otherArtefactPath);
			replaceComponent(otherArtefact, interpreter);
		} else {
			interpreter.println(
					"No parent child relation between the artefacts, event trigggered to replace the id on the newer branch: "
							+ otherArtefactPath);
			replaceComponent(otherArtefact, interpreter);
		}
	}

	private void replaceComponent(final Artefact artefact, final CommandInterpreter interpreter) {

		final long timeout = 600000;
		final IEventBus eventBus = ApplicationContext.getInstance().getService(IEventBus.class);

		final SnomedIdentifier snomedIdToReplace = SnomedIdentifiers.create(Long.toString(artefact.snomedId));
		interpreter.println("Replacing component: " + snomedIdToReplace.getComponentCategory());
		ComponentCategory componentCategory = snomedIdToReplace.getComponentCategory();

		if (componentCategory == ComponentCategory.RELATIONSHIP) {

			SnomedRelationships replationships = SnomedRequests.prepareSearchRelationship().one()
					.setComponentIds(Collections.singleton(Long.toString(artefact.snomedId))).build(artefact.branchPath)
					.executeSync(eventBus, timeout);

			for (ISnomedRelationship relationship : replationships) {

				if (!relationship.isActive()) {
					interpreter.println("**** Inactive relationship found, skipping!" + relationship);
					System.err.println("**** Inactive relationship found, skipping!" + relationship);
					continue;
				}
				SnomedRequests.prepareNewRelationship().setIdFromNamespace(snomedIdToReplace.getNamespace())
						.setCharacteristicType(relationship.getCharacteristicType())
						.setDestinationId(relationship.getDestinationId()).setGroup(relationship.getGroup())
						.setModifier(relationship.getModifier()).setModuleId(relationship.getModuleId())
						.setSourceId(relationship.getSourceId()).setTypeId(relationship.getTypeId())
						.setUnionGroup(relationship.getUnionGroup()).build("info@b2international.com",
								artefact.branchPath, "Id replaced due to duplicate artefact present.")
						.executeSync(eventBus, timeout);

				// delete the replaced component
				SnomedRequests.prepareDeleteRelationship().setComponentId(relationship.getId())
						.build("info@b2international.com", artefact.branchPath, "Deleted due to duplicate artefact id.")
						.executeSync(eventBus, timeout);
			}

		} else if (componentCategory == ComponentCategory.DESCRIPTION) {

			SnomedDescriptions descriptions = SnomedRequests.prepareSearchDescription().one()
					.setComponentIds(Collections.singleton(Long.toString(artefact.snomedId))).build(artefact.branchPath)
					.executeSync(eventBus, timeout);
			for (ISnomedDescription description : descriptions) {

				if (!description.isActive()) {
					interpreter.println("**** Inactive description found, skipping" + description);
					System.err.println("**** Inactive description found, skipping!" + description);
					continue;
				}

				SnomedRequests.prepareNewDescription().setIdFromNamespace(snomedIdToReplace.getNamespace())
						.setAcceptability(description.getAcceptabilityMap())
						.setCaseSignificance(description.getCaseSignificance()).setConceptId(description.getConceptId())
						.setLanguageCode(description.getLanguageCode()).setModuleId(description.getModuleId())
						.setTerm(description.getTerm()).setTypeId(description.getTypeId())
						.build("info@b2international.com", artefact.branchPath,
								"Id replaced due to duplicate artefact present.")
						.executeSync(eventBus, timeout);

				// delete the replaced component
				SnomedRequests.prepareDeleteDescription().setComponentId(description.getId())
						.build("info@b2international.com", artefact.branchPath, "Deleted due to duplicate artefact id.")
						.executeSync(eventBus, timeout);
			}

		} else {
			interpreter.println("Not handled: " + componentCategory);
		}

	}

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

	public synchronized void checkServices(CommandInterpreter interpreter) {

		interpreter.println("Checking core services...");

		try {
			Collection<ServiceRegistryEntry<?>> services = ApplicationContext.getInstance().checkServices();
			for (ServiceRegistryEntry<?> entry : services) {
				interpreter.println(String.format("Interface: %s : %s", entry.getServiceInterface(), entry.getImplementation()));
			}
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