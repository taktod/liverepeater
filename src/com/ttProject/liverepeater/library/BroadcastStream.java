/*******************************************************************************
 * Copyright (c) 2008, 2010 Xuggle Inc.  All rights reserved.
 *  
 * This file is part of Xuggle-Xuggler-Red5.
 *
 * Xuggle-Xuggler-Red5 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xuggle-Xuggler-Red5 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Xuggle-Xuggler-Red5.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * I Change some of this library to use stream reflection. (taktod)
 *******************************************************************************/
package com.ttProject.liverepeater.library;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCodecInfo;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.IVideoStreamCodec;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.stream.BroadcastScope;
import org.red5.server.stream.IBroadcastScope;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.VideoCodecFactory;
import org.red5.server.stream.codec.StreamCodecInfo;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IBroadcastStream that allows connection-less
 * providers to still publish a Red5 stream.
 * 
 * Don't worry if you don't understand what that means.  See
 * {@link AudioTranscoderDemo} for an example of this in action.
 * 
 */
public class BroadcastStream implements IBroadcastStream, IProvider, IPipeConnectionListener
{
	/** Listeners to get notified about received packets. */
	private Set<IStreamListener> mListeners = new CopyOnWriteArraySet<IStreamListener>();
	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private String mPublishedName;
	private IPipe mLivePipe;
	private IScope mScope;

	// Codec handling stuff for frame dropping
	private StreamCodecInfo mCodecInfo;
	private Long mCreationTime;

	public BroadcastStream(String name)
	{
		mPublishedName = name;
		mLivePipe = null;
		log.trace("name: {}", name);

		// we want to create a video codec when we get our
		// first video packet.
		mCodecInfo = new StreamCodecInfo();
		mCreationTime = null;
	}

	@Override
	public IProvider getProvider()
	{
		log.trace("getProvider()");
		return this;
	}

	@Override
	public String getPublishedName()
	{
		log.trace("getPublishedName()");
		return mPublishedName;
	}

	@Override
	public String getSaveFilename()
	{
		log.trace("getSaveFilename()");
		throw new Error("unimplemented method");
	}

	@Override
	public void addStreamListener(IStreamListener listener)
	{
		log.trace("addStreamListener(listener: {})", listener);
		mListeners.add(listener);
	}

	@Override
	public Collection<IStreamListener> getStreamListeners()
	{
		log.trace("getStreamListeners()");
		return mListeners;
	}

	@Override
	public void removeStreamListener(IStreamListener listener)
	{
		log.trace("removeStreamListener({})", listener);
		mListeners.remove(listener);
	}

	@Override
	public void saveAs(String filePath, boolean isAppend)
		throws IOException, ResourceNotFoundException, ResourceExistException
	{
		log.trace("saveAs(filepath:{}, isAppend:{})", filePath, isAppend);
		throw new Error("unimplemented method");
	}

	@Override
	public void setPublishedName(String name)
	{
		log.trace("setPublishedName(name:{})", name);
		mPublishedName = name;
	}

	@Override
	public void close()
	{
		log.trace("close");
	}
	public void terminateGhostConnection() {
		if(mLivePipe.getConsumers().size() != 0) {
			// まだつながっているユーザーが存在するため、このままおいておく。
			System.out.println(mLivePipe.getConsumers().size());
			return;
		}
		// 誰も接続していないので、データを削除する。
		IProviderService providerService = (IProviderService)mScope.getContext().getBean(IProviderService.BEAN_NAME);
		IBroadcastScope bsScope = (BroadcastScope) providerService.getLiveProviderInput(mScope, mPublishedName, true);
		bsScope.removeAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
		providerService.unregisterBroadcastStream(mScope, mPublishedName);
		System.out.println(providerService);

		log.trace("close()");
	}

	@Override
	public IStreamCodecInfo getCodecInfo()
	{
		log.trace("getCodecInfo()");
		// we don't support this right now.
		return mCodecInfo;
	}

	@Override
	public String getName()
	{
		log.trace("getName(): {}", mPublishedName);
		// for now, just return the published name
		return mPublishedName;
	}

	public void setScope(IScope scope)
	{
		mScope = scope;
	}

	@Override
	public IScope getScope()
	{
		log.trace("getScope(): {}", mScope);
		return mScope;
	}

