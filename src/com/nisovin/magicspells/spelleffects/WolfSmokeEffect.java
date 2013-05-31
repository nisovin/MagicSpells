package com.nisovin.magicspells.spelleffects;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

import com.nisovin.magicspells.MagicSpells;

class WolfSmokeEffect extends SpellEffect {

	@Override
	public void playEffect(Entity entity, String param) {
		if (entity instanceof Tameable) {
			entity.playEffect(EntityEffect.WOLF_SMOKE);
		} else {
			playEffect(entity.getLocation(), param);
		}
	}
	
	@Override
	public void playEffect(Location location, String param) {
		MagicSpells.getVolatileCodeHandler().playEntityAnimation(location, EntityType.OCELOT, EntityEffect.WOLF_SMOKE.getData(), param != null && param.equals("instant"));
	}
	
}
