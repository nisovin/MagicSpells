package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

class BlockBreakEffect extends SpellEffect {

	int id = 1;
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			try {
				id = Integer.parseInt(string);
			} catch (NumberFormatException e) {			
			}
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		id = config.getInt("id", id);
	}
	
	@Override
	public void playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.STEP_SOUND, id);
	}
}
