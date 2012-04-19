package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class EnderSignalEffect extends GraphicalEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
	}

}
