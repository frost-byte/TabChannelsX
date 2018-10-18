package com.github.games647.tabchannels;

import net.md_5.bungee.api.chat.BaseComponent;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public interface TabChannelsManager
{
	List<String> getChannelNames();
	List<String> getChannelIds();
	boolean hasChannel(String channelName);
	Channel getChannel(String id);
	void createChannel(String channelName, String channelId, boolean isPrivate, boolean isGroup);
	String joinChannel(String channelName, UUID playerId, boolean isPrivate, boolean isGroup);
	void leaveChannel(String channelName, UUID playerId);

	String joinMonitoringChannel(UUID playerId);
	void createMonitoringChannel(String channelName, UUID playerId);
	void leaveMonitoringChannel(UUID playerId);
	boolean isMonitoringChannel(String channelId);
	void sendMonitoringMessage(String message, UUID playerId);
	void sendMonitoringComponent(UUID playerId, BaseComponent... components);

	void subscribe(UUID playerId, String channelName);
	Subscriber getSubscriber(UUID playerId);
	boolean hasSubscriber(UUID playerId);
	void sendMessage(String message, String channelName);
	void sendMessage(String message, String channelId, UUID playerId);
	void broadcastMessage(String message, String channelName);

	void sendComponent(String channelName, BaseComponent... components);
	void sendComponent(String channelId, UUID playerId, BaseComponent... components);
	void broadcastComponent(String channelName, BaseComponent... components);
}
