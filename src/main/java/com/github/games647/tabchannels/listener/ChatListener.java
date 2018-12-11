package com.github.games647.tabchannels.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.tabchannels.*;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.github.games647.tabchannels.TabChannels.MESSAGE_TAG;

@SuppressWarnings( { "unused", "WeakerAccess" })
public class ChatListener implements Listener {

	private final TabChannels plugin;

	public ChatListener(TabChannels plugin) {
		this.plugin = plugin;
		AtomicReference<TabChannels> pluginRef = new AtomicReference<>(plugin);
		plugin.getProtocolManager().addPacketListener(
			new PacketAdapter(
				plugin,
				ListenerPriority.HIGH,
				PacketType.Play.Server.CHAT

			) {
				@Override public void onPacketSending(PacketEvent event)
				{
					Player player = event.getPlayer();
					UUID playerId = player.getUniqueId();
					PacketType packetType = event.getPacketType();

					if (packetType != PacketType.Play.Server.CHAT)
						return;

					Subscriber subscriber = pluginRef.get().getSubscriber(playerId);

					PacketContainer packet = event.getPacket();
					StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();

					if (chatComponents == null)
						return;

					WrappedChatComponent component = chatComponents.read(0);

					if (component == null)
						return;

					String message = component.getJson();

					if (message.startsWith(MESSAGE_TAG))
					{
						message = message.replaceFirst(MESSAGE_TAG, "");
						component.setJson(message);
						chatComponents.write(0, component);
						super.onPacketSending(event);

						return;
					}

					Channel channel = pluginRef.get().getChannel("global");
					Set<Channel> channels = Stream.of(channel).collect(Collectors.toSet());

					try
					{
						BaseComponent[] output = ComponentSerializer.parse(message);
						if (output != null && output.length > 0)
							message = BaseComponent.toLegacyText(output);
						if (!message.endsWith("\n"))
							message += "\n";
					}
					catch (Exception ex)
					{
						return;
					}

					addMessage(
						channels,
						message,
						player.getUniqueId()
					);
					event.setCancelled(true);
				}
			}
		);
	}

	//listen to the highest priority in order to let other plugins interpret it as successful event
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent playerChatEvent) {
		Player sender = playerChatEvent.getPlayer();
		UUID senderId = sender.getUniqueId();
		Set<UUID> recipientIds = playerChatEvent
			.getRecipients()
			.stream()
			.filter(OfflinePlayer::isOnline)
			.map(Entity::getUniqueId)
			.collect(Collectors.toSet());

		String message = playerChatEvent.getMessage();
		if (message == null || message.isEmpty())
			return;

		String format = playerChatEvent.getFormat();
		String chatMessage = String.format(format, sender.getDisplayName(), message);

		if (chatMessage == null || chatMessage.isEmpty())
			return;

		Subscriber subscriber = plugin.getSubscriber(senderId);
		Channel messageChannel = plugin.getChannel(subscriber.getCurrentChannel());

		if (!chatMessage.endsWith("\n"))
			chatMessage += "\n";

		addMessages(messageChannel, chatMessage, recipientIds, senderId);

		//remove the recipients from normal chats without hiding log messages
		playerChatEvent.getRecipients().clear();
	}

	private void addMessages(
		Channel channel,
		String message,
		Set<UUID> recipientIds,
		UUID senderId
	)
	{
		if (channel instanceof ComponentChannel)
		{
			ComponentChannel componentChannel = (ComponentChannel)channel;
			componentChannel.addMessages(
				recipientIds,
				TextComponent.fromLegacyText(message)
			);
		}
		else if (channel instanceof TextChannel)
		{
			TextChannel textChannel = (TextChannel)channel;
			textChannel.addMessages(recipientIds, message);
		}
		else
			return;

		notifyChanges(plugin, channel, recipientIds);
	}

	private void addMessage(
		Set<Channel> channels,
		String message,
		UUID recipientId
	)
	{
		for (Channel channel : channels) {

			if (channel instanceof ComponentChannel)
			{
				ComponentChannel componentChannel = (ComponentChannel)channel;
				componentChannel.addMessage(
					recipientId,
					TextComponent.fromLegacyText(message)
				);
			}
			else if (channel instanceof TextChannel)
			{
				TextChannel textChannel = (TextChannel)channel;
				textChannel.addMessage(recipientId, message);
			}
			else
				continue;

			notifyChanges(plugin, channel, Sets.newHashSet(recipientId));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent joinEvent) {
		Player player = joinEvent.getPlayer();
		String joinMessage = joinEvent.getJoinMessage();
		UUID playerId = player.getUniqueId();

		if (joinMessage != null && !joinMessage.isEmpty()) {
			Subscriber subscriber = plugin.getSubscriber(playerId);
			Set<Channel> channels = plugin.getSubscribedChannels(playerId);

			addMessage(channels, joinMessage, playerId);
			joinEvent.setJoinMessage("");
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onQuit(PlayerQuitEvent quitEvent) {
		Player player = quitEvent.getPlayer();
		String quitMessage = quitEvent.getQuitMessage();
		UUID playerId = player.getUniqueId();

		if (quitMessage != null && !quitMessage.isEmpty()) {
			Subscriber subscriber = plugin.getSubscriber(playerId);
			Set<Channel> channels = plugin.getSubscribedChannels(playerId);

			addMessage(channels, quitMessage, playerId);
			quitEvent.setQuitMessage("");
		}
	}

	public static void notifyChanges(
		TabChannels plugin,
		Channel messageChannel,
		Set<UUID> recipientIds
	) {
		if (messageChannel == null || recipientIds == null || recipientIds.isEmpty())
			return;

		for (UUID id : recipientIds) {
			if (!messageChannel.hasRecipient(id))
				continue;

			Subscriber receiver = plugin.getSubscriber(id);

			if (receiver != null) {
				onNewMessage(plugin, id, receiver, messageChannel);
			}
		}
	}

	public static void onNewMessage(TabChannels plugin, UUID recipient, Subscriber receiver, Channel messageChannel) {
		Player recipientPlayer = Bukkit.getPlayer(recipient);

		receiver.notifyNewMessage(messageChannel);
		Channel usedChannel = plugin.getChannel(receiver.getCurrentChannel());

		if (usedChannel != null && recipientPlayer != null && recipientPlayer.isOnline())
		{
			BaseComponent[] content = usedChannel.getContent(recipient);
			Subscriber subscriber = plugin.getSubscriber(recipient);
			recipientPlayer.spigot().sendMessage(usedChannel.getHeader(usedChannel.getChannelName()));
			recipientPlayer.spigot().sendMessage(content);
			recipientPlayer.spigot().sendMessage(subscriber.getChannelSelection());
		}
	}
}
