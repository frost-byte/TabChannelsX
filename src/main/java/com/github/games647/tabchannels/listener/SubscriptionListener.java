package com.github.games647.tabchannels.listener;

import com.github.games647.tabchannels.TabChannels;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("unused")
public class SubscriptionListener implements Listener {

	private final TabChannels plugin;

	public SubscriptionListener(TabChannels plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent joinEvent) {
		plugin.loadPlayer(joinEvent.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent quitEvent) {
		Player player = quitEvent.getPlayer();

		plugin.unsubscribeFromAllChannels(player.getUniqueId());
	}
}
