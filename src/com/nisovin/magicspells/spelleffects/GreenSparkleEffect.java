package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

class GreenSparkleEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, "happyVillager", .3F, .3F, .5F, 4, 32, 2F);
	}
	
}
