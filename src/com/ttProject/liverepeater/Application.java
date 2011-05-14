package com.ttProject.liverepeater;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.stream.BroadcastScope;
import org.red5.server.stream.IBroadcastScope;
import org.red5.server.stream.IProviderService;

import com.ttProject.liverepeater.library.BroadcastStream;
import com.ttProject.liverepeater.library.RtmpClientEx;

public class Application extends ApplicationAdapter {
	private Map<Object, RtmpClientEx> rtmpClients = new ConcurrentHashMap<Object, RtmpClientEx>();
	private String server = null;
	private Integer port = null;
	private String application = null;
	
	/**
	 * @param server the server to set
	 */
	public void setServer(String server) {
		this.server = server;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}
	/**
	 * @param application the application to set
	 */
	public void setApplication(String application) {
		this.application = application;
	}
	@Override
	public boolean appStart(IScope scope) {
		if(server == null) {
			// server must be set.
			throw new RuntimeException("please put server property for Application beans on WEB-INF/red5-web.xml");
		}
		if(port == null) {
			port = 1935;
		}
		if(application == null) {
			application = getName();
		}
		return super.appStart(scope);
	}
	@Override
	public synchronized void disconnect(IConnection conn, IScope scope) {
		// somebody disconnect from scope.
		if(scope.getClients().size() == 0) {
			// if scope has no users, terminate rtmpClient.
			String rtmpClientKey = scope.getContextPath();
			RtmpClientEx rtmpClient = rtmpClients.get(rtmpClientKey);
			rtmpClient.disconnect();
			rtmpClients.remove(rtmpClientKey);
		}
	}
	@Override
	public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item,
			boolean isLive) {
		watchStart(stream, item);
		super.streamPlayItemPlay(stream, item, isLive);
	}
	@Override
	public void streamPlayItemResume(ISubscriberStream stream, IPlayItem item,
			int position) {
		watchStart(stream, item);
		super.streamPlayItemResume(stream, item, position);
	}
	private void watchStart(ISubscriberStream stream, IPlayItem item) {
		IScope scope = stream.getScope();
		String rtmpClientKey = scope.getContextPath();
		String name = item.getName();
		// do we already have rtmpClient?
		RtmpClientEx rtmpClient = rtmpClients.get(rtmpClientKey);
		if(rtmpClient != null) {
			// do we already have this name on playing stream?
			if(rtmpClient.getStreamId(name) != null) {
				System.out.println("this is already repeating");
				return;
			}
		}
		BroadcastStream outputStream;
		outputStream = new BroadcastStream(name);
		outputStream.setScope(scope);
		
		IProviderService service = (IProviderService)getContext().getBean(IProviderService.BEAN_NAME);
		if(service.registerBroadcastStream(scope, name, outputStream)) {
			IBroadcastScope bsScope = (BroadcastScope) service.getLiveProviderInput(scope, name, true);
			bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, outputStream);
		}
		else {
			throw new RuntimeException("Failed to make mirroring stream");
		}
		if(rtmpClient != null) {
			// if we already have rtmpclient just play.
			// XXX I stacked with Nullpointer Exception only once.
			/*
[ERROR] [pool-4-thread-2] org.red5.server.stream.PlaylistSubscriberStream - error notify streamPlayItemPlay
java.lang.NullPointerException: null
	at org.red5.server.net.rtmp.RTMPConnection.registerPendingCall(RTMPConnection.java:798) ~[red5.jar:na]
	at org.red5.server.net.rtmp.RTMPConnection.invoke(RTMPConnection.java:808) ~[red5.jar:na]
	at org.red5.server.net.rtmp.RTMPConnection.invoke(RTMPConnection.java:779) ~[red5.jar:na]
	at org.red5.server.net.rtmp.RTMPConnection.invoke(RTMPConnection.java:834) ~[red5.jar:na]
	at org.red5.server.net.rtmp.BaseRTMPClientHandler.invoke(BaseRTMPClientHandler.java:393) ~[red5.jar:na]
	at org.red5.server.net.rtmp.BaseRTMPClientHandler.createStream(BaseRTMPClientHandler.java:418) ~[red5.jar:na]
	at com.ttProject.liverepeater.library.RtmpClientEx.createStream(RtmpClientEx.java:171) ~[liverepeater.jar:na]
	at com.ttProject.liverepeater.library.RtmpClientEx.play(RtmpClientEx.java:183) ~[liverepeater.jar:na]
	at com.ttProject.liverepeater.Application.watchStart(Application.java:108) ~[liverepeater.jar:na]
	at com.ttProject.liverepeater.Application.streamPlayItemPlay(Application.java:70) ~[liverepeater.jar:na]
	at org.red5.server.stream.PlaylistSubscriberStream$4.run(PlaylistSubscriberStream.java:683) ~[red5.jar:na]
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:441) [na:1.6.0_24]
	at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:303) [na:1.6.0_24]
	at java.util.concurrent.FutureTask.run(FutureTask.java:138) [na:1.6.0_24]
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$301(ScheduledThreadPoolExecutor.java:98) [na:1.6.0_24]
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:206) [na:1.6.0_24]
	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886) [na:1.6.0_24]
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908) [na:1.6.0_24]
	at java.lang.Thread.run(Thread.java:680) [na:1.6.0_24]
			 */
			rtmpClient.play(name, new StreamListener(outputStream));
			System.out.println("add new play stream on rtmpClient");
		}
		else {
			// need to make rtmpClient
			rtmpClient = new RtmpClientEx();
			rtmpClient.setServer(server);
			rtmpClient.setPort(port);
			rtmpClient.setApplication(application);
			rtmpClient.setListener(new RepeatListener(rtmpClient, new StreamListener(outputStream)));
			rtmpClient.connect();
			
			rtmpClients.put(rtmpClientKey, rtmpClient);
			System.out.println("make new rtmpClient for 1st stream.");
		}
	}
}