	@Override
	public void start()
	{
		Status status = new Status(Status.NS_PLAY_PUBLISHNOTIFY);
		StatusMessage smessage = new StatusMessage();
		smessage.setBody(status);
		try {
			mLivePipe.pushMessage(smessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		this.mLivePipe.subscribe(getProvider(), new HashMap<String, Object>() {{put("play", "start");}});
		log.trace("start()");
	}

	@Override
	public void stop()
	{
		// unscribeを送る。(とめることはできるが、始めることができない。)
//		this.mLivePipe.unsubscribe(this.getProvider());
      
		// notifyを送る。
		Status status = new Status(Status.NS_PLAY_UNPUBLISHNOTIFY);
		StatusMessage smessage = new StatusMessage();
		smessage.setBody(status);
		try {
			mLivePipe.pushMessage(smessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// BroadcastCloseイベントをおくっておく。
		// XXX mLivePipeから何人視聴している状態か確認して、視聴しているユーザーがいる間は、closeしても内部の動作をとめないようにしておく。
		// これでうまくいけそう。
//		System.out.println(mLivePipe.getConsumers().size()); (接続人数を取得できる。)
		log.trace("stop");
	}

	@Override
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg)
	{
		log.trace("onOOBControlMessage");
	}

	@Override
	public void onPipeConnectionEvent(PipeConnectionEvent event)
	{
		log.trace("onPipeConnectionEvent(event:{})", event);
		switch (event.getType())
		{
		case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
			if (event.getProvider() == this
				&& (event.getParamMap() == null || !event.getParamMap().containsKey("record")))
			{
				this.mLivePipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (this.mLivePipe == event.getSource())
			{
				// これでunpublishedがいくようになった？
				this.mLivePipe.unsubscribe(this.getProvider());
			}
			break;
		case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
			System.out.println("connect push");
			break;
		case PipeConnectionEvent.CONSUMER_DISCONNECT:
			System.out.println("disconnect");
			break;
		default:
			break;
		}
	}

	public void dispatchEvent(IEvent event)
	{
		try {
			log.trace("dispatchEvent(event:{})", event);
			if (event instanceof IRTMPEvent)
			{
				IRTMPEvent rtmpEvent = (IRTMPEvent) event;
				if (mLivePipe != null)
				{
					RTMPMessage msg = RTMPMessage.build(rtmpEvent);
					//RTMPMessage msg = new RTMPMessage();
					//msg.setBody(rtmpEvent);

					if (mCreationTime == null)
						mCreationTime = (long)rtmpEvent.getTimestamp();
					try
					{
						if (event instanceof AudioData)
						{
							mCodecInfo.setHasAudio(true);
							// 本来ならここでオーディオデータも取得する必要があるが、Red5がAudioデータにきちんと対処していないので、データが抜け落ちている。
						}
						else if (event instanceof VideoData)
						{
							IVideoStreamCodec videoStreamCodec = null;
							if (mCodecInfo.getVideoCodec() == null)
							{
								videoStreamCodec = VideoCodecFactory.getVideoCodec(((VideoData) event).getData());
								mCodecInfo.setVideoCodec(videoStreamCodec);
							} else if (mCodecInfo != null) {
								videoStreamCodec = mCodecInfo.getVideoCodec();
							}

							if (videoStreamCodec != null) {
								videoStreamCodec.addData(((VideoData) rtmpEvent).getData());
							}

							if (mCodecInfo!= null) {
								mCodecInfo.setHasVideo(true);
							}
						}
						mLivePipe.pushMessage(msg);

						// Notify listeners about received packet
						if (rtmpEvent instanceof IStreamPacket)
						{
							for (IStreamListener listener : getStreamListeners())
							{
								try
								{
									listener.packetReceived(this, (IStreamPacket) rtmpEvent);
								}
								catch (Exception e)
								{
									log.error("Error while notifying listener " + listener, e);
								}
							}
						}
					}
					catch (IOException ex)
					{
						// ignore
						log.error("Got exception: {}", ex);
					}
				}
			}
		} finally {
		}
	}

	@Override
	public long getCreationTime()
	{
		return mCreationTime != null ? mCreationTime : 0L;
	}

	@Override
	public Notify getMetaData()
	{
		return null;
	}
}