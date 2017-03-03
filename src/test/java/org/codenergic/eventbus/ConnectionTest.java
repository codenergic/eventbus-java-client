/*
 * Copyright (c) 2017 the original author or authors.
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
package org.codenergic.eventbus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.codenergic.eventbus.handler.ConnectionHandler;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionTest {
	@Test
	public void testOpenAndCloseConnection() throws Exception {
		final BlockingQueue<Boolean> connectionOpen = new ArrayBlockingQueue<Boolean>(1);

		EventBus eventBus = EventBusAdapter.connect("wss://simcat.herokuapp.com/eventbus/websocket");
		eventBus.onOpen(new ConnectionHandler() {
			@Override
			public void handle() {
				connectionOpen.add(true);
			}
		});

		eventBus.onClose(new ConnectionHandler() {
			@Override
			public void handle() {
				connectionOpen.add(false);
			}
		});

		eventBus.open();
		Assert.assertTrue(connectionOpen.poll(5, TimeUnit.SECONDS));

		eventBus.close();
		Assert.assertFalse(connectionOpen.poll(5, TimeUnit.SECONDS));
	}
}
