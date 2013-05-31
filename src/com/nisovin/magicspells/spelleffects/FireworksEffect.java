package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

public class FireworksEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		boolean flicker = false;
		boolean trail = false;
		int type = 0;
		int[] colors = new int[] { 0xFF0000 };
		int[] fadeColors = new int[] { 0xFF0000 };
		int flightDuration = 0;
		
		if (param != null && !param.isEmpty()) {
			String[] data = param.split(" ");
			if (data.length >= 1 && data[0].equalsIgnoreCase("yes")) {
				flicker = true;
			}
			if (data.length >= 2 && data[1].equalsIgnoreCase("yes")) {
				trail = true;
			}
			if (data.length >= 3) {
				type = Integer.parseInt(data[2]);
			}
			if (data.length >= 4) {
				String[] c = data[3].split(",");
				colors = new int[c.length];
				for (int i = 0; i < c.length; i++) {
					colors[i] = Integer.parseInt(c[i], 16);
				}
			}
			if (data.length >= 5) {
				String[] c = data[4].split(",");
				fadeColors = new int[c.length];
				for (int i = 0; i < c.length; i++) {
					fadeColors[i] = Integer.parseInt(c[i], 16);
				}
			}
			if (data.length >= 6) {
				flightDuration = Integer.parseInt(data[5]);
			}
		}
		
		MagicSpells.getVolatileCodeHandler().createFireworksExplosion(location, flicker, trail, type, colors, fadeColors, flightDuration);
	}
	
}
