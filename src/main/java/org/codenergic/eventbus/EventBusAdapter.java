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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

final class EventBusAdapter implements EventBus {
	private static final int CONNECTING = 0;
	private static final int OPEN = 1;
	private static final int CLOSING = 2;
	private static final int CLOSED = 3;

	private final WebSocket webSocket;
	private final ObjectMapper objectMapper;
	private final int pingInterval;
	private final Timer timer = new Timer();
	private final ExecutorService connectThreadPool = Executors.newCachedThreadPool();
	private final Map<String, List<Consumer<Message>>> messageHandlers = new ConcurrentHashMap<>();
	private final Map<String, Consumer<Message>> replyHandlers = new ConcurrentHashMap<>();
	private final Map<Consumer<Message>, BiConsumer<Message, Throwable>> errorHandlers = new ConcurrentHashMap<>();
	private Consumer<EventBus> onOpenHandler;
	private Consumer<EventBus> onCloseHandler;
	private TimerTask pingTask;
	private int state = CONNECTING;

	EventBusAdapter(WebSocket webSocket, ObjectMapper objectMapper, int pingInterval) {
		this.webSocket = webSocket;
		this.objectMapper = objectMapper;
		this.pingInterval = pingInterval;
		init();
	}

	@Override
	public void close() {
		state = CLOSING;
		webSocket.disconnect();
	}

	private void init() {
		this.webSocket.addListener(new WebSocketAdapter() {
			@Override
			public void onBinaryMessage(WebSocket ws, byte[] binary) throws Exception {
				onMessage(binary);
			}

			@Override
			public void onCloseFrame(WebSocket ws, WebSocketFrame frame) {
				state = CLOSED;
				pingTask.cancel();
				Optional.ofNullable(onCloseHandler).ifPresent(h -> h.accept(EventBusAdapter.this));
			}

			@Override
			public void onConnected(final WebSocket ws, Map<String, List<String>> headers) {
				state = OPEN;
				pingTask = new TimerTask() {
					@Override
					public void run() {
						// send ping
						ws.sendBinary("{\"type\":\"ping\"}".getBytes());
					}
				};
				timer.schedule(pingTask, 0, pingInterval);
				Optional.ofNullable(onOpenHandler).ifPresent(h -> h.accept(EventBusAdapter.this));
			}
		});
	}

	@Override
	public void onClose(Consumer<EventBus> connectionHandler) {
		this.onCloseHandler = connectionHandler;
	}

	private void onMessage(byte[] body) throws IOException {
		JsonNode json = objectMapper.readTree(body);

		String address = json.get("address").textValue();
		Message message = objectMapper.treeToValue(json, Message.class);

		if (messageHandlers.containsKey(address)) {
			List<Consumer<Message>> handlers = messageHandlers.get(address);
			handlers.forEach(handler -> onMessage(handler, message));
		} else if (replyHandlers.containsKey(address)) {
			Consumer<Message> handler = replyHandlers.get(address);
			onMessage(handler, message);
			replyHandlers.remove(address);
			errorHandlers.remove(handler);
		}
	}

	private void onMessage(Consumer<Message> messageHandler, Message message) {
		try {
			messageHandler.accept(message);
		} catch (Exception e) {
			errorHandlers.get(messageHandler).accept(message, e);
		}
	}

	@Override
	public void onOpen(Consumer<EventBus> connectionHandler) {
		this.onOpenHandler = connectionHandler;
	}

	@Override
	public CompletableFuture<EventBus> open() {
		return CompletableFuture.supplyAsync(this::openSync, connectThreadPool);
	}

	@Override
	public EventBus openSync() {
		try {
			webSocket.connect();
			return this;
		} catch (WebSocketException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void publish(String address, String message, Map<String, Object> headers) {
		sendMessage(Message.MessageType.PUBLISH, address, message, headers, null, null);
	}

	@Override
	public void registerHandler(String address, Map<String, Object> headers, Consumer<Message> handler, BiConsumer<Message, Throwable> errorHandler) {
		if (!messageHandlers.containsKey(address)) {
			messageHandlers.put(address, new ArrayList<>());
			sendMessage(Message.MessageType.REGISTER, address, null, headers, null, null);
		}

		messageHandlers.get(address).add(handler);
		errorHandlers.put(handler, errorHandler);
	}

	@Override
	public void send(String address, String message, Map<String, Object> headers, Consumer<Message> replyHandler, BiConsumer<Message, Throwable> errorHandler) {
		sendMessage(Message.MessageType.SEND, address, message, headers, replyHandler, errorHandler);
	}

	private void sendMessage(Message.MessageType type, String address, String message,
							 Map<String, Object> headers, Consumer<Message> replyHandler, BiConsumer<Message, Throwable> errorHandler) {
		if (state != OPEN) {
			throw new IllegalStateException("Connection is not currently open");
		}

		Message msg;
		if (Message.MessageType.SEND.equals(type) && replyHandler != null) {
			String replyAddress = UUID.randomUUID().toString();
			msg = new Message(Message.MessageType.SEND, address, headers, message, replyAddress);
			replyHandlers.put(replyAddress, replyHandler);
			if (errorHandler != null) {
				errorHandlers.put(replyHandler, errorHandler);
			}
		} else {
			msg = new Message(type, address, headers, message);
		}

		try {
			webSocket.sendBinary(objectMapper.writeValueAsBytes(msg));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void unregisterHandler(String address, Map<String, Object> headers, Consumer<Message> handler) {
		List<Consumer<Message>> handlers = messageHandlers.get(address);
		if (handlers == null || handlers.isEmpty()) {
			return;
		}

		sendMessage(Message.MessageType.UNREGISTER, address, null, headers, null, null);
		handlers.remove(handler);
		errorHandlers.remove(handler);
	}
}
