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
	void createComponentChannel(String channelName, String channelId, boolean isPrivate, boolean isGroup);
	void createChannel(String channelName, String channelId, boolean isPrivate, boolean isGroup);
	String joinChannel(String channelName, UUID playerId, boolean isPrivate, boolean isGroup);
	void leaveChannel(String channelName, UUID playerId);

	String joinMonitoringChannel(UUID playerId);
	void createMonitoringChannel(String channelName, UUID playerId);
	void leaveMonitoringChannel(UUID playerId);
	boolean isMonitoringChannel(String channelId);
	void sendMonitoringMessage(UUID playerId, String... message);
	void sendMonitoringComponent(UUID playerId, BaseComponent... components);

	void subscribe(UUID playerId, String channelName);
	Subscriber getSubscriber(UUID playerId);
	boolean hasSubscriber(UUID playerId);
	void sendMessage(String channelId, String... messages);
	void sendMessage(String channelId, UUID playerId, String... messages);
	void broadcastMessage(String channelId, String... messages);

	void sendComponent(String channelId, BaseComponent... components);
	void sendComponent(String channelId, UUID playerId, BaseComponent... components);
	void broadcastComponent(String channelId, BaseComponent... components);
}
