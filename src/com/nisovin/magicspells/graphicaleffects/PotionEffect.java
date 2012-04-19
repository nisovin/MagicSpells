package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;

class PotionEffect extends GraphicalEffect {

	@Override
	public void playEffect(Entity entity, String param) {
		if (entity instanceof LivingEntity) {
			LivingEntity le = (LivingEntity)entity;
			
			int color = 0xFF0000;
			int duration = 30;			
			if (param != null && !param.isEmpty()) {
				String[] data = param.split(" ");
				try {
					color = Integer.parseInt(data[0], 16);
				} catch (NumberFormatException e) {					
				}
				if (data.length > 1) {
					try {
						duration = Integer.parseInt(data[1]);
					} catch (NumberFormatException e) {						
					}
				}
			}
			
			MagicSpells.getVolatileCodeHandler().addPotionGraphicalEffect(le, color, duration);
		}
	}
	
}
