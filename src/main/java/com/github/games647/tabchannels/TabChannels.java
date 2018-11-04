package com.github.games647.tabchannels;

import com.github.games647.tabchannels.commands.ChannelCommand;
import com.github.games647.tabchannels.commands.CreateCommand;
import com.github.games647.tabchannels.commands.PrivateCommand;
import com.github.games647.tabchannels.commands.SwitchCommand;
import com.github.games647.tabchannels.listener.ChatListener;
import com.github.games647.tabchannels.listener.SubscriptionListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import net.md_5.bungee.api.chat.BaseComponent;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.bukkit.ChatColor.DARK_RED;

public class TabChannels extends JavaPlugin implements TabChannelsManager
{
	private final Map<UUID, Subscriber> subscribers = new ConcurrentHashMap<>();

	/**
	 * Map of Channel Ids to their Channel
	 */
	private final Map<String, Channel<?>> channels = new ConcurrentHashMap<>();

	private final ComponentChannel globalChannel = new ComponentChannel("global", false);
	private final String MONITORING_CHANNEL_ID_PREFIX = "mon_";

	@SuppressWarnings("FieldCanBeLocal")
	private final String MONITORING_CHANNEL_NAME = "Monitor";

	@Override
	public void onEnable() {
		//register commands
		getCommand(this.getName().toLowerCase()).setExecutor(new ChannelCommand(this));
		getCommand("switchchannel").setExecutor(new SwitchCommand(this));
		getCommand("private").setExecutor(new PrivateCommand(this));
		getCommand("createchannel").setExecutor(new CreateCommand(this));

		//register listeners
		PluginManager pm  = Bukkit.getPluginManager();
		pm.registerEvents(new SubscriptionListener(this), this);
		pm.registerEvents(new ChatListener(this), this);
//        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
//            //we cannot register it here because Java wouldn't know the type specification
//            PacketChatListener.createInstance(this);
//        } else {
//        }

		getServer().getServicesManager().register(
			TabChannelsManager.class,
			this,
			this,
			ServicePriority.Normal
		);
		channels.put(globalChannel.getId(), globalChannel);

		//load all players if the server is already started like in a reload
		Bukkit.getOnlinePlayers()
			.forEach(this::loadPlayer);
	}

	public void switchChannel(Player sender, String channelId)
	{
		UUID senderId = sender.getUniqueId();

		if (!hasSubscriber(senderId) || !hasChannel(channelId))
			return;

		Subscriber selfSubscriber = subscribers.get(senderId);
		Channel channel = channels.get(channelId);


		if (channel == null) {
			sender.sendMessage(DARK_RED + "A channel with that name doesn't exist");
		}
		else if (!channel.hasRecipient(senderId) && (channel.isPrivate() || channel.isGroup()))
		{
			sender.sendMessage(DARK_RED + "You do not have access to that channel!");
		}
		else
		{
			selfSubscriber.switchChannel(channel);
			sender.spigot().sendMessage(channel.getHeader(channel.getName(senderId)));
			sender.spigot().sendMessage(channel.getContent(senderId));
			sender.spigot().sendMessage(selfSubscriber.getChannelSelection());
		}
	}

	@Override
	public boolean isMonitoringChannel(String channelId)
	{
		return channelId.startsWith(MONITORING_CHANNEL_ID_PREFIX);
	}

	public Map<UUID, Subscriber> getSubscribers() {
		return subscribers;
	}

	@Override
	public Channel getChannel(String id)
	{
		return channels.getOrDefault(id, null);
	}

	@Override
	public void createComponentChannel(
		String channelName,
		String channelId,
		boolean isPrivate,
		boolean isGroup
	){
		if (!getChannels().containsKey(channelName))
		{
			Channel newChannel = new ComponentChannel(
				channelId,
				channelName,
				isPrivate,
				isGroup
			);
			channels.put(
				channelId,
				newChannel
			);
		}
	}

	@SuppressWarnings("unused")
	public Channel getChannelByName(String channelName)
	{
	   Channel channel = channels.values()
			.stream()
			.filter(c -> c.getName(null).equalsIgnoreCase(channelName))
			.findFirst()
			.orElse(null);

		if (channel != null)
			return channel;

		return null;
	}

	public Map<String, Channel<?>> getChannels() {
		return channels;
	}

	@SuppressWarnings("unused")
	public Channel getGlobalChannel() {
		return globalChannel;
	}

