package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

class SmokeEffect extends SpellEffect {

	int dir = 4;
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			try {
				dir = Integer.parseInt(string);
			} catch (NumberFormatException e) {			
			}
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		dir = config.getInt("dir", dir);
	}

	@Override
	public void playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.SMOKE, dir);
	}

}
