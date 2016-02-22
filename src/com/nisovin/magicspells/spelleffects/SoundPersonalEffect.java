package com.nisovin.magicspells.spelleffects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;

public class SoundPersonalEffect extends SpellEffect {
	
	String sound = "random.pop";
	float volume = 1.0F;
	float pitch = 1.0F;
	boolean broadcast = false;

	@Override
	public void loadFromString(String string) {
		if (string != null && string.length() > 0) {
			String[] data = string.split(" ");
			sound = data[0];
			if (data.length > 1) {
				volume = Float.parseFloat(data[1]);
			}
			if (data.length > 2) {
				pitch = Float.parseFloat(data[2]);
			}
		}
		if (sound.equals("random.wood_click")) {
			sound = "random.wood click";
		} else if (sound.equals("mob.ghast.affectionate_scream")) {
			sound = "mob.ghast.affectionate scream";
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		sound = config.getString("sound", sound);
		volume = (float)config.getDouble("volume", volume);
		pitch = (float)config.getDouble("pitch", pitch);
		broadcast = config.getBoolean("broadcast", broadcast);
	}

	@Override
	public void playEffectEntity(Entity entity) {
		if (broadcast) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				MagicSpells.getVolatileCodeHandler().playSound(player, sound, volume, pitch);
			}
		} else if (entity != null && entity instanceof Player) {
			MagicSpells.getVolatileCodeHandler().playSound((Player)entity, sound, volume, pitch);
		}
	}
	
}
