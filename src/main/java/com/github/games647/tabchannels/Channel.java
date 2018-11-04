package com.github.games647.tabchannels;


import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import java.util.*;

import static net.md_5.bungee.api.ChatColor.GREEN;
import static org.bukkit.util.ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;

@SuppressWarnings( { "WeakerAccess", "unchecked" })
public abstract class Channel<T>
{

	//-1 Header line -2 Chat channel selection
	protected static final int QUEUE_SIZE = ChatPaginator.OPEN_CHAT_PAGE_HEIGHT - 1 - 2;


	protected final String id;
	protected final String channelName;
	protected final boolean privateChannel;
	protected final boolean groupChannel;

	/**
	 * Map of Recipient UUIDs to a list of their chatHistory
	 */
	protected final Map<UUID, List<T>> chatMap = Maps.newHashMap();

	@SuppressWarnings("WeakerAccess")
	public Channel(
		String id,
		String channelName,
		boolean privateChannel,
		boolean groupChannel
	){
		this.id = id;
		this.channelName = StringUtils.capitalize(channelName);
		this.privateChannel = privateChannel;
		this.groupChannel = groupChannel;
	}

	public Channel(String channelName, boolean privateChannel) {
		this(
			channelName,
			channelName,
			privateChannel,
			false
		);
	}

	public String getId() {
		return id;
	}

	public boolean isGroup() { return groupChannel; }
	public boolean isPrivate() { return privateChannel; }


	@SuppressWarnings("unused")
	public String getChannelName() { return channelName; }

	public String getName(UUID self) {

		// If a uuid is specified, find a matching recipient (for private messaging)
		// For each private conversation, a tab would appear using the recipient's channelName
		if (self != null && privateChannel) {
			for (UUID recipient : chatMap.keySet()) {
				if (!self.equals(recipient)) {
					Player chatPartner = Bukkit.getPlayer(recipient);
					return chatPartner.getName();
				}
			}
		}

		// When a player UUID isn't specified, just return the channel channelName.
		return channelName;
	}

	public BaseComponent[] getHeader(String headerName) {

		String title = ' ' + headerName + ' ';
		String center = StringUtils.center(
			title,
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2,
			'='
		);

		return new ComponentBuilder(center).color(GREEN).create();
	}


	public abstract int getHistoryLength(UUID playerId);
	public boolean hasRecipients() { return chatMap != null && !chatMap.isEmpty(); }
	public boolean hasRecipient(UUID playerId)
	{
		return chatMap.containsKey(playerId);
	}

	public void addRecipient(UUID playerId) {
		if (!chatMap.containsKey(playerId))
			chatMap.put(playerId, Lists.newArrayListWithExpectedSize(QUEUE_SIZE));
	}

	public void removeRecipient(UUID playerId) {
		if (chatMap.containsKey(playerId))
			chatMap.remove(playerId);
	}

	public List<UUID> getRecipients() {
		return new ArrayList<>(chatMap.keySet());
	}

	@SuppressWarnings("unused")
	public List<T> getChatHistory(UUID playerId)
	{
		playerId = Preconditions.checkNotNull(playerId, "The player UUID cannot be null.");

		return chatMap.getOrDefault(playerId, null);
	}

	public abstract BaseComponent[] getContent(UUID playerId);

	@SuppressWarnings({"unused"})
	public abstract void addMessages(Set<UUID> recipientIds, T... messages);
	public abstract void addMessage(UUID recipientId, T... messages);
	public abstract void broadcastMessage(T... messages);
}
