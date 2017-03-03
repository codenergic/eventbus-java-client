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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.codenergic.eventbus.handler.ConnectionHandler;
import org.codenergic.eventbus.handler.Message;
import org.codenergic.eventbus.handler.MessageHandler;
import org.codenergic.eventbus.handler.ReplyHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

public final class EventBusAdapter implements EventBus {
	private Map<String, List<MessageHandler>> messageHandlers = new HashMap<String, List<MessageHandler>>();
	private Map<String, ReplyHandler> replyHandlers = new HashMap<String, ReplyHandler>();

	private ObjectMapper objectMapper;
	private ConnectionHandler onOpenHandler;
	private ConnectionHandler onCloseHandler;

	private Timer timer = new Timer();
	private int pingInterval;

	private WebSocket webSocket;
	private int state;

	private EventBusAdapter(String address, EventBusOptions options) throws IOException {
		this.objectMapper = options.getObjectMapper();
		this.pingInterval = options.getPingInterval();
		this.webSocket = new WebSocketFactory()
				.createSocket(address);

		this.state = EventBus.CONNECTING;

		this.webSocket.addListener(new WebSocketAdapter() {
			@Override
			public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
				super.onError(websocket, cause);
			}

			@Override
			public void onConnected(final WebSocket websocket, Map<String, List<String>> headers) throws Exception {
				state = EventBus.OPEN;
				if (onOpenHandler != null) {
					onOpenHandler.handle();
				}
				onOpen();
			}

			@Override
			public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
				state = EventBus.CLOSED;
				if (onCloseHandler != null) {
					onCloseHandler.handle();
				}
				onClose();
			}

			@Override
			public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
				onMessage(websocket, binary);
			}
		});
	}

	public static EventBus connect(String address, EventBusOptions options) throws IOException {
		return new EventBusAdapter(address, options);
	}

	public static EventBus connect(String address) throws IOException {
		return EventBusAdapter.connect(address, new EventBusOptions());
	}

	@Override
	public EventBus open() throws Exception {
		webSocket.connect();
		return this;
	}

	@Override
	public void onOpen(ConnectionHandler connectionHandler) {
		this.onOpenHandler = connectionHandler;
	}

	@Override
	public void onClose(ConnectionHandler connectionHandler) {
		this.onCloseHandler = connectionHandler;
	}

	@Override
	public void send(String address, String message) {
		send(address, message, null, null);
	}

	@Override
	public void send(String address, String message, Map<String, Object> headers) {
		send(address, message, headers, null);
	}

	@Override
	public void send(String address, String message, Map<String, Object> headers, ReplyHandler replyHandler) {
		sendMessage(Message.TYPE_SEND, address, message, headers, replyHandler);
	}

	@Override
	public void publish(String address, String message, Map<String, Object> headers) {
		sendMessage(Message.TYPE_PUBLISH, address, message, headers, null);
	}

	@Override
	public void registerHandler(String address, MessageHandler handler) {
		registerHandler(address, null, handler);
	}

	@Override
	public void registerHandler(String address, Map<String, Object> headers, MessageHandler handler) {
		if (!messageHandlers.containsKey(address)) {
			messageHandlers.put(address, new ArrayList<MessageHandler>());
		}

		sendMessage(Message.TYPE_REGISTER, address, null, headers, null);
		messageHandlers.get(address).add(handler);
	}

	@Override
	public void unregisterHandler(String address, MessageHandler handler) {
		unregisterHandler(address, null, handler);
	}

	@Override
	public void unregisterHandler(String address, Map<String, Object> headers, MessageHandler handler) {
		List<MessageHandler> handlers = messageHandlers.get(address);
		if (handlers == null || handlers.isEmpty()) {
			return;
		}

		sendMessage(Message.TYPE_UNREGISTER, address, null, headers, null);
		handlers.remove(handler);
	}

	@Override
	public void close() {
		state = EventBus.CLOSING;
		webSocket.sendClose();
	}

	private void sendMessage(String type, String address, String message, Map<String, Object> headers,
			ReplyHandler replyHandler) {
		if (state != EventBus.OPEN) {
			throw new RuntimeException("Invalid state");
		}

		Message msg = null;
		if (Message.TYPE_SEND.equals(type) && replyHandler != null) {
			String replyAddress = UUID.randomUUID().toString();
			msg = new Message(Message.TYPE_SEND, address, headers, message, replyAddress);
			replyHandlers.put(replyAddress, replyHandler);
		} else {
			msg = new Message(type, address, headers, message);
		}

		try {
			webSocket.sendBinary(objectMapper.writeValueAsBytes(msg));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private void onOpen() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// send ping
				webSocket.sendBinary("{\"type\":\"ping\"}".getBytes());
			}
		}, 0, pingInterval);
	}

	private void onClose() {
		timer.cancel();
	}

	private void onMessage(WebSocket ws, byte[] body) throws JsonProcessingException, IOException {
		JsonNode json = objectMapper.readTree(body);

		if (json.has("replyAddress")) {
			// currently not supported
		}

		String address = json.get("address").textValue();
		Message message = objectMapper.treeToValue(json, Message.class);

		if (messageHandlers.containsKey(address)) {
			List<MessageHandler> handlers = messageHandlers.get(address);
			for (MessageHandler handler : handlers) {
				handler.handle(message);
			}
		} else if (replyHandlers.containsKey(address)) {
			ReplyHandler handler = replyHandlers.get(address);
			replyHandlers.remove(address);
			handler.handle(message);
		} else {

		}
	}
}