	public void loadPlayer(Player player) {
		//automatically subscribe to the global channel
		UUID playerId = player.getUniqueId();

		if (!hasSubscriber(playerId))
		{
			Subscriber subscriber = new Subscriber(this, playerId, globalChannel);
			subscribers.put(playerId, subscriber);

			globalChannel.addRecipient(playerId);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public void loadPlayer(UUID playerId) {
		//automatically subscribe to the global channel
		if (!hasSubscriber(playerId))
		{
			Subscriber subscriber = new Subscriber(this, playerId, globalChannel);
			subscribers.put(playerId, subscriber);

			globalChannel.addRecipient(playerId);
		}
	}

	@Override public void createChannel(
		String channelName,
		String channelId,
		boolean isPrivate,
		boolean isGroup
	)
	{
		if (!getChannels().containsKey(channelName))
		{
			Channel newChannel = new TextChannel(
				channelId,
				channelName,
				isPrivate,
				isGroup
			);
			channels.put(
				channelId,
				newChannel
			);
		}
	}

	public void unsubscribeFromAllChannels(UUID playerId)
	{
		if (hasSubscriber(playerId))
		{
			Subscriber subscriber = subscribers.remove(playerId);

			//removes the subscriber from all channels
			for (String channelId : subscriber.getSubscriptions()) {
				Channel<?> subscription = channels.get(channelId);
				subscription.removeRecipient(playerId);


				if (subscription.isPrivate() && subscription.hasRecipients()) {
					//If it's a private chat remove the subscription of the partner too
					//so it's no longer referenced and can be garbage collected
					UUID chatPartner = subscription.getRecipients().get(0);
					Subscriber privateChatSubscriber = getSubscriber(chatPartner);
					privateChatSubscriber.unsubscribe(subscription);

					if (channels.containsKey(subscription.getId()))
						channels.remove(subscription.getId());
				}
			}
		}
	}

	@Override
	public void createMonitoringChannel(String channelName, UUID playerId)
	{
		String channelId = MONITORING_CHANNEL_ID_PREFIX + playerId.toString();

		if (!getChannels().containsKey(channelName))
		{
			Channel newChannel = new TextChannel(
				channelId,
				channelName,
				true,
				false
			);
			channels.put(
				channelId,
				newChannel
			);
		}
	}

	@Override
	public String joinMonitoringChannel(UUID playerId)
	{
		Subscriber subscriber;
		String channelId = MONITORING_CHANNEL_ID_PREFIX + playerId.toString();
		Channel channel;

		if (!hasSubscriber(playerId))
		{
			subscriber = new Subscriber(this, playerId, globalChannel);
			subscribers.put(playerId, subscriber);
			globalChannel.addRecipient(playerId);
		}
		else
			subscriber = subscribers.get(playerId);

		if (!channels.containsKey(channelId))
			createChannel(
				channelId,
				MONITORING_CHANNEL_NAME,
				true,
				false
			);

		channel = channels.get(channelId);

		if (subscriber != null && channel != null)
		{
			Player player = Bukkit.getPlayer(playerId);
			channel.addRecipient(playerId);
			subscriber.subscribe(channel);

			if (player != null && player.isOnline())
			{
				Bukkit.getScheduler().runTask(this, () -> {
					player.spigot()
						.sendMessage(channel.getHeader(channel.getName(playerId)));
					player.spigot()
						.sendMessage(channel.getContent(playerId));
					player.spigot()
						.sendMessage(subscriber.getChannelSelection());
				});
			}
			return channelId;
		}
		return null;
	}

	@Override
	public String joinChannel(String channelName, UUID playerId, boolean isPrivate, boolean isGroup)
	{
		Subscriber subscriber;
		Channel channel;

		if (!hasSubscriber(playerId))
		{
			subscriber = new Subscriber(this, playerId, globalChannel);
			subscribers.put(playerId, subscriber);
			globalChannel.addRecipient(playerId);
		}
		else
			subscriber = subscribers.get(playerId);

		if (!channels.containsKey(channelName))
			createChannel(
				channelName, // use the channel name for the id
				channelName,
				isPrivate,
				isGroup
			);

		channel = channels.get(channelName);

		if (subscriber != null && channel != null)
		{
			Player player = Bukkit.getPlayer(playerId);
			channel.addRecipient(playerId);
			subscriber.subscribe(channel);

			if (player != null && player.isOnline())
			{
				Bukkit.getScheduler().runTask(this, () -> {
					player.spigot()
						.sendMessage(channel.getHeader(channel.getName(playerId)));
					player.spigot()
						.sendMessage(channel.getContent(playerId));
					player.spigot()
						.sendMessage(subscriber.getChannelSelection());
				});
			}
			return channel.getId();
		}
		return null;
	}

	public Set<Channel> getSubscribedChannels(UUID playerId)
	{
		List<String> channelIds = getSubscriber(playerId).getSubscriptions();
		Set<Channel> result = new HashSet<>();

		channelIds.forEach(id -> {
			if (hasChannel(id))
				result.add(channels.get(id));
		});

		return result;
	}

	@Override public void leaveChannel(String channelId, UUID playerId)
	{
		if (subscribers.containsKey(playerId))
		{
			Subscriber subscriber = subscribers.get(playerId);

			if (subscriber != null && channels.containsKey(channelId))
			{
				Channel channel = channels.get(channelId);
				subscriber.unsubscribe(channel);
			}
		}
	}

	@Override public void leaveMonitoringChannel(UUID playerId)
	{
		if (subscribers.containsKey(playerId))
		{
			Subscriber subscriber = subscribers.get(playerId);
			String channelId = MONITORING_CHANNEL_ID_PREFIX + playerId.toString();

			if (subscriber != null && channels.containsKey(channelId))
			{
				Channel channel = channels.get(channelId);
				subscriber.unsubscribe(channel);
				channel.removeRecipient(playerId);
				channels.remove(channelId);
			}
		}
	}

	@Override public void sendMessage(String channelId, String... messages)
	{
		if (hasChannel(channelId))
		{
			TextChannel channel = (TextChannel)channels.get(channelId);

			if (channel != null)
			{
				channel.broadcastMessage(messages);
				ChatListener.notifyChanges(
					this,
					channel,
					Sets.newHashSet(channel.getRecipients())
				);
			}
		}
	}

	@Override public void broadcastMessage(String channelName, String... messages)
	{
		sendMessage(channelName, messages);
	}

	@Override public void sendComponent(String channelId, BaseComponent... components)
	{
		channelId = checkNotNull(channelId, "The Channel Id must not be null.");
		components = checkNotNull(components, "The base components cannot be null.");

		if (hasChannel(channelId))
		{
			ComponentChannel channel = (ComponentChannel)channels.get(channelId);

			if (channel != null)
			{
				channel.broadcastMessage(components);
				ChatListener.notifyChanges(
					this,
					channel,
					Sets.newHashSet(channel.getRecipients())
				);

			}
		}
	}

	@Override public void sendComponent(String channelId, UUID playerId, BaseComponent... components)
	{
		channelId = checkNotNull(
			channelId,
			"The Channel Id must not be null."
		);
		playerId = checkNotNull(
			playerId,
			"The Player Id must not be null."
		);
		components = checkNotNull(
			components,
			"The base components cannot be null."
		);

		if (hasChannel(channelId))
		{
			ComponentChannel channel = (ComponentChannel)channels.get(channelId);

			if (channel != null)
			{
				channel.addMessage(playerId, components);
				ChatListener.notifyChanges(
					this,
					channel,
					Sets.newHashSet(channel.getRecipients())
				);

			}
		}
	}

	@Override public void broadcastComponent(String channelId, BaseComponent... components)
	{
		channelId = checkNotNull(channelId, "The Channel Id must not be null.");
		components = checkNotNull(components, "The base components cannot be null.");

		sendComponent(channelId, components);
	}

	@Override public void sendMessage(String channelId, UUID playerId, String... messages)
	{
		if (playerId == null || messages == null || messages.length == 0)
			return;

		if (hasChannel(channelId))
		{
			TextChannel channel = (TextChannel)channels.get(channelId);

			if (channel != null && channel.hasRecipient(playerId))
			{
				channel.addMessage(playerId, Arrays.toString(messages));

				ChatListener.notifyChanges(
					this,
					channel,
					Sets.newHashSet(playerId)
				);
			}
		}
	}

	@Override public void sendMonitoringMessage(UUID playerId, String... messages)
	{
		String channelId = MONITORING_CHANNEL_ID_PREFIX + playerId.toString();

		if (hasChannel(channelId) && hasSubscriber(playerId))
		{
			TextChannel channel = (TextChannel)channels.getOrDefault(channelId, null);

			if (channel != null)
			{
				channel.addMessage(playerId, Arrays.toString(messages));
				ChatListener.notifyChanges(
					this,
					channel,
					Sets.newHashSet(playerId)
				);
			}
		}
	}

	@Override public void sendMonitoringComponent(UUID playerId, BaseComponent... components)
	{

	}

	@Override public Subscriber getSubscriber(UUID playerId)
	{
		if (subscribers.containsKey(playerId))
			return subscribers.get(playerId);

		return null;
	}

	@Override public List<String> getChannelNames()
	{
		if (!channels.isEmpty())
			return channels.values().stream().map(c -> c.getName(null)).collect(Collectors.toList());
		else
			return null;
	}

	@Override public List<String> getChannelIds()
	{
		if (!channels.isEmpty())
			return new ArrayList<>(channels.keySet());
		else
			return null;
	}

	@Override public boolean hasSubscriber(UUID playerId)
	{
		return subscribers.containsKey(playerId);
	}

	@Override public void subscribe(UUID playerId, String channelId)
	{
		if(!hasSubscriber(playerId))
		{
			loadPlayer(playerId);
		}
		Subscriber subscriber = subscribers.get(playerId);

		if (!subscriber.hasSubscribed(channelId))
		{
			if (channels.containsKey(channelId))
			{
				Channel channel = channels.get(channelId);
				subscriber.subscribe(channel);
			}
		}
	}

	@Override public boolean hasChannel(String channelId)
	{
		return channels.containsKey(channelId);
	}

}
