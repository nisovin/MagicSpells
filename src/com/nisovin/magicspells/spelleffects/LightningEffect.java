package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

class LightningEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().strikeLightningEffect(location);
	}

}
