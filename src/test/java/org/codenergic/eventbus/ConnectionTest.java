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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionTest {
	private String connectionAddress;

	@After
	public void after() {
		TestHelper.stopServer();
	}

	@Before
	public void before() throws InterruptedException {
		connectionAddress = TestHelper.startServer();
	}

	@Test
	public void testOpenAndCloseConnectionAsynchronously() throws Exception {
		EventBus eventBus = EventBus.newInstance(connectionAddress);
		assertThat(eventBus.open().get(2, TimeUnit.SECONDS)).isEqualTo(eventBus);
		eventBus.close();
	}

	@Test
	public void testOpenAndCloseConnectionSynchronously() throws Exception {
		final BlockingQueue<Boolean> connectionOpen = new ArrayBlockingQueue<>(1);

		EventBus eventBus = EventBus.newInstance(connectionAddress);
		eventBus.onOpen((eb) -> connectionOpen.add(true));
		eventBus.onClose((eb) -> connectionOpen.add(false));

		assertThat(eventBus.openSync()).isEqualTo(eventBus);
		assertThat(connectionOpen.poll(2, TimeUnit.SECONDS)).isNotNull().isTrue();

		eventBus.close();
		assertThat(connectionOpen.poll(2, TimeUnit.SECONDS)).isNotNull().isFalse();
	}
}
