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
package com.b2international.snowowl.logging.amq;

import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.setup.DefaultBootstrapFragment;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.core.setup.ModuleConfig;

@ModuleConfig(fieldName = "traceability", type = TraceabilityConfig.class)
public class ActiveMQBootstrap extends DefaultBootstrapFragment {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQBootstrap.class);
	
	@Override
	public void run(SnowOwlConfiguration configuration, Environment env, IProgressMonitor monitor) throws Exception {
		
		final TraceabilityConfig moduleConfig = configuration.getModuleConfig(TraceabilityConfig.class);
		if (moduleConfig == null || !moduleConfig.isJmsAppenderEnabled()) {
			LOGGER.info("Traceability JMS appender is not enabled.");
			return;
		}
		
		final ActiveMQAppender appender = new ActiveMQAppender();
		appender.setProviderURL(moduleConfig.getProviderUrl());
		appender.setUserName(moduleConfig.getUserName());
		appender.setPassword(moduleConfig.getPassword());
		
		final Runnable appenderCloser = new Runnable() { @Override public void run() {
			appender.stop();	
		}};
		
		Runtime.getRuntime().addShutdownHook(new Thread(appenderCloser));
		
		appender.start();
		final Logger traceabilityLogger = LoggerFactory.getLogger("traceability");
		((ch.qos.logback.classic.Logger) traceabilityLogger).addAppender(appender);
		
		LOGGER.info("Traceability JMS appender connected to {}.", moduleConfig.getProviderUrl());
	}
}
