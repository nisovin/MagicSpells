package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

class AngryEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, "angryVillager", 0F, 0F, .2F, 1, 32, 2F);
	}
	
}
