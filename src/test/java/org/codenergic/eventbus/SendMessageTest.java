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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.eventbus.DeliveryOptions;

import static org.assertj.core.api.Assertions.assertThat;

public class SendMessageTest {
	private static EventBus eventBus;

	@AfterClass
	public static void after() {
		eventBus.close();
		TestHelper.stopServer();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		eventBus = EventBus.newInstance(TestHelper.startServer()).openSync();
		io.vertx.core.eventbus.EventBus eb = TestHelper.vertx.eventBus();
		eb.consumer("test-address", event -> {
			event.reply(event.address() + "-" + event.body(), new DeliveryOptions().setHeaders(event.headers()));
			eb.send("test-address-reply", event.address() + "-" + event.body(), new DeliveryOptions().setHeaders(event.headers()));
			eb.publish("test-address-publish-reply", event.address() + "-" + event.body(), new DeliveryOptions().setHeaders(event.headers()));
		});
	}

	@Test
	public void testPublishAndSubscribe() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		List<Message> messages = new ArrayList<>();

		String body = UUID.randomUUID().toString();
		Map<String, Object> headers = Collections.singletonMap("token", "123");

		Consumer<Message> messageHandler = message -> {
			messages.add(message);
			latch.countDown();
		};

		eventBus.registerHandler("test-address-publish-reply", messageHandler);
		eventBus.registerHandler("test-address-publish-reply", messageHandler);

		eventBus.publish("test-address", body, headers);

		boolean completed = latch.await(5, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		assertThat(messages).hasSize(2);
		messages.forEach(message -> {
			assertThat(message).isNotNull();
			assertThat(message.getBody()).isEqualTo("test-address-" + body);
			assertThat(message.getHeaders()).isEqualTo(headers);
		});
	}

	@Test
	public void testSendAndSubscribe() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		List<Message> messages = new ArrayList<>();

		String body = UUID.randomUUID().toString();
		Map<String, Object> headers = Collections.singletonMap("token", "123");

		eventBus.registerHandler("test-address-reply", message -> {
			messages.add(message);
			latch.countDown();
		});

		eventBus.send("test-address", body, headers);
		eventBus.send("test-address", body);

		boolean completed = latch.await(5, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		assertThat(messages).hasSize(2);
		assertThat(messages.get(0).getHeaders()).isEqualTo(headers);
		assertThat(messages.get(1).getHeaders()).isNull();
		messages.forEach(message -> {
			assertThat(message).isNotNull();
			assertThat(message.getBody()).isEqualTo("test-address-" + body);
		});
	}

	@Test
	public void testSendMessageWithReply() throws InterruptedException {
		List<String> messageBodies = IntStream.rangeClosed(0, 20)
				.mapToObj(i -> UUID.randomUUID().toString())
				.collect(Collectors.toList());

		CountDownLatch latch = new CountDownLatch(messageBodies.size());
		List<String> messages = new ArrayList<>();

		messageBodies.forEach(body -> {
			eventBus.send("test-address", body, null, message -> {
				messages.add(message.getBody());
				latch.countDown();
			});
		});

		boolean sent = latch.await(5, TimeUnit.SECONDS);
		assertThat(sent).isTrue();
		assertThat(messages.size()).isEqualTo(messageBodies.size());

		messageBodies.forEach(message -> {
			String expectedBody = "test-address-" + message;
			assertThat(messages).contains(expectedBody);
			messages.remove(expectedBody);
		});

		assertThat(messages).isEmpty();
	}

	@Test
	public void testSubscribeAndThrowError() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(4);
		List<Message> messages = new ArrayList<>();
		List<Throwable> throwables = new ArrayList<>();

		String body = UUID.randomUUID().toString();
		eventBus.send("test-address", body, null, message -> {
			messages.add(message);
			latch.countDown();
			throw new IllegalStateException();
		}, (message, throwable) -> {
			latch.countDown();
			throwables.add(throwable);
		});

		eventBus.registerHandler("test-address-reply", message -> {
			messages.add(message);
			latch.countDown();
			throw new IllegalStateException();
		}, ((message, throwable) -> {
			latch.countDown();
			throwables.add(throwable);
		}));
		eventBus.send("test-address", body);

		assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(messages).hasSize(2);
		messages.forEach(message -> assertThat(message.getBody()).isEqualTo("test-address-" + body));

		assertThat(throwables).hasSize(2);
		throwables.forEach(throwable -> assertThat(throwable).isInstanceOf(IllegalStateException.class));
	}

	@Test
	public void testSubscribeAndUnsubscribe() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(2);
		List<String> messages = new ArrayList<>();

		Consumer<Message> messageHandler = message -> {
			messages.add(message.getBody());
			latch.countDown();
		};
		eventBus.registerHandler("test-address-publish-reply", messageHandler);
		eventBus.publish("test-address", "testing");

		boolean sent = latch.await(2, TimeUnit.SECONDS);
		assertThat(sent).isFalse();

		eventBus.unregisterHandler("test-address-publish-reply", messageHandler);
		eventBus.publish("test-address", "testing");

		sent = latch.await(2, TimeUnit.SECONDS);
		assertThat(sent).isFalse();

		assertThat(messages).hasSize(1);
		assertThat(messages.iterator().next()).isEqualTo("test-address-testing");
	}
}
