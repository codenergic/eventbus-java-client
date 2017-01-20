package org.codenergic.eventbus;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventBusOptions {
	private int pingInterval = 5000;
	private ObjectMapper objectMapper = new ObjectMapper();

	protected int getPingInterval() {
		return pingInterval;
	}

	public EventBusOptions setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public EventBusOptions setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		return this;
	}
}
