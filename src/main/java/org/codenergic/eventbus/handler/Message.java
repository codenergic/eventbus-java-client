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
