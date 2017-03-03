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

import java.util.Map;

import org.codenergic.eventbus.handler.ConnectionHandler;
import org.codenergic.eventbus.handler.MessageHandler;
import org.codenergic.eventbus.handler.ReplyHandler;

public interface EventBus {
	static int CONNECTING = 0;
	static int OPEN = 1;
	static int CLOSING = 2;
	static int CLOSED = 3;

	void onOpen(ConnectionHandler connectionHandler);

	void onClose(ConnectionHandler connectionHandler);

	void send(String address, String message);

	void send(String address, String message, Map<String, Object> headers);

	void send(String address, String message, Map<String, Object> headers, ReplyHandler replyHandler);

	void publish(String address, String message, Map<String, Object> headers);

	void registerHandler(String address, MessageHandler handler);

	void registerHandler(String address, Map<String, Object> headers, MessageHandler handler);

	void unregisterHandler(String address, MessageHandler handler);

	void unregisterHandler(String address, Map<String, Object> headers, MessageHandler handler);

	EventBus open() throws Exception;

	void close();
}
