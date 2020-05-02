package net.frostbyte.tabchannelsx;

import net.frostbyte.tabchannelsx.util.ComponentUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;


import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.md_5.bungee.api.ChatColor.GOLD;

import static net.md_5.bungee.api.chat.BaseComponent.toPlainText;
import static net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention.*;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.bukkit.util.ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH;
import static org.bukkit.util.ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;
import static org.bukkit.util.ChatPaginator.wordWrap;

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


	private int numComponentLines(List<BaseComponent> components)
	{
		if (components == null)
			return 0;

		StringBuilder sb = new StringBuilder();

		for (BaseComponent component : components)
		{
			if (component == null)
				continue;
			String s = toPlainText(component);
			sb.append(s);
		}

		String combined = sb.toString();

		// Note: Unfortunately, due to the users
		// each potentially using resource packs with different fonts
		// the number of characters per line in their Chat Pages can vary
		// significantly.
		return wordWrap(
			combined,
			AVERAGE_CHAT_PAGE_WIDTH
		).length;
	}

	private int numComponentLines(BaseComponent... components)
	{
		if (components == null)
			return 0;

		String plainText = toPlainText(components);

		return wordWrap(
			plainText,
			AVERAGE_CHAT_PAGE_WIDTH
		).length;
	}

	private int numComponentLines(BaseComponent component)
	{
		return wordWrap(
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

		int numLines = numComponentLines(messages);

		if (numLines > 0)
		{
			int messagesLength = messages.length;
			int i = 0;

			for (; i < messagesLength; i++)
			{
				BaseComponent message = messages[i];
				if (i > 0) {
					BaseComponent last = messages[i - 1];
					ChatColor previous = ComponentUtil.findLastDecorator(last);
					message.setColor(previous);
				}

				for (UUID recipientId : recipientIds)
				{
					removeOverflow(
						recipientId,
						numLines
					);
					List<BaseComponent> history = getChatHistory(recipientId);
					history.add(message);

					chatMap.put(
						recipientId,
						history
					);
				}
			}
		}
	}

	@Override
	public void addMessage(UUID recipientId, BaseComponent... messages) {
		if (messages == null || messages.length == 0)
			return;

		int numLines = numComponentLines(messages);

		if (numLines > 0)
		{
			removeOverflow(
				recipientId,
				numLines
			);

			List<BaseComponent> history = getChatHistory(recipientId);
			int messagesLength = messages.length;
			int i = 0;

			for (; i < messagesLength; i++)
			{
				ComponentBuilder builder = new ComponentBuilder("");
				BaseComponent message = messages[i];

				if (message != null)
					history.add(message);
			}

			chatMap.put(
				recipientId,
				history
			);
		}
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
