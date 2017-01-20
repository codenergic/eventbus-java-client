
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
