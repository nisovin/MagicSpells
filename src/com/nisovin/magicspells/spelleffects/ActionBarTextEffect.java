package com.nisovin.magicspells.spelleffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class ActionBarTextEffect extends SpellEffect {

	String message = "";
	boolean broadcast = false;
	
	@Override
	public void loadFromString(String string) {
		message = ChatColor.translateAlternateColorCodes('&', string);
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		message = ChatColor.translateAlternateColorCodes('&', config.getString("message", message));
		broadcast = config.getBoolean("broadcast", broadcast);
	}
	
	@Override
	protected void playEffectEntity(Entity entity) {
		if (broadcast) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				MagicSpells.getVolatileCodeHandler().sendActionBarMessage(player, message);
			}
		} else if (entity != null && entity instanceof Player) {
			MagicSpells.getVolatileCodeHandler().sendActionBarMessage((Player)entity, message);
		}
	}
	
}
