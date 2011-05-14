package com.ttProject.liverepeater.library;

import org.red5.server.api.service.IServiceCall;

public interface IRtmpClientEx {
	public void onConnect();
	public void onDisconnect();
	public void onCreateStream(Integer streamId);
	public void onInvoke(IServiceCall call);
}
