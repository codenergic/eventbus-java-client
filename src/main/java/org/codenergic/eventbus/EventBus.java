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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

public interface EventBus {
	static EventBus newInstance(WebSocket webSocket, ObjectMapper objectMapper, int pingInterval) {
		return new EventBusAdapter(webSocket, objectMapper, pingInterval);
	}

	static EventBus newInstance(WebSocket webSocket, ObjectMapper objectMapper) {
		return newInstance(webSocket, objectMapper, 5000);
	}

	static EventBus newInstance(WebSocket webSocket) {
		return newInstance(webSocket, new ObjectMapper());
	}

	static EventBus newInstance(WebSocket webSocket, int pingInterval) {
		return new EventBusAdapter(webSocket, new ObjectMapper(), pingInterval);
	}

	static EventBus newInstance(String address, int pingInterval) {
		try {
			return newInstance(new WebSocketFactory().createSocket(address), pingInterval);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static EventBus newInstance(String address) {
		return newInstance(address, 5000);
	}

	void close();

	void onClose(Consumer<EventBus> connectionHandler);

	void onOpen(Consumer<EventBus> connectionHandler);

	CompletableFuture<EventBus> open();

	EventBus openSync();

	default void publish(String address, String message) {
		publish(address, message, null);
	}

	void publish(String address, String message, Map<String, Object> headers);

	default void registerHandler(String address, Consumer<Message> handler) {
		registerHandler(address, null, handler);
	}

	default void registerHandler(String address, Consumer<Message> handler, BiConsumer<Message, Throwable> errorHandler) {
		registerHandler(address, null, handler, errorHandler);
	}

	default void registerHandler(String address, Map<String, Object> headers, Consumer<Message> handler) {
		registerHandler(address, headers, handler, (m, e) -> {
			// do nothing
		});
	}

	void registerHandler(String address, Map<String, Object> headers, Consumer<Message> handler, BiConsumer<Message, Throwable> errorHandler);

	default void send(String address, String message) {
		send(address, message, null);
	}

	default void send(String address, String message, Map<String, Object> headers) {
		send(address, message, headers, null);
	}

	default void send(String address, String message, Map<String, Object> headers, Consumer<Message> replyHandler) {
		send(address, message, headers, replyHandler, (m, e) -> {
			// do nothing
		});
	}

	void send(String address, String message, Map<String, Object> headers, Consumer<Message> replyHandler, BiConsumer<Message, Throwable> errorHandler);

	default void unregisterHandler(String address, Consumer<Message> handler) {
		unregisterHandler(address, null, handler);
	}

	void unregisterHandler(String address, Map<String, Object> headers, Consumer<Message> handler);
}
