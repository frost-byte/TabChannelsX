package com.github.games647.tabchannels.commands;

import com.github.games647.tabchannels.Channel;
import com.github.games647.tabchannels.TabChannels;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class SwitchCommand implements TabExecutor {

	private final TabChannels plugin;

	public SwitchCommand(TabChannels plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			//switch the channel
			if (args.length > 0) {
				String channelName = args[0];
				Player player = (Player) sender;

				plugin.switchChannel(player, channelName.toLowerCase());

			} else {
				sender.sendMessage(ChatColor.DARK_RED + "Missing channel name");
			}
		} else {
			sender.sendMessage(ChatColor.DARK_RED + "Only players could have chat channels");
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			UUID senderId = player.getUniqueId();
			Set<Channel> subscriptions = plugin.getSubscribedChannels(senderId);

			List<String> suggestions = Lists.newArrayList();
			for (Channel channel : subscriptions) {
				//show only channels where the player is already in
				if (channel.getRecipients().contains(senderId)) {
					suggestions.add(channel.getName(senderId).toLowerCase());
				}
			}

			suggestions.sort(String.CASE_INSENSITIVE_ORDER);
			return suggestions;
		} else {
			//only players have channels
			return null;
		}
	}
}
