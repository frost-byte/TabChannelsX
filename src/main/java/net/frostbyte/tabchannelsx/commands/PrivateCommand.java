package net.frostbyte.tabchannelsx.commands;

import net.frostbyte.tabchannelsx.Channel;
import net.frostbyte.tabchannelsx.Subscriber;
import net.frostbyte.tabchannelsx.TabChannelsX;

import java.util.UUID;

import net.frostbyte.tabchannelsx.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrivateCommand implements CommandExecutor {

	private final TabChannelsX plugin;

	public PrivateCommand(TabChannelsX plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + "You cannot start a private message channel");
			return true;
		}

		if (args.length > 0) {
			String target = args[0];

			Player self = (Player) sender;
			Player targetPlayer = Bukkit.getPlayerExact(target);
			if (targetPlayer == null) {
				sender.sendMessage(ChatColor.DARK_RED + "This player isn't online");
			} else if (self.equals(targetPlayer)) {
				sender.sendMessage(ChatColor.DARK_RED + "You cannot message with yourself");
			} else {
				//user who started the chat + the target user
				String channelId = self.getUniqueId().toString() + targetPlayer.getUniqueId().toString();
				String partnerChannelId = targetPlayer.getUniqueId().toString() + self.getUniqueId().toString();
				if (plugin.getChannels().containsKey(channelId) || plugin.getChannels().containsKey(partnerChannelId)) {
					sender.sendMessage(ChatColor.DARK_RED + "This chat already exists");
				} else {
					startPrivateChat(self, targetPlayer, channelId);
				}
			}
		} else {
			sender.sendMessage(ChatColor.DARK_RED + "Missing receiver name");
		}

		return true;
	}

	private void startPrivateChat(Player self, Player targetPlayer, String channelId) {
		//start a private chat
		Subscriber selfSubscriber = plugin.getSubscribers().get(self.getUniqueId());

		UUID targetUUID = targetPlayer.getUniqueId();
		Subscriber targetSubscriber = plugin.getSubscribers().get(targetUUID);

		TextChannel selfPrivateChannel = new TextChannel(channelId, "Private", true, false);
		selfPrivateChannel.addRecipient(self.getUniqueId());
		selfPrivateChannel.addRecipient(targetUUID);
		selfSubscriber.subscribe(selfPrivateChannel);
		targetSubscriber.subscribe(selfPrivateChannel);

		plugin.getChannels().put(channelId, selfPrivateChannel);

		sendNewChat(selfSubscriber, self);
		sendNewChat(targetSubscriber, targetPlayer);
	}

	private void sendNewChat(Subscriber subscriber, Player player)
	{
		if (player == null)
			return;

		String channelId = subscriber.getCurrentChannel();
		UUID playerId = player.getUniqueId();
		Channel<?> channel = plugin.getChannel(channelId);

		try
		{
			player.spigot().sendMessage(channel.getHeader(channel.getChannelName()));
			player.spigot().sendMessage(channel.getContent(playerId));
			player.spigot().sendMessage(subscriber.getChannelSelection());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
