package com.ttProject.liverepeater;

import org.red5.server.api.service.IServiceCall;

import com.ttProject.liverepeater.library.IRtmpClientEx;
import com.ttProject.liverepeater.library.RtmpClientEx;

public class RepeatListener implements IRtmpClientEx {
	private RtmpClientEx rtmpClient;
	private StreamListener listener;
	public RepeatListener(RtmpClientEx rtmpClient, StreamListener listener) {
		this.rtmpClient = rtmpClient;
		this.listener = listener;
	}
	@Override
	public void onConnect() {
		rtmpClient.play(listener.getName(), listener);
	}
	@Override
	public void onCreateStream(Integer streamId) {
	}
	@Override
	public void onDisconnect() {
	}
	@Override
	public Object onInvoke(IServiceCall call) {
		return "";
	}
}
