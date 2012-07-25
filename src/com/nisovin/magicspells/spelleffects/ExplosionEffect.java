package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

class ExplosionEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().createExplosion(location, 0F);
	}

}
