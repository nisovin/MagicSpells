package com.nisovin.magicspells.spelleffects;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

class ParticlesEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		String name = "explode";
		float horizSpread = 0.2F;
		float vertSpread = 0.2F;
		float speed = 0.2F;
		int count = 5;
		float yOffset = 1.9F;
						
		if (param != null && !param.isEmpty()) {
			String[] data = param.split(" ");
			
			if (data.length >= 1) {
				name = data[0];
			}
			if (data.length >= 2) {
				horizSpread = Float.parseFloat(data[1]);
			}
			if (data.length >= 3) {
				vertSpread = Float.parseFloat(data[2]);
			}
			if (data.length >= 4) {
				speed = Float.parseFloat(data[3]);
			}
			if (data.length >= 5) {
				count = Integer.parseInt(data[4]);
			}
			if (data.length >= 6) {
				yOffset = Float.parseFloat(data[5]);
			}
		}
		
		MagicSpells.getVolatileCodeHandler().playParticleEffect(location, name, horizSpread, vertSpread, speed, count, 15, yOffset);
	}
	
}
