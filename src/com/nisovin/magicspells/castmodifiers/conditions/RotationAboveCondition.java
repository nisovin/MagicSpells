package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class RotationAboveCondition extends Condition {

	float rotation;
	
	@Override
	public boolean setVar(String var) {
		try {
			rotation = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return check(null, player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		float yaw = target.getLocation().getYaw();
		if (yaw < 0) yaw += 360;
		return yaw >= rotation;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
