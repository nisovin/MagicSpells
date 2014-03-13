package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;

class GreenSparkleEffect extends SpellEffect {

	@Override
	public void loadFromString(String string) {
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
	}

	@Override
	public void playEffectLocation(Location location) {
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, "happyVillager", .3F, .3F, .5F, 4, 32, 2F);
	}
	
}
