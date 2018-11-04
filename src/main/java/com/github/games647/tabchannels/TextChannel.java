package com.github.games647.tabchannels;

import java.util.*;
import java.util.stream.Collectors;

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

	/**
	 * Calculate the total number of characters stored in the Channel's Message Queue
	 * for the player with the given UUID.
	 *
	 * @param playerId The player whose chat history length is going to be determined.
	 *
	 * @return The chat history number of lines output
	 */
	@Override
	public int getHistoryLength(UUID playerId)
	{
		List<String> history = getChatHistory(playerId);
		if (history == null || history.isEmpty())
			return 0;

		String combined = history.stream()
			.collect(Collectors.joining());

		return ChatPaginator.wordWrap(
			combined,
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		).length;
	}

	@Override
	public void addMessages(Set<UUID> recipientIds, String... messages)
	{
		if (messages == null || messages.length == 0)
			return;

		//-1 because of the added space after a line break
		// Convert the messages being sent into a paginated
		// list of messages, where each line has be constrained based
		// upon the chat page width
		String[] linesToAdd = ChatPaginator.wordWrap(
			String.join("", messages),
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		);

		recipientIds.forEach(id -> {
			List<String> history = chatMap.getOrDefault(id, new ArrayList<>());
			// Determine the number of lines that will overflow the chat window,
			// so the new messages can be inserted in the recipient's updated chat window
			int oversize = getHistoryLength(id) + linesToAdd.length - QUEUE_SIZE;

			int i = 1;

			while (!history.isEmpty() && i <= oversize)
			{
				history.remove(0);
				i++;
			}

			history.add(linesToAdd[0]);

			for (i = 1; i < linesToAdd.length; i++) {
				String messagePart = ' ' + linesToAdd[i];
				history.add(messagePart);
			}

			chatMap.put(id, history);
		});
	}

	@Override
	public void addMessage(UUID recipientId, String... messages) {
		if (messages == null || messages.length == 0)
			return;

		//-1 because of the added space after a line break
		String[] linesToAdd = ChatPaginator.wordWrap(
			Arrays.toString(messages),
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		);
		// Get the lines of text from the recipient's current chat history in the channel
		List<String> history = chatMap.getOrDefault(recipientId, new ArrayList<>());
		int oversize = getHistoryLength(recipientId) + linesToAdd.length - QUEUE_SIZE;

		for (int i = 1; i <= oversize && i < history.size(); i++) {
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
	public void broadcastMessage(String... messages)
	{
		if (messages == null || messages.length == 0)
			return;

		for (UUID uuid : chatMap.keySet())
			addMessage(uuid, messages);
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
