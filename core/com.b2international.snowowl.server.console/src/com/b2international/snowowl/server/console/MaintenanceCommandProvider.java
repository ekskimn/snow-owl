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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.search.Query;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.ApplicationContext.ServiceRegistryEntry;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.terminology.ComponentCategory;
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
import com.b2international.snowowl.snomed.datastore.server.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * OSGI command contribution with Snow Owl commands.
 * 
 *
 */
public class MaintenanceCommandProvider implements CommandProvider {

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
		// buffer.append("\tsnowowl test - Execute Snow Owl server smoke
		// test\n");
		buffer.append("\tsnowowl checkservices - Checks the core services presence\n");
		buffer.append(
				"\tsnowowl dbcreateindex [nsUri] - creates the CDO_CREATED index on the proper DB tables for all classes contained by a package identified by its unique namspace URI\n");
		buffer.append("\tsnowowl listrepositories - prints all the repositories in the system \n");
		buffer.append("\tsnowowl listbranches [repository] - prints all the branches in the system for a repository\n");
		buffer.append("\tsnowowl checkdupids - checks the SNOMED CT repository for duplicate ids\n");
		return buffer.toString();
	}

	/**
	 * Reflective template method declaratively registered. Needs to start with
	 * "_".
	 * 
	 * @param interpreter
	 */
	public void _snowowl(CommandInterpreter interpreter) {
		try {
			String cmd = interpreter.nextArgument();

			if ("checkservices".equals(cmd)) {
				checkServices(interpreter);
				return;
			}

			// if ("test".equals(cmd)) {
			// test(interpreter);
			// return;
			// }

			if ("dbcreateindex".equals(cmd)) {
				executeCreateDbIndex(interpreter);
				return;
			}

			if ("listrepositories".equals(cmd)) {
				listRepositories(interpreter);
				return;
			}

			if ("listbranches".equals(cmd)) {
				listBranches(interpreter);
				return;
			}

			if ("checkdupids".equals(cmd)) {
				checkDuplicateIds(interpreter);
				return;
			}

			interpreter.println(getHelp());
		} catch (Exception ex) {
			interpreter.println(ex.getMessage());
		}
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
		ICDORepositoryManager repositoryManager = ApplicationContext.getServiceForClass(ICDORepositoryManager.class);
		Set<String> uuidKeySet = repositoryManager.uuidKeySet();
		if (!uuidKeySet.isEmpty()) {
			interpreter.println("Repositories:");
			for (String repositoryName : uuidKeySet) {
				interpreter.println("  " + repositoryName);
			}
		}
	}

	public synchronized void listBranches(CommandInterpreter interpreter)
			throws InterruptedException, ExecutionException {

		String repositoryName = interpreter.nextArgument();
		if (isValidRepositoryName(repositoryName, interpreter)) {
			IEventBus eventBus = ApplicationContext.getInstance().getService(IEventBus.class);
			interpreter.println("Repository " + repositoryName + " branches:");
			Branch branch = RepositoryRequests.branching(repositoryName).prepareGet("MAIN").executeSync(eventBus, 1000);
			processBranch(branch, 0, interpreter);
		}
	}

	// Depth-first traversal
	private void processBranch(Branch childBranch, int indent, CommandInterpreter interpreter) {

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			sb.append(' ');

		}
		sb.append(childBranch.name());
		interpreter.println(sb.toString());
		indent++;
		Collection<? extends Branch> children = childBranch.children();
		for (Branch branch : children) {
			processBranch(branch, indent, interpreter);
		}
		indent--;
	}

	private void checkDuplicateIds(CommandInterpreter interpreter) throws InterruptedException, ExecutionException {
		IEventBus eventBus = ApplicationContext.getInstance().getService(IEventBus.class);
		Branch mainBranch = RepositoryRequests.branching("snomedStore").prepareGet("MAIN").executeSync(eventBus);

		Map<Long, Artefact> artefactMap = Maps.newHashMap();
		chechBranch(mainBranch, artefactMap, interpreter);
	}

	// Depth-first traversal
	private void chechBranch(Branch childBranch, Map<Long, Artefact> artefactMap, CommandInterpreter interpreter) {

		// These are already published versions with no dups
		List<String> skipVersions = Lists.newArrayList("2002-01-31", "2002-07-31", "2003-01-31", "2003-07-31",
				"2003-10-31", "2004-01-31", "2004-07-31", "2004-09-30", "2005-01-31", "2005-07-31", "2005-09-30",
				"2006-01-31", "2006-07-31", "2006-09-30", "2007-01-31", "2007-07-31", "2007-10-01", "2008-01-31",
				"2008-07-31", "2008-10-31", "2009-01-31", "2009-07-31", "2009-10-31", "2010-01-31", "2010-07-31",
				"2010-10-01", "2011-01-31", "2013-07-31", "2014-01-31", "2014-07-31", "2015-01-31", "2011-07-31",
				"2011-10-01", "2012-01-31", "2012-07-31", "2013-01-31", "2015-07-31");

		if (!skipVersions.contains(childBranch.name())) {
			interpreter.println("Processing: " + childBranch.name());
			processBranchContent(childBranch, artefactMap, interpreter);
		}

		Collection<? extends Branch> children = childBranch.children();
		for (Branch branch : children) {
			chechBranch(branch, artefactMap, interpreter);
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
			interpreter.println("Event triggered to replace the id for the component on the child branch:" + otherArtefactPath);
			replaceComponent(otherArtefact, interpreter);
		} else {
			interpreter.println("No parent child relation between the artefacts, event trigggered to replace the id on the newer branch: "
					+ otherArtefactPath);
			replaceComponent(otherArtefact, interpreter);
		}
	}

	private void replaceComponent(final Artefact artefact, CommandInterpreter interpreter) {

		final IEventBus eventBus = ApplicationContext.getInstance().getService(IEventBus.class);

		final SnomedIdentifier snomedIdToReplace = SnomedIdentifiers.create(Long.toString(artefact.snomedId));
		interpreter.println("Replacing component: " + snomedIdToReplace.getComponentCategory());
		ComponentCategory componentCategory = snomedIdToReplace.getComponentCategory();

		if (componentCategory == ComponentCategory.RELATIONSHIP) {

			SnomedRequests.prepareSearchRelationship().one()
					.setComponentIds(Collections.singleton(Long.toString(artefact.snomedId))).build(artefact.branchPath)
					.execute(eventBus).then(new Function<SnomedRelationships, Void>() {

						@Override
						public Void apply(SnomedRelationships relationships) {
							for (ISnomedRelationship relationship : relationships) {
								SnomedRequests.prepareNewRelationship()
										.setIdFromNamespace(snomedIdToReplace.getNamespace())
										.setCharacteristicType(relationship.getCharacteristicType())
										.setDestinationId(relationship.getDestinationId())
										.setGroup(relationship.getGroup())
										.setModifier(relationship.getModifier())
										.setModuleId(relationship.getModuleId())
										.setSourceId(relationship.getSourceId())
										.setTypeId(relationship.getTypeId())
										.setUnionGroup(relationship.getUnionGroup())
										.build("info@b2international.com",
												artefact.branchPath, "Id replaced due to duplicate artefact present.")
										.execute(eventBus);
							}
							return null;
						}
					});

		} else if (componentCategory == ComponentCategory.DESCRIPTION) {

			SnomedRequests.prepareSearchDescription().one()
					.setComponentIds(Collections.singleton(Long.toString(artefact.snomedId))).build(artefact.branchPath)
					.execute(eventBus).then(new Function<SnomedDescriptions, Void>() {

						@Override
						public Void apply(SnomedDescriptions descriptions) {
							for (ISnomedDescription description : descriptions) {
								SnomedRequests.prepareNewDescription()
										.setIdFromNamespace(snomedIdToReplace.getNamespace())
										.setAcceptability(description.getAcceptabilityMap())
										.setCaseSignificance(description.getCaseSignificance())
										.setConceptId(description.getConceptId())
										.setLanguageCode(description.getLanguageCode())
										.setModuleId(description.getModuleId())
										.setTerm(description.getTerm())
										.setTypeId(description.getTypeId())
										.build("info@b2international.com",
												artefact.branchPath, "Id replaced due to duplicate artefact present.")
										.execute(eventBus);
							}
							return null;
						}
					});

		} else {
			interpreter.println("Not handled: " + componentCategory);
		}

	}

	public synchronized void checkServices(CommandInterpreter ci) {

		ci.println("Checking core services...");
		try {

			Collection<ServiceRegistryEntry<?>> services = ApplicationContext.getInstance().checkServices();
			for (ServiceRegistryEntry<?> entry : services) {
				ci.println("Interface: " + entry.getServiceInterface() + " : " + entry.getImplementation());
			}
			ci.println("Core services are registered properly and available for use.");

		} catch (final Throwable t) {
			ci.print("Error: " + t.getMessage());
		}
	}

	private boolean isValidRepositoryName(String repositoryName, CommandInterpreter interpreter) {
		if (repositoryName == null) {
			interpreter.println(
					"Repository name should be specified. Execute 'listrepositories' to see the available repositories.");
			return false;
		}

		ICDORepositoryManager repositoryManager = ApplicationContext.getServiceForClass(ICDORepositoryManager.class);
		Set<String> uuidKeySet = repositoryManager.uuidKeySet();
		if (!uuidKeySet.contains(repositoryName)) {
			interpreter.println("Could not find repository called: " + repositoryName);
			interpreter.println("Available repository names are: " + uuidKeySet);
			return false;
		}
		return true;

	}

	// /**
	// * OSGi console contribution, the test touches cdo and index stores.
	// * @param ci
	// */
	// public synchronized void test(CommandInterpreter ci) {
	//
	// ci.println("Smoke testing the Snow Owl server....");
	// SnomedConceptIndexEntry rootConceptMini = null;
	// SnomedClientTerminologyBrowser terminologyBrowser =
	// ApplicationContext.getInstance().getService(SnomedClientTerminologyBrowser.class);
	// Collection<SnomedConceptIndexEntry> rootConcepts =
	// terminologyBrowser.getRootConcepts();
	// for (SnomedConceptIndexEntry rootConcept : rootConcepts) {
	// rootConceptMini = rootConcept;
	// ci.println(" Root concept from the semantic cache: " + rootConcept);
	// }
	//
	// ci.println(" Semantic cache size: " +
	// terminologyBrowser.getConceptCount());
	// SnomedClientIndexService indexSearcher =
	// ApplicationContext.getInstance().getService(SnomedClientIndexService.class);
	//
	// SnomedConceptFullQueryAdapter adapter = new
	// SnomedConceptFullQueryAdapter(rootConceptMini.getId(),
	// SnomedConceptFullQueryAdapter.SEARCH_BY_CONCEPT_ID);
	// List<SnomedConceptIndexEntry> search = indexSearcher.search(adapter);
	// ci.println(" Root concept from the index store: " +
	// search.get(0).getLabel());
	//
	// SnomedEditingContext editingContext = null;
	// try {
	// editingContext = new SnomedEditingContext();
	// Concept rootConcept = new
	// SnomedConceptLookupService().getComponent(rootConceptMini.getId(),
	// editingContext.getTransaction());
	// ci.println(" Root concept from the main repository: " +
	// rootConcept.getFullySpecifiedName());
	// } finally {
	// editingContext.close();
	// }
	// }
}