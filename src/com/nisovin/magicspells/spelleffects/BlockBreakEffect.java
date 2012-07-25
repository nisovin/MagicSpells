package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class BlockBreakEffect extends SpellEffect {
	
	@Override
	public void playEffect(Location location, String param) {
		int id = 0;
		if (param != null && !param.isEmpty()) {
			try {
				id = Integer.parseInt(param);
			} catch (NumberFormatException e) {			
			}
		}
		location.getWorld().playEffect(location, Effect.STEP_SOUND, id);
	}
}
