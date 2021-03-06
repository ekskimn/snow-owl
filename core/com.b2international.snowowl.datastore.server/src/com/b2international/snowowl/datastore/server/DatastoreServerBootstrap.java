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
package com.b2international.snowowl.datastore.server;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.net4j.jvm.IJVMConnector;
import org.eclipse.net4j.jvm.JVMUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.api.SnowowlServiceException;
import com.b2international.snowowl.core.api.index.IIndexServerServiceManager;
import com.b2international.snowowl.core.config.ClientPreferences;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.core.setup.ModuleConfig;
import com.b2international.snowowl.core.setup.PreRunCapableBootstrapFragment;
import com.b2international.snowowl.core.users.SpecialUserStore;
import com.b2international.snowowl.datastore.cdo.CDOConnectionFactoryProvider;
import com.b2international.snowowl.datastore.cdo.ICDORepositoryManager;
import com.b2international.snowowl.datastore.config.RepositoryConfiguration;
import com.b2international.snowowl.datastore.net4j.Net4jUtils;
import com.b2international.snowowl.datastore.server.index.IndexServerServiceManager;
import com.b2international.snowowl.datastore.server.index.SingleDirectoryIndexManager;
import com.b2international.snowowl.datastore.server.index.SingleDirectoryIndexManagerImpl;
import com.b2international.snowowl.datastore.server.internal.DefaultRepositoryManager;
import com.b2international.snowowl.datastore.server.internal.ExtensionBasedEditingContextFactoryProvider;
import com.b2international.snowowl.datastore.server.internal.ExtensionBasedRepositoryClassLoaderProviderRegistry;
import com.b2international.snowowl.datastore.server.session.ApplicationSessionManager;
import com.b2international.snowowl.datastore.server.session.LogListener;
import com.b2international.snowowl.datastore.server.session.VersionProcessor;
import com.b2international.snowowl.datastore.serviceconfig.ServiceConfigJobManager;
import com.b2international.snowowl.datastore.session.IApplicationSessionManager;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.eventbus.net4j.EventBusNet4jUtil;
import com.b2international.snowowl.rpc.RpcConfiguration;
import com.b2international.snowowl.rpc.RpcProtocol;
import com.b2international.snowowl.rpc.RpcUtil;
import com.google.common.base.Stopwatch;

/**
 * @since 3.3
 */
@ModuleConfig(fieldName = "reviewManager", type = ReviewConfiguration.class)
public class DatastoreServerBootstrap implements PreRunCapableBootstrapFragment {

	private static final Logger LOG = LoggerFactory.getLogger(DatastoreServerBootstrap.class);
	
	@Override
	public void init(SnowOwlConfiguration configuration, Environment env) throws Exception {
		final IManagedContainer container = env.container();
		final RpcConfiguration rpcConfig = configuration.getModuleConfig(RpcConfiguration.class);
		LOG.debug("Preparing RPC communication with config {}", rpcConfig);
		RpcUtil.prepareContainer(container, rpcConfig);
		LOG.debug("Preparing EventBus communication");
		EventBusNet4jUtil.prepareContainer(container);
		env.services().registerService(IEventBus.class, EventBusNet4jUtil.getBus(container));
	}

