package com.nisovin.magicspells.spelleffects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class BroadcastEffect extends SpellEffect {

	String message = "";
	int range = 0;
	int rangeSq = 0;
	boolean targeted = false;
	
	@Override
	public void loadFromString(String string) {
		message = string;
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		message = config.getString("message", message);
		range = config.getInt("range", range);
		rangeSq = range * range;
		targeted = config.getBoolean("targeted", targeted);
	}

	@Override
	public void playEffectLocation(Location location) {
		broadcast(location, message);
	}
	
	@Override
	public void playEffectEntity(Entity entity) {
		if (targeted) {
			if (entity != null && entity instanceof Player) {
				MagicSpells.sendMessage((Player)entity, message);
			}
		} else {
			String msg = message;
			if (entity != null && entity instanceof Player) {
				msg = msg.replace("%a", ((Player)entity).getDisplayName()).replace("%t", ((Player)entity).getDisplayName()).replace("%n", ((Player)entity).getName());
			}
			broadcast(entity == null ? null : entity.getLocation(), msg);
		}
	}
	
	private void broadcast(Location location, String message) {
		if (range <= 0) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				MagicSpells.sendMessage(player, message);
			}
		} else if (location != null) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) <= rangeSq) {
					MagicSpells.sendMessage(player, message);
				}
			}
		}
	}

}
