package org.codenergic.eventbus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.codenergic.eventbus.EventBus;
import org.codenergic.eventbus.EventBusAdapter;
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