	@Override
	public void preRun(SnowOwlConfiguration configuration, Environment env) {
		if (env.isServer() || env.isEmbedded()) {
			LOG.debug(">>> Starting server-side datastore bundle.");
			final IManagedContainer container = env.container();
			final Stopwatch serverStopwatch = Stopwatch.createStarted();
			
			RpcUtil.getInitialServerSession(container).registerServiceLookup(new RpcServerServiceLookup());
			final ApplicationSessionManager manager = new ApplicationSessionManager(configuration);
			manager.addListener(new LogListener());
			manager.addListener(new VersionProcessor());
			
			env.services().registerService(IApplicationSessionManager.class, manager);
			env.services().registerService(InternalApplicationSessionManager.class, manager);
			
			final ClassLoader managerClassLoader = manager.getClass().getClassLoader();
			RpcUtil.getInitialServerSession(container).registerClassLoader(IApplicationSessionManager.class, managerClassLoader);
			RpcUtil.getInitialServerSession(container).registerClassLoader(InternalApplicationSessionManager.class, managerClassLoader);
			
			final ICDORepositoryManager cdoRepositoryManager = CDORepositoryManager.getInstance();
			cdoRepositoryManager.activate();
			env.services().registerService(ICDORepositoryManager.class, cdoRepositoryManager);
			
			// register index manager services, one for branching, one for single directory
			// TODO would be nice to merge these into one single IndexManager
			env.services().registerService(IIndexServerServiceManager.class, IndexServerServiceManager.INSTANCE);
			env.services().registerService(SingleDirectoryIndexManager.class, new SingleDirectoryIndexManagerImpl());

			env.services().registerService(RepositoryManager.class, new DefaultRepositoryManager());
			env.services().registerService(EditingContextFactoryProvider.class, new ExtensionBasedEditingContextFactoryProvider());
			env.services().registerService(RepositoryClassLoaderProviderRegistry.class, new ExtensionBasedRepositoryClassLoaderProviderRegistry());
			
			LOG.debug("<<< Server-side datastore bundle started. [{}]", serverStopwatch);
		} else {
			LOG.debug("Snow Owl application is running in remote mode.");
			LOG.info("Connecting to Snow Owl Terminology Server at {}", env.service(ClientPreferences.class).getCDOUrl());
		}
		if (configuration.isSystemUserNeeded() || env.isServer()) {
			try {
				connectSystemUser(env.container());
			} catch (SnowowlServiceException e) {
				throw new SnowowlRuntimeException(e);
			}
		}
	}
	
	@Override
	public void run(SnowOwlConfiguration configuration, Environment env, IProgressMonitor monitor) throws Exception {
		ServiceConfigJobManager.INSTANCE.registerServices(monitor);
		
		if (env.isEmbedded() || env.isServer()) {
			initializeRepositories(configuration, env);
		}
	}

	private void initializeRepositories(SnowOwlConfiguration configuration, Environment env) {
		
		final Stopwatch branchStopwatch = Stopwatch.createStarted();
		LOG.debug(">>> Initializing branch and review services.");
		
		final DefaultRepositoryManager repositories = (DefaultRepositoryManager) env.service(RepositoryManager.class);
		
		RepositoryConfiguration repositoryConfig = configuration.getModuleConfig(RepositoryConfiguration.class);
		final ICDORepositoryManager cdoRepositoryManager = env.service(ICDORepositoryManager.class);
		for (String repositoryId : cdoRepositoryManager.uuidKeySet()) {
			repositories
				.prepareCreate(repositoryId, cdoRepositoryManager.getByUuid(repositoryId).getSnowOwlTerminologyComponentId())
				.setNumberOfWorkers(repositoryConfig.getNumberOfWorkers())
				.setMergeMaxResults(repositoryConfig.getMergeMaxResults())
				.build(env);
		}
		
		LOG.debug("<<< Branch and review services registered. [{}]", branchStopwatch);
	}

	private void connectSystemUser(IManagedContainer container) throws SnowowlServiceException {
		// Normally this is done for us by CDOConnectionFactory
		final IJVMConnector connector = JVMUtil.getConnector(container, Net4jUtils.NET_4_J_CONNECTOR_NAME);
		final RpcProtocol clientProtocol = RpcUtil.getRpcClientProtocol(container);
		clientProtocol.open(connector);

		RpcUtil.getRpcClientProxy(InternalApplicationSessionManager.class).connectSystemUser();
		CDOConnectionFactoryProvider.INSTANCE.getConnectionFactory().connect(SpecialUserStore.SYSTEM_USER);
	}
}
