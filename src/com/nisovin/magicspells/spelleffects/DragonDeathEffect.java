package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;

public class DragonDeathEffect extends SpellEffect {

	@Override
	public void loadFromString(String string) {
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
	}

	@Override
	public void playEffectLocation(Location location) {
		MagicSpells.getVolatileCodeHandler().playDragonDeathEffect(location);
	}

}
