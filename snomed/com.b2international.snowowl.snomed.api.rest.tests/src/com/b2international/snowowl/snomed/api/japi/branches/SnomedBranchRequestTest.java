/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.japi.branches;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.junit.Before;
import org.junit.Test;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.core.merge.Merge;
import com.b2international.snowowl.datastore.request.Branching;
import com.b2international.snowowl.datastore.request.CommitInfo;
import com.b2international.snowowl.datastore.server.internal.CDOBasedRepository;
import com.b2international.snowowl.datastore.server.internal.branch.InternalCDOBasedBranch;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.Acceptability;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.request.SnomedDescriptionCreateRequestBuilder;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * @since 4.7
 */
public class SnomedBranchRequestTest {

	private IEventBus bus;
	private CDOBranchManager cdoBranchManager;
	
	@Before
	public void setup() {
		bus = ApplicationContext.getInstance().getService(IEventBus.class);
		cdoBranchManager = getSnomedCdoBranchManager();
	}

	@Test
	public void createTwoBranchesSameTimeWithSameName() throws Exception {
		final Branching branches = SnomedRequests.branching();
		
		// try to create two branches at the same time
		final String branchName = UUID.randomUUID().toString();
		final Promise<Branch> first = branches.prepareCreate().setParent(Branch.MAIN_PATH).setName(branchName).build().execute(bus);
		final Promise<Branch> second = branches.prepareCreate().setParent(Branch.MAIN_PATH).setName(branchName).build().execute(bus);
		final String error = Promise.all(first, second)
			.then(new Function<List<Object>, String>() {
				@Override
				public String apply(List<Object> input) {
					final Branch first = (Branch) input.get(0);
					final Branch second = (Branch) input.get(1);
					return first.baseTimestamp() == second.baseTimestamp() ? null : "Two branches created with the same name but different baseTimestamp";
				}
			})
			.fail(new Function<Throwable, String>() {
				@Override
				public String apply(Throwable input) {
					return input.getMessage() != null ? input.getMessage() : Throwables.getRootCause(input).getClass().getSimpleName();
				}
			})
			.getSync();
		
		if (error != null) {
			assertThat(error, allOf(containsString("Branch with '"), containsString("' identifier already exists")));
		}
		
		assertEquals(1, getCdoBranches(branchName).size());
	}
	
	@Test
	public void createTwoBranchesSameTimeWithDifferentName() throws Exception {
		final Branching branches = SnomedRequests.branching();
		
		// try to create two branches at the same time
		final String branchA = UUID.randomUUID().toString();
		final String branchB = UUID.randomUUID().toString();
		final Promise<Branch> first = branches.prepareCreate().setParent(Branch.MAIN_PATH).setName(branchA).build().execute(bus);
		final Promise<Branch> second = branches.prepareCreate().setParent(Branch.MAIN_PATH).setName(branchB).build().execute(bus);
		
		Promise.all(first, second).then(new Function<List<Object>, String>() {
			@Override
			public String apply(List<Object> input) {
				final InternalCDOBasedBranch main = (InternalCDOBasedBranch) branches.prepareGet(Branch.MAIN_PATH).executeSync(bus);
				final InternalCDOBasedBranch createdFirst;
				final InternalCDOBasedBranch createdSecond;
				
				if (((Branch) input.get(0)).headTimestamp() > ((Branch) input.get(1)).headTimestamp()) {
					createdFirst = (InternalCDOBasedBranch) input.get(1);
					createdSecond = (InternalCDOBasedBranch) input.get(0);
				} else {
					createdFirst = (InternalCDOBasedBranch) input.get(0);
					createdSecond = (InternalCDOBasedBranch) input.get(1);
				}
				
				assertBranchesCreated(branchA, branchB, ((Branch) input.get(0)), ((Branch) input.get(1)));
				assertBranchSegmentsValid(main, createdFirst, createdSecond);
				return null;
			}
		})
		.getSync();
	}

	@Test
	public void createBranchAndCommitToParent() throws Exception {
		final Branching branches = SnomedRequests.branching();
		
		final String branchA = UUID.randomUUID().toString();
		final String branchB = UUID.randomUUID().toString();

		final Branch first = branches.prepareCreate()
				.setParent(Branch.MAIN_PATH)
				.setName(branchA)
				.build()
				.execute(bus)
				.getSync();
		
		final SnomedDescriptionCreateRequestBuilder fsnBuilder = SnomedRequests.prepareNewDescription()
				.setModuleId(Concepts.MODULE_ROOT)
				.setTerm("FSN " + branchA)
				.setTypeId(Concepts.FULLY_SPECIFIED_NAME)
				.setAcceptability(ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_UK, Acceptability.PREFERRED));
		
