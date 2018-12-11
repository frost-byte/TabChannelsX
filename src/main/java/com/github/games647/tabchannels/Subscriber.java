package com.github.games647.tabchannels;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;

import static net.md_5.bungee.api.ChatColor.GREEN;
import static net.md_5.bungee.api.ChatColor.YELLOW;
import static net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;

@SuppressWarnings("WeakerAccess")
public class Subscriber {

	private static final String CHANNEL_SEPARATOR = " || ";

	@SuppressWarnings( { "FieldCanBeLocal", "unused" })
	private final UUID selfUUID;

	/**
	 * The id for the Current Channel the Subscriber is watching.
	 */
	private String currentChannelId;

	/**
	 * Map of Subscribed Channel Ids to the number of Unread messages for each
	 */
	private Map<String, MutableInt> unreadChannels = new ConcurrentHashMap<>();

	private final TabChannels plugin;

	public Subscriber(TabChannels plugin, UUID uuid, Channel global) {
		this.plugin = plugin;
		this.selfUUID = uuid;
		this.currentChannelId = global.getId();
		this.unreadChannels.put(global.getId(), new MutableInt());
	}

	public String getCurrentChannel() {
		return currentChannelId;
	}


	public void subscribe(Channel toSubscribe) {
		unreadChannels.put(toSubscribe.getId(), new MutableInt());
	}

	public void unsubscribe(Channel toSubscribe) {
		unreadChannels.remove(toSubscribe.getId());
	}

	public boolean hasSubscribed(String channelId)
	{
		return unreadChannels.containsKey(channelId);
	}

	public List<String> getSubscriptions()
	{
		return new ArrayList<>(unreadChannels.keySet());
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean switchChannel(Channel channel) {
		String channelId = channel.getId();
		//test if the player is in this channel
		if (unreadChannels.containsKey(channelId)) {
			this.currentChannelId = channelId;
			//mark all messages as read
			unreadChannels.get(channelId).setValue(0);
			return true;
		}

		return false;
	}

	public void notifyNewMessage(Channel fromChannel) {
		String channelId = fromChannel.getId();
		if (!channelId.equals(currentChannelId)) {
			MutableInt missedMessages = unreadChannels.get(channelId);
			if (missedMessages != null) {
				missedMessages.increment();
			}
		}
	}

	@SuppressWarnings("unused")
	public int getUnreadMessages(Channel channel) {

		if (unreadChannels.containsKey(channel.getId()))
		{
			MutableInt result = unreadChannels.get(channel.getId());
			if (result != null)
				return result.intValue();
		}
		return -1;
	}

	public BaseComponent[] getChannelSelection() {
		String newName = currentChannelId;

		if (plugin.isMonitoringChannel(currentChannelId))
			newName = "Monitor";

		String currentLine = StringUtils.capitalize(newName);
		ComponentBuilder builder = new ComponentBuilder(currentLine).bold(true).color(GREEN);

		for (Map.Entry<String, MutableInt> entry : unreadChannels.entrySet()) {

			String channelId = entry.getKey();
			int unreadMessage = entry.getValue().intValue();
			Channel channel = plugin.getChannel(channelId);
			String channelName = channel.getChannelName();

			if (plugin.isMonitoringChannel(channelName.toLowerCase()))
				channelName = "Monitor";

			if (!channelId.equals(currentChannelId)) {
				builder.append(CHANNEL_SEPARATOR).reset();
				builder.append(channelName)
				.color(GREEN)
				.event(
					new ClickEvent(
						RUN_COMMAND,
						"/switch " + channelId
					)
				);

				//show the number of unread messages
				if (unreadMessage > 0) {
					builder.append("(").reset()
							.append(Integer.toString(unreadMessage)).color(YELLOW)
							.append(")").reset();
				}
			}
		}

		return builder.create();
	}
}
