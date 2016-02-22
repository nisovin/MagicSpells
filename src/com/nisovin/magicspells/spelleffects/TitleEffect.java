package com.nisovin.magicspells.spelleffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class TitleEffect extends SpellEffect {

	String title = null;
	String subtitle = null;
	int fadeIn = 10;
	int stay = 40;
	int fadeOut = 10;
	boolean broadcast = false;
	
	@Override
	public void loadFromString(String string) {
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		title = config.getString("title", title);
		if (title != null) title = ChatColor.translateAlternateColorCodes('&', title);
		subtitle = config.getString("subtitle", subtitle);
		if (subtitle != null) subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
		fadeIn = config.getInt("fade-in", fadeIn);
		stay = config.getInt("stay", stay);
		fadeOut = config.getInt("fade-out", fadeOut);
		broadcast = config.getBoolean("broadcast", broadcast);
	}
	
	@Override
	protected void playEffectEntity(Entity entity) {
		if (broadcast) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				MagicSpells.getVolatileCodeHandler().sendTitleToPlayer(player, title, subtitle, fadeIn, stay, fadeOut);
			}
		} else if (entity != null && entity instanceof Player) {
			MagicSpells.getVolatileCodeHandler().sendTitleToPlayer((Player)entity, title, subtitle, fadeIn, stay, fadeOut);
		}
	}

}
