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
package com.b2international.snowowl.core.events;

import java.io.Serializable;

import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.eventbus.IHandler;
import com.b2international.snowowl.eventbus.IMessage;

/**
 * {@link Event}s represent messages coming from different parts of the system to execute something or just notify us.
 *
 * @since 4.1
 */
public interface Event extends Serializable {

	void send(IEventBus bus);
	
	void publish(IEventBus bus);
	
	void send(IEventBus bus, IHandler<IMessage> replyHandler);
	
	<T> Promise<T> send(IEventBus bus, Class<T> returnType);
	
}
