package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.Util;

public class RidingCondition extends Condition {

	EntityType entityType;
	
	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) {
			return true;
		}
		entityType = Util.getEntityType(var);
		return entityType != null;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		Entity vehicle = target.getVehicle();
		if (vehicle == null) return false;		
		return entityType == null || vehicle.getType() == entityType;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
