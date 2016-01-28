package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;

public class ParticleLineEffect extends ParticlesEffect {
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			String[] data = string.split(" ");
			
			if (data.length >= 1) {
				distanceBetween = Float.parseFloat(data[0]);
			}
			if (data.length >= 2) {
				name = data[1];
			}
			if (data.length >= 3) {
				xSpread = Float.parseFloat(data[2]);
				zSpread = xSpread;
			}
			if (data.length >= 4) {
				ySpread = Float.parseFloat(data[3]);
			}
			if (data.length >= 5) {
				speed = Float.parseFloat(data[4]);
			}
			if (data.length >= 6) {
				count = Integer.parseInt(data[5]);
			}
			if (data.length >= 7) {
				yOffset = Float.parseFloat(data[6]);
			}
		}
	}
	
	@Override
	protected void playEffectLine(Location location1, Location location2) {
		
		int c = (int)Math.ceil(location1.distance(location2) / distanceBetween) - 1;
		if (c <= 0) return;
		Vector v = location2.toVector().subtract(location1.toVector()).normalize().multiply(distanceBetween);
		Location l = location1.clone();
		
		for (int i = 0; i < c; i++) {
			l.add(v);
			MagicSpells.getVolatileCodeHandler().playParticleEffect(l, name, xSpread, ySpread, zSpread, speed, count, 15, yOffset);
		}
	}

}
