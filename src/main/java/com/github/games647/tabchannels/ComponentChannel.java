package com.github.games647.tabchannels;


import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.util.ChatPaginator;

import java.util.*;

import static net.md_5.bungee.api.ChatColor.GOLD;
import static org.bukkit.util.ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;

@SuppressWarnings("unused")
public class ComponentChannel extends Channel<BaseComponent[]>
{
	@SuppressWarnings("WeakerAccess")
	public ComponentChannel(
		String id,
		String channelName,
		boolean privateChannel,
		boolean groupChannel
	) {
		super(id, channelName, privateChannel, groupChannel);
	}

	public ComponentChannel(String channelName, boolean privateChannel) {
		super(
			channelName,
			channelName,
			privateChannel,
			false
		);
	}

	// Get the number of actual lines of text from all the components
	// in the current history
	private int chatHistoryLength(UUID playerId)
	{
		List<BaseComponent[]> history = getChatHistory(playerId);

		if (history == null || history.isEmpty())
			return 0;

		return history.stream()
			.mapToInt(this::numComponentLines)
			.sum();
	}

	private int numComponentLines(BaseComponent[] components)
	{
		String combined = BaseComponent.toLegacyText(components);
		return ChatPaginator.wordWrap(
			combined,
			GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 2 - 1
		).length;
	}

	private void removeOverflow(UUID playerId, int messageLength)
	{
		List<BaseComponent[]> history = getChatHistory(playerId);

		// Calculate the number of lines contained in the history's
		// components plus the number of lines contained in the new Message
		// and subtract the queue size (or total number of lines that can be
		// displayed)
		int historyLength = chatHistoryLength(playerId);
		int entriesToRemove = 0;
		int oversize = historyLength + messageLength - QUEUE_SIZE;
		int numLines;

		// Remove older components to make room for the new one.
		if (!history.isEmpty())
		{
			for (BaseComponent[] components : history)
			{
				numLines = numComponentLines(components);
				entriesToRemove += 1;
				oversize -= numLines;

				if (oversize <= 0)
					break;
			}

			for (int i = 1; i <= entriesToRemove; i++)
				//remove the oldest element
				history.remove(0);
		}
	}
	@Override
	public void addMessages(BaseComponent[] message, Set<UUID> recipientIds)
	{
		if (message == null)
			return;

		int messageLength = numComponentLines(message);
		for (UUID recipientId : recipientIds)
		{
			List<BaseComponent[]> history = getChatHistory(recipientId);
			removeOverflow(recipientId, messageLength);

			history.add(message);

			chatMap.put(
				recipientId,
				history
			);
		}
	}

	@Override
	public void addMessage(BaseComponent[] message, UUID recipientId) {
		if (message == null)
			return;

		List<BaseComponent[]> history = getChatHistory(recipientId);
		removeOverflow(recipientId, numComponentLines(message));

		history.add(message);

		// This was meant to split the input message that was in text
		// into multiple lines. The 0 slot in the array was added
		// before the loop, then a space was added between
		// each of the other lines.
//		for (int i = 1; i < linesToAdd.length; i++) {
//			String messagePart = ' ' + linesToAdd[i];
//			history.add(messagePart);
//		}

		chatMap.put(recipientId, history);
	}

	@Override
	public void broadcastMessage(BaseComponent[] message)
	{
		if (message == null)
			return;

		addMessages(message, chatMap.keySet());
	}

	@Override
	public BaseComponent[] getContent(UUID playerId) {
		StringBuilder emptyLineBuilder = new StringBuilder();

		List<BaseComponent[]> history = chatMap.getOrDefault(playerId, null);

		if (history == null)
			return null;

		// Fill in the empty space in the output
		// above the current messages in the history
		for (int i = QUEUE_SIZE - history.size(); i > 0; i--) {
			emptyLineBuilder.append("\n");
		}

		// Convert the empty lines into a component.
		ComponentBuilder builder = new ComponentBuilder(emptyLineBuilder.toString());

		// Add the chat history to the output
		for (BaseComponent[] previousMessage : history) {
			builder.append(previousMessage).append("\n");
		}

		// Add the separator for the line before footer.
		builder.append(StringUtils.repeat("=", 26)).color(GOLD);
		builder.create();
		return builder.create();
	}
}
