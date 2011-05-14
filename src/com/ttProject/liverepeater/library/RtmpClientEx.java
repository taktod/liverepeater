package com.ttProject.liverepeater.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.DeferredResult;
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to connect other rtmp server.
 */
public class RtmpClientEx extends RTMPClient{
	private static final Logger log = LoggerFactory.getLogger(RtmpClientEx.class);
	private String server;
	private int port;
	private String application;
	private RTMPConnection conn;
	private IRtmpClientEx listener;

	private Object tmplistener;
	private String name;
	private MODE mode = null;
	private enum MODE{
		Play, Publish;
	}
	private Map<String, Integer> streamIds = new ConcurrentHashMap<String, Integer>();
	// Map to hold IEventDispatcher(play)orINetStreamEventHandler(publish)
	private Map<String, Object> listeners = new ConcurrentHashMap<String, Object>();
	
	public RtmpClientEx() {
	}
	/**
	 * @param server
	 * @param port
	 * @param application
	 * @param name
	 * @param listener
	 */
	public RtmpClientEx(String server, int port, String application, 
			IRtmpClientEx listener) {
		super();
		this.server = server;
		this.port = port;
		this.application = application;
		this.listener = listener;
	}
	/**
	 * @return the host
	 */
	public String getServer() {
		return server;
	}
	/**
	 * @param server the host to set
	 */
	public void setServer(String server) {
		this.server = server;
	}
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return the app
	 */
	public String getApplication() {
		return application;
	}
	/**
	 * @param application the app to set
	 * app/room
	 */
	public void setApplication(String application) {
		this.application = application;
	}
	/**
	 * @return the listener
	 */
	public IRtmpClientEx getListener() {
		return listener;
	}
	/**
	 * @param listener the listener to set
	 */
	public void setListener(IRtmpClientEx listener) {
		this.listener = listener;
	}
	/**
	 * @param name the name on streamId set
	 * @return streamId
	 */
	public Integer getStreamId(String name) {
		return streamIds.get(name);
	}

	public void connect() {
		this.connect(server, port, application);
	}
	public void connect(Object[] params) {
		this.connect(server, port, makeDefaultConnectionParams(server, port, application), null, params);
	}
	@Override
	public void connect(String server, int port,
			Map<String, Object> connectionParams) {
		this.connect(server, port, connectionParams, null);
	}
	@Override
	public void connect(String server, int port,
			Map<String, Object> connectionParams,
			IPendingServiceCallback connectCallback) {
		this.connect(server, port, connectionParams, connectCallback, null);
	}
	@Override
	public void connect(String server, int port,
			Map<String, Object> connectionParams,
			IPendingServiceCallback connectCallback,
			Object[] connectCallArguments) {
		super.connect(server, port, connectionParams, new ConnectCallback(connectCallback),
				connectCallArguments);
	}
	@Override
	public void connect(String server, int port, String application) {
		this.connect(server, port, application, null);
	}
	@Override
	public void connect(String server, int port, String application,
			IPendingServiceCallback connectCallback) {
		this.connect(server, port, makeDefaultConnectionParams(server, port, application), connectCallback, null);
	}
	@Override
	public void disconnect() {
		super.disconnect();
	}
	@Override
	public void connectionOpened(RTMPConnection conn, RTMP state) {
		super.connectionOpened(conn, state);
		this.conn = conn;
	}
	@Override
	public void connectionClosed(RTMPConnection conn, RTMP state) {
		if(listener != null) {
			listener.onDisconnect();
		}
		super.connectionClosed(conn, state);
	}
	@Override
	protected void onInvoke(RTMPConnection conn, Channel channel, Header source,
			Notify invoke, RTMP rtmp) {
		super.onInvoke(conn, channel, source, invoke, rtmp);
/*		if (invoke.getType() == IEvent.Type.STREAM_DATA) {
			log.debug("Ignoring stream data notify with header: {}", source);
			return;
		}
		log.debug("onInvoke: {}, invokeId: {}", invoke, invoke.getInvokeId());
		final IServiceCall call = invoke.getCall();
		String methodName = call.getServiceMethodName();
		if ("_result".equals(methodName) || "_error".equals(methodName)) {
			super.onInvoke(conn, channel, source, invoke, rtmp);
			return;
		}

		// potentially used twice so get the value once
		boolean onStatus = call.getServiceMethodName().equals("onStatus");
		log.debug("onStatus {}", onStatus);
		if (onStatus) {
			super.onInvoke(conn, channel, source, invoke, rtmp);
			return;
		}

		if (call instanceof IPendingServiceCall) {
			IPendingServiceCall psc = (IPendingServiceCall) call;
			Object result = psc.getResult();
			log.debug("Pending call result is: {}", result);
			if (result instanceof DeferredResult) {
				super.onInvoke(conn, channel, source, invoke, rtmp);
				return;
			} else if (!onStatus) {
				Invoke reply = new Invoke();
//				psc.setResult("_error");
				psc.setStatus(Call.STATUS_METHOD_NOT_FOUND);
				reply.setCall(psc);
				reply.setInvokeId(invoke.getInvokeId());
				
				log.debug("Sending empty call reply: {}", reply);
				channel.write(reply);
			}
		}// */
	}
	@Override
	public void createStream(IPendingServiceCallback callback) {
		super.createStream(new CreateStreamCallback(callback));
	}
	/**
	 * start play with default information.
	 */
	public boolean play(String name, IEventDispatcher listener) {
		if(listener == null) {
			return false;
		}
		this.name = name;
		this.tmplistener = listener;
		this.mode = MODE.Play;
		createStream(null);
		setStreamEventDispatcher(listener);
		return true;
	}
	/**
	 * start publish with default information.
	 */
	public boolean publish(String name, INetStreamEventHandler listener) {
		this.name = name;
		this.tmplistener = listener;
		this.mode = MODE.Publish;
		createStream(null);
		return true;
	}
	private void startMediaStream(Integer streamId) {
		switch(mode) {
		case Play:
			play(streamId, name, -2000, -2);
			break;
		case Publish:
			publish(streamId, name, "live", (INetStreamEventHandler)tmplistener);
			break;
		default:
			return;
		}
		streamIds.put(name, streamId);
		listeners.put(streamId.toString(), tmplistener);
		name = null;
		tmplistener = null;
		mode = null;
	}
	/**
	 * callback wrapper for createStream
	 */
	private class CreateStreamCallback implements IPendingServiceCallback {
		private IPendingServiceCallback wrapped;
		public CreateStreamCallback(IPendingServiceCallback wrapped) {
			this.wrapped = wrapped;
		}
		@Override
		public void resultReceived(IPendingServiceCall call) {
			Integer streamIdInt = (Integer)call.getResult();
			if(conn != null && streamIdInt != null) {
				startMediaStream(streamIdInt);
				listener.onCreateStream(streamIdInt);
			}
			if(wrapped != null) {
				wrapped.resultReceived(call);
			}
		}
	}
	/**
	 * callback wrapper for connect.
	 */
	private class ConnectCallback implements IPendingServiceCallback {
		private IPendingServiceCallback wrapped;
		public ConnectCallback(IPendingServiceCallback wrapped) {
			this.wrapped = wrapped;
		}
		@Override
		public void resultReceived(IPendingServiceCall call) {
			// Check the connect message.
			if("connect".equals(call.getServiceMethodName())) {
				if(listener != null) {
					listener.onConnect();
				}
			}
			if(wrapped != null) {
				wrapped.resultReceived(call);
			}
		}
	}
}
