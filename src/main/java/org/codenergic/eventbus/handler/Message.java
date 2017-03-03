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
package org.codenergic.eventbus.handler;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, setterVisibility = Visibility.NONE)
public class Message {
	public static final String TYPE_SEND = "send";
	public static final String TYPE_PUBLISH = "publish";
	public static final String TYPE_REGISTER = "register";
	public static final String TYPE_UNREGISTER = "unregister";

	private String type;
	private String address;
	private Map<String, Object> headers;
	private String body;
	private String replyAddress;
	private int failureCode;
	private String failureType;
	private String message;

	protected Message() {
	}

	public Message(String type, String address, Map<String, Object> headers, String body, String replyAddress) {
		this.type = type;
		this.address = address;
		this.headers = headers;
		this.body = body;
		this.replyAddress = replyAddress;
	}

	public Message(String type, String address, Map<String, Object> headers, String body) {
		this(type, address, headers, body, null);
	}

	public Message(int failureCode, String failureType, String message) {
		this.failureCode = failureCode;
		this.failureType = failureType;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public String getAddress() {
		return address;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public String getBody() {
		return body;
	}

	public String getReplyAddress() {
		return replyAddress;
	}

	public int getFailureCode() {
		return failureCode;
	}

	public String getFailureType() {
		return failureType;
	}

	public String getMessage() {
		return message;
	}
}
