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

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.codenergic.eventbus.handler.Message;
import org.codenergic.eventbus.handler.MessageHandler;
import org.codenergic.eventbus.handler.ReplyHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SendMessageTest {
	private EventBus eventBus;

	@Before
	public void init() throws Exception {
		eventBus = EventBusAdapter.connect("wss://simcat.herokuapp.com/eventbus/websocket").open();
	}

	@Test
	public void testSendMessageWithReply() throws InterruptedException {
		final BlockingQueue<Message> result = new ArrayBlockingQueue<Message>(1);

		for (int i = 0; i < 20; i++) {
			eventBus.send("chat.token", null, null, new ReplyHandler() {
				@Override
				public void handle(Message message) {
					result.add(message);
				}
			});

			Message message = result.poll(5, TimeUnit.SECONDS);
			Assert.assertNotNull(message.getBody());
		}
	}

	@Test
	public void testSendAndSubscribe() throws InterruptedException {
		final BlockingQueue<Message> result = new ArrayBlockingQueue<Message>(1);

		eventBus.registerHandler("chat.message.123", new MessageHandler() {
			@Override
			public void handle(Message message) {
				result.add(message);
			}
		});

		eventBus.send("chat.message", "Hello", Collections.singletonMap("token", (Object) "123"));

		Message message = result.poll(5, TimeUnit.SECONDS);
		Assert.assertEquals("Server: Hello", message.getBody());
	}

	@Test
	public void testPublishAndSubscribe() throws InterruptedException {
		final BlockingQueue<Message> result = new ArrayBlockingQueue<Message>(1);

		eventBus.registerHandler("chat.message.123", new MessageHandler() {
			@Override
			public void handle(Message message) {
				result.add(message);
			}
		});

		eventBus.publish("chat.message", "Hello", Collections.singletonMap("token", (Object) "123"));

		Message message = result.poll(5, TimeUnit.SECONDS);
		Assert.assertEquals("Server: Hello", message.getBody());
	}
}
