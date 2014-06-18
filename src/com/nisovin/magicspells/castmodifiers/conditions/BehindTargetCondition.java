package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.castmodifiers.Condition;

public class BehindTargetCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(Player player) {
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		float targetFaceAngle = target.getLocation().getDirection().angle(new Vector(0, 0, 0));
		float diffAngle = target.getLocation().toVector().subtract(player.getLocation().toVector()).angle(new Vector(0, 0, 0));
		float diff = Math.abs(targetFaceAngle - diffAngle);
		return (diff >= 160 && diff <= 200);
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
