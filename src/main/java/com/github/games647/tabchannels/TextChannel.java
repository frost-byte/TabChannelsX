package com.github.games647.tabchannels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import org.apache.commons.lang.StringUtils;
import org.bukkit.util.ChatPaginator;

import static net.md_5.bungee.api.ChatColor.GOLD;
import static org.bukkit.util.ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;

public class TextChannel extends Channel<String>
{

	@SuppressWarnings("WeakerAccess")
	public TextChannel(
		String id,
		String channelName,
		boolean privateChannel,
		boolean groupChannel
	) {
		super(id, channelName, privateChannel, groupChannel);
	}

	@SuppressWarnings( { "unused", "WeakerAccess" })
	public TextChannel(String channelName, boolean privateChannel) {
		super(
			channelName,
			channelName,
			privateChannel,
			false
		);
	}

	@Override
	public void addMessages(String message, Set<UUID> recipientIds)
	{
		if (message == null || message.isEmpty())
			return;

		//-1 because of the added space after a line break
		String[] linesToAdd = ChatPaginator.wordWrap(
			message,
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		);

		recipientIds.forEach(id -> {
			List<String> history = chatMap.getOrDefault(id, new ArrayList<>());
			int oversize = history.size() + linesToAdd.length - QUEUE_SIZE;

			if (!history.isEmpty())
			{
				for (int i = 1; i <= oversize; i++) {
					//remove the oldest element
					history.remove(0);
				}
			}

			history.add(linesToAdd[0]);

			for (int i = 1; i < linesToAdd.length; i++) {
				String messagePart = ' ' + linesToAdd[i];
				history.add(messagePart);
			}
			chatMap.put(id, history);
		});
	}

	@Override
	public void addMessage(String message, UUID recipientId) {
		if (message == null || message.isEmpty())
			return;

		//-1 because of the added space after a line break
		String[] linesToAdd = ChatPaginator.wordWrap(
			message,
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		);
		List<String> history = chatMap.getOrDefault(recipientId, new ArrayList<>());
		int oversize = history.size() + linesToAdd.length - QUEUE_SIZE;

		for (int i = 1; i <= oversize; i++) {
			//remove the oldest element
			history.remove(0);
		}

		history.add(linesToAdd[0]);

		for (int i = 1; i < linesToAdd.length; i++) {
			String messagePart = ' ' + linesToAdd[i];
			history.add(messagePart);
		}
		chatMap.put(recipientId, history);
	}

	@Override
	public void broadcastMessage(String message)
	{
		if (message == null || message.isEmpty())
			return;

		for (UUID uuid : chatMap.keySet())
			addMessage(message, uuid);
	}

	@Override
	public BaseComponent[] getContent(UUID playerId) {
		StringBuilder emptyLineBuilder = new StringBuilder();

		List<String> history = chatMap.getOrDefault(playerId, null);

		if (history == null)
			return null;

		for (int i = QUEUE_SIZE - history.size(); i > 0; i--) {
			emptyLineBuilder.append("\n");
		}

		ComponentBuilder builder = new ComponentBuilder(emptyLineBuilder.toString());

		//chat history
		for (String previousMessage : history) {
			builder.append(previousMessage).append("\n");
		}

		builder.append(StringUtils.repeat("=", GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2)).color(GOLD);
		builder.create();
		return builder.create();
	}
}