		final SnomedDescriptionCreateRequestBuilder ptBuilder = SnomedRequests.prepareNewDescription()
				.setModuleId(Concepts.MODULE_ROOT)
				.setTerm("PT " + branchA)
				.setTypeId(Concepts.SYNONYM)
				.setAcceptability(ImmutableMap.of(Concepts.REFSET_LANGUAGE_TYPE_UK, Acceptability.PREFERRED));
		
		final Request<ServiceProvider, CommitInfo> conceptRequest = SnomedRequests.prepareNewConcept()
				.setParent(Concepts.ROOT_CONCEPT)
				.setModuleId(Concepts.MODULE_ROOT)
				.addDescription(fsnBuilder)
				.addDescription(ptBuilder)
				.build("user", first.path(), "Created new concept");
		
		final CommitInfo info = conceptRequest.execute(bus).getSync();
		final String conceptId = info.getResultAs(String.class);
		final Promise<Merge> merge = branches.prepareMerge()
				.setSource(first.path())
				.setTarget(first.parentPath())
				.setCommitComment("Merging changes")
				.build()
				.execute(bus);
		
		final Promise<Branch> second = branches.prepareCreate()
				.setParent(first.parentPath())
				.setName(branchB)
				.build()
				.execute(bus);
		
		Promise.all(merge, second).then(new Function<List<Object>, Void>() {
			@Override
			public Void apply(final List<Object> input) {
				final Merge merge = (Merge) input.get(0);

				final InternalCDOBasedBranch target = (InternalCDOBasedBranch) branches.prepareGet(merge.getTarget()).executeSync(bus);
				final InternalCDOBasedBranch first = (InternalCDOBasedBranch) branches.prepareGet(merge.getSource()).executeSync(bus);
				final InternalCDOBasedBranch second = (InternalCDOBasedBranch) input.get(1);
				
				final InternalCDOBasedBranch createdFirst;
				final InternalCDOBasedBranch createdSecond;
				
				if (first.headTimestamp() > second.headTimestamp()) {
					createdFirst = second;
					createdSecond = first;
				} else {
					createdFirst = first;
					createdSecond = second;
				}
				
				assertBranchesCreated(branchA, branchB, first, second);
				assertBranchSegmentsValid(target, createdFirst, createdSecond);
				return null;
			}
		})
		.getSync();
		
		// Check that the concept is visible on parent
		SnomedRequests.prepareGetConcept().setComponentId(conceptId).build(first.parentPath()).executeSync(bus);
	}

	private void assertBranchesCreated(final String branchA, final String branchB, final Branch first, final Branch second) {
		assertEquals(branchA, first.name());
		assertEquals(branchB, second.name());
	}
	
	private void assertBranchSegmentsValid(final InternalCDOBasedBranch parent, final InternalCDOBasedBranch createdFirst, final InternalCDOBasedBranch createdSecond) {
		// All segments of the parent except the last should be present in the second child branch's parentSegment collection
		SortedSet<Integer> parentSegments = Sets.newTreeSet(parent.segments());
		parentSegments.remove(parent.segmentId());

		assertEquals(createdSecond.parentSegments().size(), parentSegments.size());
		assertTrue(createdSecond.parentSegments().containsAll(parentSegments));
		
		// Remove the greatest value to get the first child branch's parentSegments
		parentSegments = parentSegments.headSet(parentSegments.last());

		assertEquals(createdFirst.parentSegments().size(), parentSegments.size());
		assertTrue(createdFirst.parentSegments().containsAll(parentSegments));
	}

	private Set<CDOBranch> getCdoBranches(final String branchName) {
		return FluentIterable.from(newArrayList(cdoBranchManager.getMainBranch().getBranches()))
			.filter(new Predicate<CDOBranch>() {
				@Override
				public boolean apply(CDOBranch input) {
					return input.getName().equals(branchName);
				}
			})
			.toSet();
	}
	
	private CDOBranchManager getSnomedCdoBranchManager() {
		final RepositoryManager repositoryManager = ApplicationContext.getInstance().getService(RepositoryManager.class);
		return ((CDOBasedRepository) repositoryManager.get(SnomedDatastoreActivator.REPOSITORY_UUID)).getCdoBranchManager();
	}
}
