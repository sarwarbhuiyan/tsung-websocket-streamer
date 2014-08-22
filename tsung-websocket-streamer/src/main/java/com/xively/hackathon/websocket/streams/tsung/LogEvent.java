package com.xively.hackathon.websocket.streams.tsung;

public class LogEvent {
	private String payload;

	public LogEvent(String payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "LogEvent [payload=" + payload + "]";
	}

	public String getPayload() {
		return this.payload;
	}
}
