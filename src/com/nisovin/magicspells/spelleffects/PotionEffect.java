package com.nisovin.magicspells.spelleffects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;

class PotionEffect extends SpellEffect {
	
	int color = 0xFF0000;
	int duration = 30;
	
	@Override
	public void loadFromString(String string) {
		if (string != null && !string.isEmpty()) {
			String[] data = string.split(" ");
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
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		String c = config.getString("color", "");
		if (!c.isEmpty()) {
			try {
				color = Integer.parseInt(c, 16);
			} catch (NumberFormatException e) {
			}
		}
		duration = config.getInt("duration", duration);
	}

	@Override
	public void playEffectEntity(Entity entity) {
		if (entity instanceof LivingEntity) {
			LivingEntity le = (LivingEntity)entity;
			MagicSpells.getVolatileCodeHandler().addPotionGraphicalEffect(le, color, duration);
		}
	}
	
}
