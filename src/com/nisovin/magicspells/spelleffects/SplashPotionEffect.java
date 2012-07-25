package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class SplashPotionEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		int pot = 0;
		if (param != null && !param.isEmpty()) {
			try {
				pot = Integer.parseInt(param);
			} catch (NumberFormatException e) {			
			}
		}
		location.getWorld().playEffect(location, Effect.POTION_BREAK, pot);
	}
	
}
