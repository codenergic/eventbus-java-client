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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, setterVisibility = Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
	private String type;
	private String address;
	private Map<String, Object> headers;
	private String body;
	private String replyAddress;
	private Integer failureCode;
	private String failureType;
	private String failureMessage;

	protected Message() {
	}

	public Message(MessageType type, String address, Map<String, Object> headers, String body, String replyAddress) {
		this.type = type.getEventBusMessageType();
		this.address = address;
		this.headers = Optional.ofNullable(headers).orElse(new HashMap<>());
		this.body = body;
		this.replyAddress = replyAddress;
	}

	public Message(MessageType type, String address, Map<String, Object> headers, String body) {
		this(type, address, headers, body, null);
	}

	public Message(int failureCode, String failureType, String failureMessage) {
		this.failureCode = failureCode;
		this.failureType = failureType;
		this.failureMessage = failureMessage;
	}

	public String getAddress() {
		return address;
	}

	public String getBody() {
		return body;
	}

	public Integer getFailureCode() {
		return failureCode;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public String getFailureType() {
		return failureType;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public String getReplyAddress() {
		return replyAddress;
	}

	public String getType() {
		return type;
	}

	public enum MessageType {
		SEND, PUBLISH, REGISTER, UNREGISTER;

		private String getEventBusMessageType() {
			return name().toLowerCase();
		}
	}
}
