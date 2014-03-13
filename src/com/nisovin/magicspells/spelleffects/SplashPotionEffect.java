package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

class SplashPotionEffect extends SpellEffect {
	
	int pot = 0;

	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			try {
				pot = Integer.parseInt(string);
			} catch (NumberFormatException e) {			
			}
		}
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		pot = config.getInt("potion", pot);
	}

	@Override
	public void playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.POTION_BREAK, pot);
	}
	
}
