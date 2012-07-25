package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

class MobSpawnerEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 0);
	}
	
}
