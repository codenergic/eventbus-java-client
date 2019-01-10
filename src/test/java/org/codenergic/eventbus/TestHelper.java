package org.codenergic.eventbus;

import java.util.concurrent.CountDownLatch;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

class TestHelper {
	static Vertx vertx;

	static String startServer() throws InterruptedException {
		final String host = "127.0.0.1";
		final int port = 8080;
		final String connectionAddress = "ws://" + host + ":" + port + "/eventbus/websocket";
		vertx = Vertx.vertx();
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);
		router.route("/eventbus/*").handler(SockJSHandler.create(vertx)
				.bridge(new BridgeOptions()
						.addInboundPermitted(new PermittedOptions().setAddress("test-address"))
						.addOutboundPermitted(new PermittedOptions().setAddress("test-address-reply"))
						.addOutboundPermitted(new PermittedOptions().setAddress("test-address-publish-reply"))));
		CountDownLatch latch = new CountDownLatch(1);
		server.requestHandler(router).listen(port, host, event -> {
			if (event.failed()) {
				throw new IllegalStateException(event.cause());
			}
			latch.countDown();
		});
		latch.await();
		return connectionAddress;
	}

	static void stopServer() {
		if (vertx != null)
			vertx.close();
	}
}
