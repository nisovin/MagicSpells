package com.nisovin.magicspells.spelleffects;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class ActionBarTextEffect extends SpellEffect {

	String message = "";
	
	@Override
	public void loadFromString(String string) {
		message = ChatColor.translateAlternateColorCodes('&', string);
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		message = ChatColor.translateAlternateColorCodes('&', config.getString("message", message));
	}
	
	@Override
	protected void playEffectEntity(Entity entity) {
		if (entity instanceof Player) {
			MagicSpells.getVolatileCodeHandler().sendActionBarMessage((Player)entity, message);
		}
	}
	
}
