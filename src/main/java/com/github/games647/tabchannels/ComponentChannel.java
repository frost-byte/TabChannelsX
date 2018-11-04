package com.github.games647.tabchannels;


import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import org.bukkit.util.ChatPaginator;

import java.util.*;
import java.util.stream.Collectors;

import static net.md_5.bungee.api.ChatColor.GOLD;
import static net.md_5.bungee.api.chat.BaseComponent.toPlainText;
import static net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention.*;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.bukkit.util.ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH;
import static org.bukkit.util.ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;

@SuppressWarnings("unused")
public class ComponentChannel extends Channel<BaseComponent>
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

	@SuppressWarnings("WeakerAccess")
	public ComponentChannel(String channelName, boolean privateChannel) {
		super(
			channelName,
			channelName,
			privateChannel,
			false
		);
	}

	@Override public int getHistoryLength(UUID playerId)
	{
		return chatHistoryLength(playerId);
	}

	// Get the number of actual lines of text from all the components
	// in the current history
	private int chatHistoryLength(UUID playerId)
	{
		if (playerId == null)
			return 0;

		List<BaseComponent> history = getChatHistory(playerId);

		if (history == null || history.isEmpty())
			return 0;

		return numComponentLines(history);
	}

	@SuppressWarnings("Convert2MethodRef")
	private int numComponentLines(List<BaseComponent> components)
	{
		String combined = components.stream()
			.map(c -> toPlainText(c))
			.collect(Collectors.joining());

		// Note: Unfortunately, due to the users
		// each potentially using resource packs with different fonts
		// the number of characters per line in their Chat Pages can vary
		// significantly.
		return ChatPaginator.wordWrap(
			combined,
			AVERAGE_CHAT_PAGE_WIDTH
		).length;
	}

	private int numComponentLines(BaseComponent component)
	{
		return ChatPaginator.wordWrap(
			component.toPlainText(),
			AVERAGE_CHAT_PAGE_WIDTH
		).length;
	}

	private void removeOverflow(UUID playerId, int messageLength)
	{
		List<BaseComponent> history = getChatHistory(playerId);

		// Calculate the number of lines contained in the history's
		// components plus the number of lines contained in the new Message
		// and subtract the queue size (or total number of lines that can be
		// displayed)
		int historyLength = numComponentLines(history);
		int oversize = historyLength + messageLength - QUEUE_SIZE;
		int i = 1;

		// Remove older components to make room for the new one.
		while (i <= oversize)
		{
			//remove the oldest element
			if (!history.isEmpty())
			{
				i += numComponentLines(history.get(0));
				history.remove(0);
			}
			else
				break;
		}
		chatMap.put(playerId, history);
	}

	@Override
	public void addMessages(Set<UUID> recipientIds, BaseComponent... messages)
	{
		if (messages == null || messages.length == 0)
			return;

		List<BaseComponent> componentList = Arrays.asList(messages);
		int messageLength = numComponentLines(componentList);
		for (UUID recipientId : recipientIds)
		{
			removeOverflow(recipientId, messageLength);
			List<BaseComponent> history = getChatHistory(recipientId);

			history.addAll(componentList);

			chatMap.put(
				recipientId,
				history
			);
		}
	}

	@Override
	public void addMessage(UUID recipientId, BaseComponent... messages) {
		if (messages == null || messages.length == 0)
			return;

		List<BaseComponent> componentList = Arrays.asList(messages);
		removeOverflow(recipientId, numComponentLines(componentList));

		List<BaseComponent> history = getChatHistory(recipientId);
		history.addAll(componentList);

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
	public void broadcastMessage(BaseComponent... messages)
	{
		if (messages == null || messages.length == 0)
			return;

		addMessages(chatMap.keySet(), messages);
	}

	@Override
	public BaseComponent[] getContent(UUID playerId) {
		List<BaseComponent> history = chatMap.getOrDefault(playerId, null);

		if (history == null)
			return null;

		ComponentBuilder emptyLineBuilder = new ComponentBuilder("");
		// Fill in the empty space in the output
		// above the current messages in the history
		int lines = (history.size() > 0) ? numComponentLines(history) : 0;

		for (int i = QUEUE_SIZE - lines; i > 1; i--)
			emptyLineBuilder.append("\n");

		// Convert the empty lines into a component.
		ComponentBuilder builder = new ComponentBuilder(emptyLineBuilder);

		// Add the chat history to the output
		boolean prevStylized = false;

		// Prevent formatting from carrying over between elements
		history.forEach(component -> builder.append(
			component,
			NONE
		));

		// Add the separator for the line before footer.
		builder.append(
			repeat(
				"=",
				GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH - 7
			)
		)
		.color(GOLD);

		return builder.create();
	}
}
