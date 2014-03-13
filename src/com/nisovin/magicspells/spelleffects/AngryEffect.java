package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;

class AngryEffect extends SpellEffect {

	@Override
	public void loadFromString(String string) {
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
	}

	@Override
	public void playEffectLocation(Location location) {
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, "angryVillager", 0F, 0F, .2F, 1, 32, 2F);
	}
	
}
