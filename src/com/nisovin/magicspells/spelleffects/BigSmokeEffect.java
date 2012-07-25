package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;

class BigSmokeEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		World world = location.getWorld();
		int lx = location.getBlockX();
		int ly = location.getBlockY();
		int lz = location.getBlockZ();
		Location loc;
		
		for (int x = lx-1; x <= lx+1; x++) {
			for (int y = ly; y <= ly+1; y++) {
				for (int z = lz-1; z <= lz+1; z++) {
					for (int i = 0; i <= 8; i+=2) {
						loc = new Location(world, x, y, z);
						world.playEffect(loc, Effect.SMOKE, i);
					}
				}
			}
		}
	}
	
}
