package com.ttProject.liverepeater;

import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;

import com.ttProject.liverepeater.library.BroadcastStream;

public class StreamListener implements IEventDispatcher {
	private BroadcastStream stream;
	public StreamListener(BroadcastStream stream) {
		this.stream = stream;
	}
	public String getName() {
		return stream.getName();
	}

	@Override
	public void dispatchEvent(IEvent event) {
		stream.dispatchEvent(event);
	}
}
