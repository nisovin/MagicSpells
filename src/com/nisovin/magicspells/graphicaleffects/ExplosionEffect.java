package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.Location;

class ExplosionEffect extends GraphicalEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().createExplosion(location, 0F);
	}

}
