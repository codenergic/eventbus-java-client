[![Build Status](https://travis-ci.org/codenergic/eventbus-java-client.svg?branch=master)](https://travis-ci.org/codenergic/eventbus-java-client)

# EventBus Java Client

This is a port of [vertx3-eventbus-client.js](https://www.npmjs.com/package/vertx3-eventbus-client) 
writen in Java.

## How to
### Create ```EventBus``` instance
```java
EventBus eventBus = EventBusAdapter.connect("ws://localhost/eventbus/websocket");
```
### Register ```onOpen``` and ```onClose``` Events
```java
eventBus.onOpen(new ConnectionHandler() {
	@Override
	public void handle() {
		// handle on connection open
	}
});

eventBus.onClose(new ConnectionHandler() {
	@Override
	public void handle() {
		// handle on connection closed
	}
});
```
### Open Connection
```java
eventBus.open();
```
### Registering Handlers
```java
eventBus.registerHandler("chat.message.123", new MessageHandler() {
	@Override
	public void handle(Message message) {
		System.out.println("I have received a message: " + message.body());
	}
});
```
### Publishing Messages
```java
eventBus.publish("chat.message", "Hello");
```
### Sending Messages

```java
eventBus.send("chat.token", null, null, new ReplyHandler() {
	@Override
	public void handle(Message message) {
		System.out.println("Received reply: " + message.body());
	}
});
```
