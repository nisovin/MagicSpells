package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class SmokeEffect extends GraphicalEffect {

	@Override
	public void playEffect(Location location, String param) {
		int dir = 4;
		if (param != null && !param.isEmpty()) {
			try {
				dir = Integer.parseInt(param);
			} catch (NumberFormatException e) {			
			}
		}
		location.getWorld().playEffect(location, Effect.SMOKE, dir);
	}

}
