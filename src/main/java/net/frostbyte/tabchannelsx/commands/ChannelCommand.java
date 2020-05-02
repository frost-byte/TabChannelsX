package net.frostbyte.tabchannelsx.commands;

import net.frostbyte.tabchannelsx.TabChannelsX;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

public class ChannelCommand implements CommandExecutor {

    private final TabChannelsX plugin;

    public ChannelCommand(TabChannelsX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        String[] args
    ) {
        if (args.length > 0) {
            String subCommand = args[0];
            if ("switch".equalsIgnoreCase(subCommand) || "create".equalsIgnoreCase(subCommand)) {
                forwardCommand(sender, subCommand, args);
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "Unknown subcommand");
            }
        }

        return true;
    }

    private void forwardCommand(CommandSender sender, String command, String[] args) {
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command);
        if (pluginCommand != null) {
            String[] movedArgs = Arrays.copyOfRange(args, 1, args.length);
            pluginCommand.execute(sender, command, movedArgs);
        }
    }
}
