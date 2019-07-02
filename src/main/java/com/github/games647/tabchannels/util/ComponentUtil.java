package com.github.games647.tabchannels.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;

public final class ComponentUtil {
	private ComponentUtil() {}

	public static ChatColor findLastDecorator(BaseComponent input) {
		String temp = input.toLegacyText();
		int lastIndex = temp.lastIndexOf(ChatColor.COLOR_CHAR);

		if (lastIndex >= 0 && lastIndex < temp.length() - 1) {
			return ChatColor.getByChar(temp.charAt(lastIndex + 1));
		}

		return ChatColor.WHITE;
	}
}
