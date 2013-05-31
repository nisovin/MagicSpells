package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

class BlueSparkleEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, "witchMagic", .2F, .2F, .1F, 20, 32, 2F);
	}
	
}
