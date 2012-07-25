package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class EnderSignalEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
	}

}
