package net.frostbyte.tabchannelsx.commands;

import net.frostbyte.tabchannelsx.Channel;
import net.frostbyte.tabchannelsx.ComponentChannel;
import net.frostbyte.tabchannelsx.Subscriber;
import net.frostbyte.tabchannelsx.TabChannelsX;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateCommand implements CommandExecutor {

    private final TabChannelsX plugin;

    public CreateCommand(TabChannelsX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length > 0) {
            String channelName = args[0];
            if (plugin.getChannels().containsKey(channelName)) {
                sender.sendMessage(ChatColor.DARK_RED + "The channel with this name already exists");
            } else {
                Channel newChannel = new ComponentChannel(channelName, false);
                plugin.getChannels().put(channelName, newChannel);
                sender.sendMessage(ChatColor.DARK_GREEN + "Channel created");
                if (sender instanceof Player) {
                    UUID uniqueId = ((Player) sender).getUniqueId();
                    Subscriber subscriber = plugin.getSubscribers().get(uniqueId);
                    if (subscriber != null) {
                        subscriber.subscribe(newChannel);
                        sender.sendMessage(ChatColor.DARK_GREEN + "You auto joined this channel");
                    }
                }
            }
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "You have to provide the channel name you want to create");
        }

        return true;
    }
}
